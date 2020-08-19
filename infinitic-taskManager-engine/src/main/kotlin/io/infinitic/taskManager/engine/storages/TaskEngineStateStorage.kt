package io.infinitic.taskManager.engine.storages

import io.infinitic.taskManager.common.avro.AvroConverter
import io.infinitic.taskManager.common.data.TaskId
import io.infinitic.taskManager.engine.avroInterfaces.AvroStorage
import io.infinitic.taskManager.common.states.TaskEngineState

class TaskEngineStateStorage(val avroStorage: AvroStorage) {

    fun getState(taskId: TaskId): TaskEngineState? {
        return avroStorage.getTaskEngineState(taskId.id)?.let { AvroConverter.fromStorage(it) }
    }

    fun updateState(taskId: TaskId, newState: TaskEngineState, oldState: TaskEngineState?) {
        avroStorage.updateTaskEngineState(
            taskId.id,
            AvroConverter.toStorage(newState),
            oldState?.let { AvroConverter.toStorage(it) }
        )
    }

    fun deleteState(taskId: TaskId) {
        avroStorage.deleteTaskEngineState(taskId.id)
    }
}
