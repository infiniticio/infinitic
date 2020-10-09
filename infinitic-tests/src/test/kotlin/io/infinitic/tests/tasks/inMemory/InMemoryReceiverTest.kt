package io.infinitic.tests.tasks.inMemory

import io.infinitic.common.tasks.data.TaskStatus
import io.infinitic.common.tasks.messages.TaskStatusUpdated
import kotlinx.coroutines.channels.Channel

class InMemoryReceiverTest<T>(private val transport: Channel<T>) : InMemoryReceiver<T>(transport) {
    lateinit var taskStatus: TaskStatus
    override suspend fun onMessage(apply: (T) -> Unit): T {
        val msg = super.onMessage(apply)

        if (msg is TaskStatusUpdated) {
            taskStatus = msg.newStatus
        }

        return msg
    }
}
