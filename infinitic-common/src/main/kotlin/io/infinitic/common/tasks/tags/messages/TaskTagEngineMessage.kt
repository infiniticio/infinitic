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

package io.infinitic.common.tasks.tags.messages

import io.infinitic.common.data.MessageId
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.common.tasks.data.TaskTag
import kotlinx.serialization.Serializable

@Serializable
sealed class TaskTagEngineMessage : Message {
    val messageId = MessageId()
    abstract val taskTag: TaskTag
    abstract val taskName: TaskName

    override fun envelope() = TaskTagEngineEnvelope.from(this)
}

@Serializable
data class RetryTaskPerTag(
    override val taskTag: TaskTag,
    override val taskName: TaskName,
    val methodName: MethodName?,
    val methodParameterTypes: MethodParameterTypes?,
    val methodParameters: MethodParameters?,
    val taskTags: Set<TaskTag>?,
    val taskMeta: TaskMeta?,
    val taskOptions: TaskOptions?
) : TaskTagEngineMessage()

@Serializable
data class CancelTaskPerTag(
    override val taskTag: TaskTag,
    override val taskName: TaskName
) : TaskTagEngineMessage()

@Serializable
data class AddTaskTag(
    override val taskTag: TaskTag,
    override val taskName: TaskName,
    val taskId: TaskId,
) : TaskTagEngineMessage()

@Serializable
data class RemoveTaskTag(
    override val taskTag: TaskTag,
    override val taskName: TaskName,
    val taskId: TaskId,
) : TaskTagEngineMessage()
