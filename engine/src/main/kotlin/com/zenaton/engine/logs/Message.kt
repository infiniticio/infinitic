package com.zenaton.engine.logs

sealed class Message() {

    data class Debug(
        val message: String
    ) : Message()

    data class Info(
        val message: String
    ) : Message()

    data class Warning(
        val message: String
    ) : Message()

    data class Error(
        val message: String
    ) : Message()

    data class Fatal(
        val message: String
    ) : Message()
}
