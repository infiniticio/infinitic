package com.zenaton.jobManager.engine

import com.zenaton.jobManager.data.JobId

interface EngineStorage {
    fun getState(jobId: JobId): EngineState?
    fun updateState(jobId: JobId, newState: EngineState, oldState: EngineState?)
    fun deleteState(jobId: JobId)
}
