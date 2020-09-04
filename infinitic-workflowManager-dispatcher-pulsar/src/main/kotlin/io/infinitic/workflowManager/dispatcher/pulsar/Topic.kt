package io.infinitic.workflowManager.dispatcher.pulsar

enum class Topic {
    TASK_ENGINE {
        override fun get(prefix: String, name: String?) = "$prefix-engine"
    },
    WORKFLOW_ENGINE {
        override fun get(prefix: String, name: String?) = "workflows"
    };

    abstract fun get(prefix: String, name: String? = ""): String
}
