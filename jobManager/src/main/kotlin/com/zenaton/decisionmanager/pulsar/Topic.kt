package com.zenaton.decisionmanager.pulsar

enum class Topic {
    DECISIONS {
        override fun get(name: String?) = "decisions"
    },
    DECISIONS_ATTEMPTS {
        override fun get(name: String?) = "decisions-$name"
    },
    LOGS {
        override fun get(name: String?) = "logs"
    };

    abstract fun get(name: String? = ""): String
}
