package com.zenaton.taskmanager.pulsar

enum class Topic {
    ENGINE {
        override fun get(name: String?) = "tasks"
    },
    WORKERS {
        override fun get(name: String?) = "tasks-$name"
    },
    METRICS {
        override fun get(name: String?) = "tasks-metrics"
    },
    LOGS {
        override fun get(name: String?) = "logs"
    };

    abstract fun get(name: String? = ""): String
}
