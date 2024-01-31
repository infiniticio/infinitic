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

package io.infinitic.events.properties

const val ERROR = "error"
const val RESULT = "result"
const val WORKER_NAME = "workerName"
const val INFINITIC_VERSION = "infiniticVersion"
const val REQUESTER = "requester"

const val SERVICE_NAME = "serviceName"
const val TASK_RETRY_SEQUENCE = "retrySequence"
const val TASK_RETRY_INDEX = "retryIndex"
const val TASK_RETRY_DELAY = "retryDelayMillis"
const val TASK_META = "taskMeta"
const val TASK_TAGS = "taskTags"
const val TASK_NAME = "taskName"
const val TASK_ARGS = "taskArgs"
const val TASK_ID = "taskId"
const val TASK_STATUS = "taskStatus"

const val WORKFLOW_ID = "workflowId"
const val WORKFLOW_NAME = "workflowName"
const val WORKFLOW_META = "workflowMeta"
const val WORKFLOW_TAGS = "workflowTags"
const val WORKFLOW_METHOD_ARGS = "workflowMethodArgs"
const val WORKFLOW_METHOD_NAME = "workflowMethodName"
const val WORKFLOW_METHOD_ID = "workflowMethodId"

const val CHANNEL_NAME = "channelName"
const val SIGNAL_ID = "signalId"
const val SIGNAL_DATA = "signalData"
const val TIMER_ID = "timerId"

/* Events */
const val WORKFLOW_EXECUTOR_FAILED = "workflowExecutorFailed"

const val TASK_COMPLETED = "taskCompleted"
const val TASK_FAILED = "taskFailed"
const val TASK_CANCELED = "taskCanceled"
const val TASK_TIMED_OUT = "taskTimedOut"
const val TASK_UNKNOWN = "taskUnknown"

const val CHILD_WORKFLOW_COMPLETED = "childWorkflowCompleted"
const val CHILD_WORKFLOW_FAILED = "childWorkflowFailed"
const val CHILD_WORKFLOW_CANCELED = "childWorkflowCanceled"
const val CHILD_WORKFLOW_TIMED_OUT = "childWorkflowTimedOut"
const val CHILD_WORKFLOW_UNKNOWN = "childWorkflowUnknown"
