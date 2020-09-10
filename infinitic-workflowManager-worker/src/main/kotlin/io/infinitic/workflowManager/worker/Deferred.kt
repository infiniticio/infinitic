package io.infinitic.workflowManager.worker

import io.infinitic.workflowManager.common.data.commands.NewCommand
import io.infinitic.workflowManager.common.data.instructions.PastCommand
import io.infinitic.workflowManager.data.commands.CommandStatus
import io.infinitic.workflowManager.worker.exceptions.KnownStepException
import io.infinitic.workflowManager.worker.exceptions.NewStepException

class Deferred<T>(
    val id: String,
    private val newCommand: NewCommand,
    private val pastCommand: PastCommand?
) {
    fun await(): Deferred<T> {
        if (isOngoing()) {
            if (pastCommand == null) {
                throw NewStepException(newCommand)
            } else {
                throw KnownStepException()
            }
        }
        return this
    }

    fun result(): T? {
        if (isOngoing()) {
            await()
        }
        @Suppress("UNCHECKED_CAST")
        return pastCommand?.commandOutput as T?
    }

    // TODO Incorrect implementation, time must be taken into account
    fun status(): Status {
        if (pastCommand == null) return Status.ONGOING

        return when (pastCommand.commandStatus) {
            CommandStatus.DISPATCHED -> Status.ONGOING
            CommandStatus.COMPLETED -> Status.COMPLETED
            CommandStatus.CANCELED -> Status.CANCELED
        }
    }

    private fun isOngoing() = status() == Status.ONGOING

    enum class Status {
        ONGOING, CANCELED, COMPLETED
    }
}
