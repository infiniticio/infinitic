package com.zenaton.taskManager.pulsar.dispatcher

enum class Topic {
    JOB_ENGINE {
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
    LOGS {
        override fun get(prefix: String, name: String?) = "$prefix-logs"
    };

    abstract fun get(prefix: String, name: String? = ""): String
}
