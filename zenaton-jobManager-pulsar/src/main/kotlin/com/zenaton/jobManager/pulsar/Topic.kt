package com.zenaton.jobManager.pulsar

enum class Topic {
    ENGINE {
        override fun get(name: String?) = "${Topic.topicPrefix}-engine"
    },
    WORKERS {
        override fun get(name: String?) = "${Topic.topicPrefix}-workers-$name"
    },
    MONITORING_PER_INSTANCE {
        override fun get(name: String?) = "${Topic.topicPrefix}-monitoring-per-instance"
    },
    MONITORING_PER_NAME {
        override fun get(name: String?) = "${Topic.topicPrefix}-monitoring-per-name"
    },
    MONITORING_GLOBAL {
        override fun get(name: String?) = "${Topic.topicPrefix}-monitoring-global"
    },
    LOGS {
        override fun get(name: String?) = "${Topic.topicPrefix}-logs"
    };

    companion object {
        var topicPrefix = "job"
    }

    abstract fun get(name: String? = ""): String
}
