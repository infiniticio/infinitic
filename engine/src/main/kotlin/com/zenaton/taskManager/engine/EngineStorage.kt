package com.zenaton.taskManager.engine

import com.zenaton.taskManager.data.JobId

interface EngineStorage {
    fun getState(jobId: JobId): EngineState?
    fun updateState(jobId: JobId, newState: EngineState, oldState: EngineState?)
    fun deleteState(jobId: JobId)
}
