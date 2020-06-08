package com.zenaton.jobManager.pulsar

enum class Topic {
    ENGINE {
        override fun get(name: String?) = "engine"
    },
    WORKERS {
        override fun get(name: String?) = "workers-$name"
    },
    MONITORING_PER_INSTANCE {
        override fun get(name: String?) = "monitoring-per-instance"
    },
    MONITORING_PER_NAME {
        override fun get(name: String?) = "monitoring-per-name"
    },
    MONITORING_GLOBAL {
        override fun get(name: String?) = "monitoring-global"
    },
    LOGS {
        override fun get(name: String?) = "logs"
    };

    abstract fun get(name: String? = ""): String
}
