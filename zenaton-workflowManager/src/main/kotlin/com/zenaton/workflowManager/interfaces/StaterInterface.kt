package com.zenaton.workflowManager.interfaces

import com.zenaton.commons.data.interfaces.StateInterface

interface StaterInterface<T : StateInterface> {
    fun getState(key: String): T?
    fun createState(key: String, state: T)
    fun updateState(key: String, state: T)
    fun deleteState(key: String)
}
