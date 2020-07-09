package com.zenaton.workflowManager.avroEngines.jobInMemory

class WorkflowA : Workflow {
    override lateinit var runTask: (String, String, Array<out Any>) -> Any

    fun handle() {
        runTask("taskA", "handle", arrayOf())
        runTask("taskB", "handle", arrayOf())
        runTask("taskC", "handle", arrayOf())
    }
}
