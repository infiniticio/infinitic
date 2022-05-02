package io.infinitic.storage.jdbc

import com.sksamuel.hoplite.Secret

data class JDBC (
    val host: String = "127.0.0.1",
    var port: Int = 3306,
    var timeout: Int = 30000,
    var user: String? = null,
    var password: Secret? = null,
    var database: String = ""
)
