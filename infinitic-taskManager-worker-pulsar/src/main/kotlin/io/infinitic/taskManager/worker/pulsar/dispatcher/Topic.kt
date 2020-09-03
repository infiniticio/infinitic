package io.infinitic.taskManager.worker.pulsar.dispatcher

enum class Topic {
    TASK_ENGINE {
        override fun get(prefix: String, name: String?) = "$prefix-engine"
    };

    abstract fun get(prefix: String, name: String? = ""): String
}
