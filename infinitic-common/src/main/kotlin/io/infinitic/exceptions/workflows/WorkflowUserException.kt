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

import io.infinitic.common.workers.config.WorkflowVersion
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.exceptions.UserException
import io.infinitic.workflows.Channel

sealed class WorkflowUserException(
    msg: String,
    help: String
) : UserException("$msg.\n$help")

object InvalidInlineException : WorkflowUserException(
    msg = "Task or workflow must not be used inside an inline function",
    help = ""
)

object MultipleCustomIdException : WorkflowUserException(
    msg = "",
    help = ""
)

object OutOfBoundAwaitException : WorkflowUserException(
    msg = "",
    help = ""
)

class NonIdempotentChannelGetterException(
    workflow: String,
    method: String
) : WorkflowUserException(
    msg = "in workflow $workflow, method $method should return the same object when called multiple times",
    help = ""
)

class MultipleGettersForSameChannelException(
    workflow: String,
    method: String,
    otherMethod: String
) : WorkflowUserException(
    msg = "in workflow $workflow, getter $method and $otherMethod return the same channel",
    help = "Make sure to not have multiple getters returning the same channel"
)

object ChannelWithoutGetterException : WorkflowUserException(
    msg = "A ${Channel::class.simpleName} is used without getter",
    help = "Make sure to add a getter of this channel to the workflow interface"
)

class WorkflowChangedException(
    workflow: String,
    method: String,
    position: String
) : WorkflowUserException(
    msg = "Workflow \"$workflow\" has been updated since its launch (detected at position $position in $method)",
    help = "You can either kill this instance or restore the workflow definition to be able to resume it"
)

class UnknownWorkflowVersionException(
    workflowName: WorkflowName,
    workflowVersion: WorkflowVersion?
) : WorkflowUserException(
    msg = "Unknown version \"$workflowVersion\" for Workflow \"$workflowName\"",
    help = ""
)
