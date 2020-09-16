package io.infinitic.taskManager.dispatcher.pulsar

enum class Topic {
    TASK_ENGINE {
        override fun get(prefix: String, name: String?) = "$prefix-engine"
    },
    WORKERS {
        override fun get(prefix: String, name: String?) = "$prefix-workers-$name"
    },
    MONITORING_PER_NAME {
        override fun get(prefix: String, name: String?) = "$prefix-monitoring-per-name"
    },
    MONITORING_GLOBAL {
        override fun get(prefix: String, name: String?) = "$prefix-monitoring-global"
    },
    WORKFLOW_ENGINE {
        override fun get(prefix: String, name: String?) = "workflows"
    },
    DELAYS {
        override fun get(prefix: String, name: String?) = "delays"
    },
    LOGS {
        override fun get(prefix: String, name: String?) = "$prefix-logs"
    };

    abstract fun get(prefix: String, name: String? = ""): String
}
