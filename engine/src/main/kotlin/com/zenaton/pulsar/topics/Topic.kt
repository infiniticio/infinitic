package com.zenaton.pulsar.topics

import com.zenaton.engine.interfaces.data.NameInterface

enum class Topic {
    WORKFLOWS { override fun get(name: NameInterface?) = "workflows" },
    TASKS { override fun get(name: NameInterface?) = "tasks" },
    DELAYS { override fun get(name: NameInterface?) = "delays" },
    DECISIONS { override fun get(name: NameInterface?) = "decisions" },
    TASK_ATTEMPTS { override fun get(name: NameInterface?) = "tasks" },
    DECISION_ATTEMPTS { override fun get(name: NameInterface?) = "tasks" },
    LOGS { override fun get(name: NameInterface?) = "logs" };

    abstract fun get(name: NameInterface? = null): String
}
