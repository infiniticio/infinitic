package com.zenaton.taskmanager.data

enum class TaskStatus {
    RUNNING_OK {
        override fun isTerminated() = false
    },
    RUNNING_WARNING {
        override fun isTerminated() = false
    },
    RUNNING_ERROR {
        override fun isTerminated() = false
    },
    TERMINATED_COMPLETED {
        override fun isTerminated() = true
    },
    TERMINATED_CANCELED {
        override fun isTerminated() = true
    };

    abstract fun isTerminated(): Boolean
}
