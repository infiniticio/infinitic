package com.zenaton.workflowManager.avroEngines.jobInMemory

interface Workflow {
    var runTask: (String, String, Array<out Any>) -> Any
}
