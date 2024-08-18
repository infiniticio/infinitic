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
package io.infinitic.workers.register.config

import io.infinitic.common.workers.config.ExponentialBackoffRetryPolicy
import io.infinitic.events.config.EventListenerConfig
import io.infinitic.events.config.EventLoggerConfig
import io.infinitic.tasks.WithRetry
import io.infinitic.tasks.WithTimeout
import io.infinitic.tasks.tag.config.ServiceTagEngineConfig
import io.infinitic.workflows.engine.config.WorkflowStateEngineConfig
import io.infinitic.workflows.tag.config.WorkflowTagEngineConfig

/**
 * Note: Final default values for withRetry, withTimeout and workflow check mode
 * are in TaskExecutors as they can be defined through annotations as well
 */
internal const val DEFAULT_CONCURRENCY = 1

internal const val UNDEFINED_TIMEOUT = -Double.MAX_VALUE

internal val UNDEFINED_WITH_TIMEOUT = WithTimeout { UNDEFINED_TIMEOUT }

internal val UNDEFINED_RETRY = ExponentialBackoffRetryPolicy().apply { isDefined = false }

internal val UNDEFINED_WITH_RETRY = WithRetry { _: Int, _: Exception -> null }

internal val UNDEFINED_EVENT_LISTENER = EventListenerConfig().apply { isDefined = false }

internal val UNDEFINED_EVENT_LOGGER = EventLoggerConfig().apply { isDefined = false }

internal val DEFAULT_SERVICE_TAG_ENGINE = ServiceTagEngineConfig().apply { isDefault = true }

internal val DEFAULT_WORKFLOW_STATE_ENGINE = WorkflowStateEngineConfig().apply { isDefault = true }

internal val DEFAULT_WORKFLOW_TAG_ENGINE = WorkflowTagEngineConfig().apply { isDefault = true }

