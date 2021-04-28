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

package io.infinitic.exceptions.workflows

import io.infinitic.common.data.Error
import io.infinitic.exceptions.RunException
import java.util.UUID

sealed class WorkflowRunException(
    msg: String,
    causeError: Error? = null
) : RunException(msg, causeError)

class CanceledDeferredException(val name: String?, val id: UUID) : WorkflowRunException(
    msg = "Canceled deferred: $name ($id)",
)

class FailedDeferredException(val name: String?, val id: UUID, error: Error? = null) : WorkflowRunException(
    msg = "Failed deferred: $name ($id)",
    causeError = error
)

class TimedOutDeferredException(val name: String?, val id: UUID) : WorkflowRunException(
    msg = "Timed-out deferred: $name ($id)",
)
