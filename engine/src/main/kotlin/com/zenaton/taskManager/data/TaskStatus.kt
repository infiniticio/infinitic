package com.zenaton.taskManager.data

enum class TaskStatus {
    RUNNING_OK {
        override val isTerminated: Boolean get() = false
    },
    RUNNING_WARNING {
        override val isTerminated: Boolean get() = false
    },
    RUNNING_ERROR {
        override val isTerminated: Boolean get() = false
    },
    TERMINATED_COMPLETED {
        override val isTerminated: Boolean get() = true
    },
    TERMINATED_CANCELED {
        override val isTerminated: Boolean get() = true
    };

    abstract val isTerminated: Boolean
}
