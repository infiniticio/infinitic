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
package io.infinitic.common.tasks.tags.storage

import io.infinitic.common.data.MessageId
import io.infinitic.common.storage.Flushable
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskTag

/**
 * TagStateStorage implementations are responsible for storing the different state objects used by
 * the engine.
 *
 * No assumptions are made on whether the storage should be persistent or not, nor how the data
 * should be transformed before being stored. These details are left to the implementation.
 */
interface TaskTagStorage : Flushable {
  suspend fun getLastMessageId(tag: TaskTag, serviceName: ServiceName): MessageId?

  suspend fun setLastMessageId(tag: TaskTag, serviceName: ServiceName, messageId: MessageId)

  suspend fun getTaskIds(tag: TaskTag, serviceName: ServiceName): Set<TaskId>

  suspend fun addTaskId(tag: TaskTag, serviceName: ServiceName, taskId: TaskId)

  suspend fun removeTaskId(tag: TaskTag, serviceName: ServiceName, taskId: TaskId)
}
