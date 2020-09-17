package io.infinitic.messaging.pulsar

enum class Topic {
    WORKFLOW_ENGINE {
        override fun get(prefix: String, name: String?) = "workflows"
    },
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
    DELAYS {
        override fun get(prefix: String, name: String?) = "delays"
    },
    LOGS {
        override fun get(prefix: String, name: String?) = "$prefix-logs"
    };

    abstract fun get(prefix: String, name: String? = ""): String
}
