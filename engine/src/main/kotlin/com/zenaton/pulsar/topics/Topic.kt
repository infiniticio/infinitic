package com.zenaton.pulsar.topics

enum class Topic {
    WORKFLOWS {
        override fun get(name: String?) = "workflows"
    },
    TASKS {
        override fun get(name: String?) = "tasks"
    },
    DECISIONS {
        override fun get(name: String?) = "decisions"
    },
    DELAYS {
        override fun get(name: String?) = "delays"
    },
    TASK_ATTEMPTS {
        override fun get(name: String?) = "tasks-$name"
    },
    DECISION_ATTEMPTS {
        override fun get(name: String?) = "decisions-$name"
    },
    LOGS {
        override fun get(name: String?) = "logs"
    };

    abstract fun get(name: String? = ""): String
}
