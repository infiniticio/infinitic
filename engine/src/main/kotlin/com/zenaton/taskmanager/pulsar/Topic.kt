package com.zenaton.taskmanager.pulsar

enum class Topic {
    TASKS {
        override fun get(name: String?) = "tasks"
    },
    TASK_ATTEMPTS {
        override fun get(name: String?) = "tasks-$name"
    },
    TASK_STATUS_UPDATES {
        override fun get(name: String?) = "task-status-updates"
    },
    LOGS {
        override fun get(name: String?) = "logs"
    };

    abstract fun get(name: String? = ""): String
}
