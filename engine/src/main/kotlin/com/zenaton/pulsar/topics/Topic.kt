package com.zenaton.pulsar.topics

enum class Topic(val topic: String) {
    WORKFLOWS("workflows"),
    TASKS("tasks"),
    DELAYS("delays"),
    DECISIONS("decisions"),
    LOGS("logs")
}
