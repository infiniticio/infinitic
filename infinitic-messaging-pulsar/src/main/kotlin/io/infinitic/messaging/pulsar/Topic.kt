package io.infinitic.messaging.pulsar

enum class Topic {
    WORKFLOW_ENGINE {
        override fun get(name: String) = "workflows-engine"
    },
    TASK_ENGINE {
        override fun get(name: String) = "tasks-engine"
    },
    WORKERS {
        override fun get(name: String) = "tasks-workers-$name"
    },
    MONITORING_PER_INSTANCE {
        override fun get(name: String) = "tasks-monitoring-per-instance"
    },
    MONITORING_PER_NAME {
        override fun get(name: String) = "tasks-monitoring-per-name"
    },
    MONITORING_GLOBAL {
        override fun get(name: String) = "tasks-monitoring-global"
    },
    LOGS {
        override fun get(name: String) = "tasks-logs"
    };

    abstract fun get(name: String = ""): String
}
