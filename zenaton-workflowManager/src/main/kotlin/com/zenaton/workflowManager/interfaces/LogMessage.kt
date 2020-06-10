package com.zenaton.workflowManager.interfaces

sealed class LogMessage(open val message: String) {

    data class Debug(
        override val message: String
    ) : LogMessage(message)

    data class Info(
        override val message: String
    ) : LogMessage(message)

    data class Warning(
        override val message: String
    ) : LogMessage(message)

    data class Error(
        override val message: String
    ) : LogMessage(message)

    data class Fatal(
        override val message: String
    ) : LogMessage(message)
}
