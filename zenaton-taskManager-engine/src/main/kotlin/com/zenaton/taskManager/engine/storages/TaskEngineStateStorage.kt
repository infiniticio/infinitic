package com.zenaton.taskManager.engine.storages

import com.zenaton.taskManager.common.avro.AvroConverter
import com.zenaton.taskManager.common.data.TaskId
import com.zenaton.taskManager.engine.avroInterfaces.AvroStorage
import com.zenaton.taskManager.common.states.TaskEngineState

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
