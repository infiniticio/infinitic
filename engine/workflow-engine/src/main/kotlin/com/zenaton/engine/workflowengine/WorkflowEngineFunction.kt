package com.zenaton.engine.workflowengine

import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class WorkflowEngineFunction : Function<String, Void> {
    override fun process(input: String?, context: Context?): Void? {
        TODO()
    }
}
