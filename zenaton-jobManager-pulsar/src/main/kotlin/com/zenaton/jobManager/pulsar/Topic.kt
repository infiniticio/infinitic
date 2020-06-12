package com.zenaton.jobManager.pulsar

enum class Topic {
    ENGINE {
        override fun get(name: String?) = "${Topic.prefix}-engine"
    },
    WORKERS {
        override fun get(name: String?) = "${Topic.prefix}-workers-$name"
    },
    MONITORING_PER_INSTANCE {
        override fun get(name: String?) = "${Topic.prefix}-monitoring-per-instance"
    },
    MONITORING_PER_NAME {
        override fun get(name: String?) = "${Topic.prefix}-monitoring-per-name"
    },
    MONITORING_GLOBAL {
        override fun get(name: String?) = "${Topic.prefix}-monitoring-global"
    },
    LOGS {
        override fun get(name: String?) = "${Topic.prefix}-logs"
    };

    companion object {
        var prefix = "jobs"
    }

    abstract fun get(name: String? = ""): String
}
