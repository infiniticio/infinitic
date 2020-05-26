package com.zenaton.taskmanager.metrics.state

interface TaskMetricsStateStorage {
    fun getState(key: String): TaskMetricsState?
    fun putState(key: String, state: TaskMetricsState)
    fun deleteState(key: String)
    fun incrCounter(key: String, amount: Long)
    fun getCounter(key: String): Long
}
