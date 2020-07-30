package com.zenaton.workflowManager

import com.zenaton.workflowManager.avroConverter.AvroConverter
import com.zenaton.workflowManager.states.WorkflowEngineState
import com.zenaton.workflowManager.utils.TestFactory

fun main() {
    val o1 = TestFactory.random(WorkflowEngineState::class)
    println(o1)
    val o2 = AvroConverter.toStorage(o1)
    println(o2)
    val o3 = AvroConverter.fromStorage(o2)
    println(o3)
    val o4 = AvroConverter.toStorage(o3)
    println(o4)
}
