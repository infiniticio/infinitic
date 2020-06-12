package com.zenaton.api.extensions.java.sql

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import java.sql.Connection
import java.sql.ResultSet
import java.sql.PreparedStatement

typealias Binder = (PreparedStatement) -> Unit

data class SqlStatement(val sql: String, val binder: Binder? = null) {
    operator fun invoke(preparedStatement: PreparedStatement) =
        preparedStatement.apply { binder?.invoke(preparedStatement) }
}

typealias Extractor<T> = (ResultSet) -> T

fun <T> Connection.query(sql: String, extractor: Extractor<T>): List<T> = query(SqlStatement(sql), extractor)

fun <T> Connection.query(sqlStatement: SqlStatement, extractor: Extractor<T>): List<T> =
    runBlocking {
        mutableListOf<T>().apply {
            asFlow(sqlStatement, extractor).toList(this)
        }
    }

fun <T> Connection.asFlow(sqlStatement: SqlStatement, extractor: Extractor<T>): Flow<T> = flow {
    sqlStatement(sqlStatement).use { statement ->
        statement.executeQuery().use { rs ->
            while (rs.next()) {
                emit(extractor(rs))
            }
        }
    }
}

fun Connection.sqlStatement(sqlStatement: SqlStatement): PreparedStatement =
    prepareStatement(sqlStatement.sql).apply { sqlStatement(this) }
