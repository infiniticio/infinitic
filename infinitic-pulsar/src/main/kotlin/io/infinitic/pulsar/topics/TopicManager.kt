/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */
package io.infinitic.pulsar.topics

import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.pulsar.admin.PulsarInfiniticAdmin
import io.infinitic.pulsar.config.Pulsar
import java.util.concurrent.ConcurrentHashMap

@Suppress("MemberVisibilityCanBePrivate", "unused")
class TopicManager(
  val admin: PulsarInfiniticAdmin,
  val topicNamer: TopicNamer
) : TopicNamer by topicNamer {

  /** Set of topics for current tenant and namespace */
  val topicSet: Set<String>
    get() = admin.getTopicsSet().getOrThrow()

  /** Set of task's names for current tenant and namespace */
  val taskSet: Set<String>
    get() {
      val tasks = mutableSetOf<String>()
      val prefix = topicNamer.getTopicName(ServiceTopics.EXECUTOR, ServiceName(""))
      topicSet.map { if (it.startsWith(prefix)) tasks.add(it.removePrefix(prefix)) }

      return tasks
    }

  /** Set of workflow's names for current tenant and namespace */
  val workflowSet: Set<String>
    get() {
      val workflows = mutableSetOf<String>()
      val prefix = topicNamer.getTopicName(WorkflowTopics.ENGINE, WorkflowName(""))
      topicSet.map { if (it.startsWith(prefix)) workflows.add(it.removePrefix(prefix)) }

      return workflows
    }

  /**
   * Check if a topic exists, and create it if not
   * We skip this if the topic has already been initialized successfully
   */
  fun initTopic(topicType: TopicType, name: String): Result<String> =
      topicNamer.getTopicName(topicType, name).let { topic ->
        when (initializedTopics.contains(topic)) {
          true -> Result.success(topic)
          false -> admin.initTopic(
              topic,
              topicType.isPartitioned,
              topicType.isDelayed,
          ).map { initializedTopics.add(topic); topic }
        }
      }

  /**
   * Check if a Dead Letter Queue topic exists, and create it if not
   * We skip this if the topic has already been initialized successfully
   */
  fun initDLQTopic(topicType: TopicType, name: String): Result<String?> =
      topicNamer.getTopicDLQName(topicType, name)?.let { topic ->
        when (initializedTopics.contains(topic)) {
          true -> Result.success(topic)
          false -> admin.initTopic(
              topic,
              topicType.isPartitioned,
              topicType.isDelayed,
          ).map { initializedTopics.add(topic); topic }
        }
      } ?: Result.success(null)

  /**
   * Delete a topic by name
   */
  fun deleteTopic(topic: String): Result<Unit> = admin.deleteTopic(topic)

  companion object {
    // thread-safe set of initialized topics
    private val initializedTopics = ConcurrentHashMap.newKeySet<String>()

    /** Create TopicManager from a Pulsar configuration instance */
    fun from(pulsar: Pulsar) = TopicManager(
        PulsarInfiniticAdmin(pulsar.admin, pulsar),
        TopicNamerDefault(pulsar.tenant, pulsar.namespace),
    )
  }
}
