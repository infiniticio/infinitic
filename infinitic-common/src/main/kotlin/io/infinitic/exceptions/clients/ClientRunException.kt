/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.exceptions.clients

import io.infinitic.common.errors.Error
import io.infinitic.exceptions.RunException

sealed class ClientRunException(
    msg: String,
    help: String,
    causeError: Error? = null
) : RunException("$msg.\n$help", causeError)

class CanceledException(
    val id: String,
    val name: String,
) : ClientRunException(
    msg = "canceled deferred: $name ($id)",
    help = ""
)

class FailedException(
    val id: String,
    val name: String,
    causeError: Error
) : ClientRunException(
    msg = "failed deferred: $name ($id)",
    help = "",
    causeError = causeError
)

data class TimedOutDeferredException(
    val id: String,
    val name: String
) : ClientRunException(
    msg = "timed out deferred: $name ($id)",
    help = ""
)

class UnknownTaskException(
    taskId: String,
    taskName: String,
) : ClientRunException(
    msg = "Unknown task instance: $taskName ($taskId)",
    help = "This instance is probably already completed"
)

class UnknownException(
    workflowId: String,
    workflowName: String
) : ClientRunException(
    msg = "Unknown workflow instance: $workflowName ($workflowId)",
    help = "This instance is probably already completed"
)

class AlreadyCompletedException(
    workflowId: String,
    workflowName: String
) : ClientRunException(
    msg = "Workflow's method is already completed: $workflowName ($workflowId)",
    help = ""
)
