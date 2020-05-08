package com.zenaton.pulsar.topics

import com.zenaton.engine.data.interfaces.NameInterface

enum class Topic {
    WORKFLOWS {
        override fun get(name: NameInterface?) = "workflows"
    },
    TASKS {
        override fun get(name: NameInterface?) = "tasks"
    },
    DECISIONS {
        override fun get(name: NameInterface?) = "decisions"
    },
    DELAYS {
        override fun get(name: NameInterface?) = "delays"
    },
    TASK_ATTEMPTS {
        override fun get(name: NameInterface?) = "tasks-$name"
    },
    DECISION_ATTEMPTS {
        override fun get(name: NameInterface?) = "decisions-$name"
    },
    LOGS {
        override fun get(name: NameInterface?) = "logs"
    };

    abstract fun get(name: NameInterface? = null): String
}
