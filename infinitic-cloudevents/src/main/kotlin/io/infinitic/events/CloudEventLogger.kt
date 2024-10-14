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
package io.infinitic.events

import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.messages.Message
import io.infinitic.common.transport.ClientTopic
import io.infinitic.common.transport.NamingTopic
import io.infinitic.common.transport.ServiceExecutorEventTopic
import io.infinitic.common.transport.ServiceExecutorRetryTopic
import io.infinitic.common.transport.ServiceExecutorTopic
import io.infinitic.common.transport.ServiceTagEngineTopic
import io.infinitic.common.transport.Topic
import io.infinitic.common.transport.WorkflowExecutorEventTopic
import io.infinitic.common.transport.WorkflowExecutorRetryTopic
import io.infinitic.common.transport.WorkflowExecutorTopic
import io.infinitic.common.transport.WorkflowStateCmdTopic
import io.infinitic.common.transport.WorkflowStateEngineTopic
import io.infinitic.common.transport.WorkflowStateEventTopic
import io.infinitic.common.transport.WorkflowStateTimerTopic
import io.infinitic.common.transport.WorkflowTagEngineTopic
import io.infinitic.logger.ignoreNull

private const val INFINITIC_PREFIX = "io.infinitic"
private const val CLOUD_EVENTS = "$INFINITIC_PREFIX.cloudEvents"

private const val WORKFLOW_STATE_ENGINE = "WorkflowStateEngine"
private const val WORKFLOW_TAG_ENGINE = "WorkflowTagEngine"
private const val WORKFLOW_EXECUTOR = "WorkflowExecutor"
private const val SERVICE_TAG_ENGINE = "ServiceTagEngine"
private const val SERVICE_EXECUTOR = "ServiceExecutor"

const val LOGS_WORKFLOW_STATE_ENGINE = "$CLOUD_EVENTS.$WORKFLOW_STATE_ENGINE"
const val LOGS_WORKFLOW_TAG_ENGINE = "$CLOUD_EVENTS.$WORKFLOW_TAG_ENGINE"
const val LOGS_WORKFLOW_EXECUTOR = "$CLOUD_EVENTS.$WORKFLOW_EXECUTOR"
const val LOGS_SERVICE_TAG_ENGINE = "$CLOUD_EVENTS.$SERVICE_TAG_ENGINE"
const val LOGS_SERVICE_EXECUTOR = "$CLOUD_EVENTS.$SERVICE_EXECUTOR"

class CloudEventLogger(
  private val topic: Topic<*>,
  entity: String,
  private val prefix: String,
  private val beautify: Boolean
) {
  private val logger = topic.eventLogger(entity)?.ignoreNull()

  fun log(message: Message, publishedAt: MillisInstant) = try {
    logger?.debug {
      message.toCloudEvent(topic, publishedAt, prefix)?.toJsonString(beautify)
    }
  } catch (e: Exception) {
    logger?.warn(e) { "Error while logging the CloudEvent json of: $message" }
  }

  private fun Topic<*>.eventLogger(entity: String) = when (this) {
    ClientTopic -> null
    NamingTopic -> null
    ServiceExecutorEventTopic -> "$LOGS_SERVICE_EXECUTOR.$entity"
    ServiceExecutorRetryTopic -> null
    ServiceExecutorTopic -> "$LOGS_SERVICE_EXECUTOR.$entity"
    ServiceTagEngineTopic -> "$LOGS_SERVICE_TAG_ENGINE.$entity"
    WorkflowExecutorEventTopic -> "$LOGS_WORKFLOW_EXECUTOR.$entity"
    WorkflowExecutorRetryTopic -> null
    WorkflowExecutorTopic -> "$LOGS_WORKFLOW_EXECUTOR.$entity"
    WorkflowStateCmdTopic -> "$LOGS_WORKFLOW_STATE_ENGINE.$entity"
    WorkflowStateEngineTopic -> "$LOGS_WORKFLOW_STATE_ENGINE.$entity"
    WorkflowStateEventTopic -> "$LOGS_WORKFLOW_STATE_ENGINE.$entity"
    WorkflowStateTimerTopic -> null
    WorkflowTagEngineTopic -> "$LOGS_WORKFLOW_TAG_ENGINE.$entity"
  }?.let {
    KotlinLogging.logger(it)
  }
}

