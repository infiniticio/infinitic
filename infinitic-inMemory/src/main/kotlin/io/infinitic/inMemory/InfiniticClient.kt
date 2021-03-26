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

package io.infinitic.inMemory

import io.infinitic.client.Client
import io.infinitic.common.workers.MessageToProcess
import io.infinitic.inMemory.transport.InMemoryClientOutput
import io.infinitic.inMemory.workers.startInMemory
import io.infinitic.storage.inMemory.InMemoryKeyValueStorage
import io.infinitic.tags.engine.transport.TagEngineMessageToProcess
import io.infinitic.tasks.TaskExecutorRegister
import io.infinitic.tasks.engine.transport.TaskEngineMessageToProcess
import io.infinitic.tasks.executor.register.TaskExecutorRegisterImpl
import io.infinitic.workflows.engine.transport.WorkflowEngineMessageToProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel

class InfiniticClient private constructor(
    private val taskExecutorRegister: TaskExecutorRegister,
    private val tagEngineCommandsChannel: Channel<TagEngineMessageToProcess>,
    private val taskEngineCommandsChannel: Channel<TaskEngineMessageToProcess>,
    private val workflowEngineCommandsChannel: Channel<WorkflowEngineMessageToProcess>,
    private val logFn: (_: MessageToProcess<*>) -> Unit
) : Client(
        InMemoryClientOutput(
            tagEngineCommandsChannel,
            taskEngineCommandsChannel,
            workflowEngineCommandsChannel
        )
    ),
    TaskExecutorRegister by taskExecutorRegister {

    companion object {
        @JvmStatic @JvmOverloads
        fun build(logFn: (_: MessageToProcess<*>) -> Unit = { }): InfiniticClient {
            val taskExecutorRegister = TaskExecutorRegisterImpl()
            val tagEngineCommandsChannel = Channel<TagEngineMessageToProcess>()
            val taskEngineCommandsChannel = Channel<TaskEngineMessageToProcess>()
            val workflowEngineCommandsChannel = Channel<WorkflowEngineMessageToProcess>()

            return InfiniticClient(
                taskExecutorRegister,
                tagEngineCommandsChannel,
                taskEngineCommandsChannel,
                workflowEngineCommandsChannel,
                logFn
            )
        }
    }

    init {
        with(CoroutineScope(Dispatchers.IO)) {
            startInMemory(
                taskExecutorRegister,
                InMemoryKeyValueStorage(),
                this@InfiniticClient,
                taskEngineCommandsChannel,
                workflowEngineCommandsChannel,
                logFn
            )
        }
    }
}
