// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.api.extensions.java.sql

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
