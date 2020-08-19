package com.zenaton.workflowManager.engine.pulsar.dispatcher

enum class Topic {
    WORKFLOW_ENGINE {
        override fun get(name: String?) = "workflows"
    },
    DELAYS {
        override fun get(name: String?) = "delays"
    },
    LOGS {
        override fun get(name: String?) = "logs"
    };

    abstract fun get(name: String? = ""): String
}
