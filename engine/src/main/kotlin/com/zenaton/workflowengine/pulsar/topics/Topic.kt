package com.zenaton.workflowengine.pulsar.topics

enum class Topic {
    WORKFLOWS {
        override fun get(name: String?) = "workflows"
    },
    DECISIONS {
        override fun get(name: String?) = "decisions"
    },
    DELAYS {
        override fun get(name: String?) = "delays"
    },
    DECISION_ATTEMPTS {
        override fun get(name: String?) = "decisions-$name"
    },
    LOGS {
        override fun get(name: String?) = "logs"
    };

    abstract fun get(name: String? = ""): String
}
