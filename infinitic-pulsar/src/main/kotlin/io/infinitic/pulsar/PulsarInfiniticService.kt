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
package io.infinitic.pulsar
//
//import io.infinitic.pulsar.config.Pulsar
//import io.infinitic.pulsar.producers.getProducerName
//import io.infinitic.transport.pulsar.PulsarStarter
//import io.infinitic.transport.pulsar.topics.GlobalTopics
//import io.infinitic.transport.pulsar.topics.ServiceTopics
//import io.infinitic.transport.pulsar.topics.TopicNames
//import io.infinitic.transport.pulsar.topics.TopicNamesDefault
//import io.infinitic.transport.pulsar.topics.WorkflowTaskTopics
//import io.infinitic.transport.pulsar.topics.WorkflowTopics
//import io.infinitic.workers.WorkerAbstract
//import io.infinitic.workers.register.WorkerRegister
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.asCoroutineDispatcher
//import kotlinx.coroutines.cancel
//import kotlinx.coroutines.future.future
//import org.apache.pulsar.client.admin.Namespaces
//import org.apache.pulsar.client.admin.PulsarAdmin
//import org.apache.pulsar.client.admin.PulsarAdminException
//import org.apache.pulsar.client.admin.Tenants
//import org.apache.pulsar.client.admin.TopicPolicies
//import org.apache.pulsar.client.admin.Topics
//import org.apache.pulsar.client.api.PulsarClient
//import java.util.concurrent.CompletableFuture
//import java.util.concurrent.Executors
//
//@Suppress("MemberVisibilityCanBePrivate", "unused")
//class PulsarInfiniticService(
//  workerRegister: WorkerRegister,
//  val pulsarClient: PulsarClient,
//  val pulsarAdmin: PulsarAdmin,
//  val pulsar: Pulsar
//) : WorkerAbstract(workerRegister) {
//
//  private val pulsarNamespaces: Namespaces = pulsarAdmin.namespaces()
//  private val pulsarTenants: Tenants = pulsarAdmin.tenants()
//  private val pulsarTopics: Topics = pulsarAdmin.topics()
//  private val pulsarTopicPolicies: TopicPolicies = pulsarAdmin.topicPolicies()
//
//  /**
//   * We use a thread pool that creates new threads as needed, to improve performance when processing
//   * messages in parallel
//   */
//  private val threadPool = Executors.newCachedThreadPool()
//
//  /** Coroutine scope used to run workers */
//  private val scope = CoroutineScope(threadPool.asCoroutineDispatcher() + Job())
//
//  /** [PulsarInfiniticAdmin] instance: used to create tenant, namespace, topics, etc. */
//  val infiniticAdmin by lazy { PulsarInfiniticAdmin(pulsarAdmin, pulsar) }
//
//  /** [TopicNames] instance: used to get topic's name */
//  private val topicNames: TopicNames = TopicNamesDefault(pulsar.tenant, pulsar.namespace)
//
//  /** Worker unique name: from workerConfig or generated through Pulsar */
//  override val workerName by lazy {
//    getProducerName(
//        pulsarClient,
//        topicNames.topic(GlobalTopics.NAMER),
//        workerRegistry.name,
//    )
//  }
//
//  /** Full Pulsar namespace */
//  private val fullNamespace = "${pulsar.tenant}/${pulsar.namespace}"
//
//  override val workerStarter by lazy {
//    PulsarStarter(pulsarClient, topicNames, workerName, pulsar.producer, pulsar.consumer)
//  }
//
//  override val clientFactory = {
//    PulsarInfiniticClient(
//        pulsarClient,
//        pulsarAdmin,
//        pulsar.tenant,
//        pulsar.namespace,
//        pulsar.producer,
//        pulsar.consumer,
//    )
//  }
//
//  /** Start worker asynchronously */
//  override fun startAsync(): CompletableFuture<Unit> {
//    // ensure tenant exists
//    infiniticAdmin.createTenant()
//    // ensure namespace exists
//    val existing = infiniticAdmin.createNamespace()
//    // set namespace policies if required
//    if (!existing || pulsar.policies.forceUpdate) {
//      infiniticAdmin.updateNamespacePolicies()
//    }
//    // check that topics exist or create them
//    checkOrCreateTopics()
//
//    return scope.future { startWorker().join() }
//  }
//
//  /** Close worker */
//  override fun close() {
//    scope.cancel()
//    threadPool.shutdown()
//
//    try {
//      pulsarClient.close()
//    } catch (e: Exception) {
//      logger.warn(e) { "Error while closing Pulsar client" }
//    }
//
//    try {
//      pulsarAdmin.close()
//    } catch (e: Exception) {
//      logger.warn(e) { "Error while closing Pulsar admin" }
//    }
//  }
//
//  // create a topic if it does not exist already
//  private fun checkOrCreateTopic(topic: String, isPartitioned: Boolean, isDelayed: Boolean) {
//    val existing = infiniticAdmin.createTopic(topic, isPartitioned)
//    // set TTL for delayed topic if required
//    if (isDelayed && (!existing || pulsar.policies.forceUpdate))
//      try {
//        pulsarTopicPolicies.setMessageTTL(topic, pulsar.policies.delayedTTLInSeconds)
//      } catch (e: PulsarAdminException) {
//        logger.warn(e) {
//          "Exception when setting messageTTLInSeconds=${pulsar.policies.delayedTTLInSeconds} for topic $topic"
//        }
//      }
//  }
//
//  private fun checkOrCreateTopics() {
//    GlobalTopics.values().forEach {
//      checkOrCreateTopic(topicNames.topic(it), it.isPartitioned, it.isDelayed)
//    }
//
//    for (service in workerRegistry.services) {
//      ServiceTopics.values().forEach {
//        checkOrCreateTopic(topicNames.topic(it, service.key), it.isPartitioned, it.isDelayed)
//        checkOrCreateTopic(topicNames.topicDLQ(it, service.key), it.isPartitioned, it.isDelayed)
//      }
//    }
//
//    for (workflow in workerRegistry.workflows) {
//      WorkflowTopics.values().forEach {
//        checkOrCreateTopic(topicNames.topic(it, workflow.key), it.isPartitioned, it.isDelayed)
//        checkOrCreateTopic(topicNames.topicDLQ(it, workflow.key), it.isPartitioned, it.isDelayed)
//      }
//
//      WorkflowTaskTopics.values().forEach {
//        checkOrCreateTopic(topicNames.topic(it, workflow.key), it.isPartitioned, it.isDelayed)
//        checkOrCreateTopic(topicNames.topicDLQ(it, workflow.key), it.isPartitioned, it.isDelayed)
//      }
//    }
//  }
//}
