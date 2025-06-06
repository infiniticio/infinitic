/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */
package io.infinitic.storage.databases.postgres

import com.zaxxer.hikari.HikariDataSource
import io.infinitic.storage.config.PostgresConfig
import io.infinitic.storage.keySet.KeySetStorage
import org.jetbrains.annotations.TestOnly
import java.sql.ResultSet

class PostgresKeySetStorage(
  internal val pool: HikariDataSource,
  private val tableName: String,
  private val schema: String
) : KeySetStorage {

  companion object {
    fun from(config: PostgresConfig) = PostgresKeySetStorage(
        config.getPool(),
        config.keySetTable,
        config.schema,
    )
  }

  init {
    // Create table if needed
    initKeySetTable()
  }

  override fun close() {
    pool.close()
  }

  override suspend fun get(key: String): Set<ByteArray> =
      pool.connection.use { connection ->
        connection.prepareStatement(
            "SELECT value FROM $schema.$tableName WHERE key = ?",
        )
            .use { statement ->
              statement.setString(1, key)
              statement.executeQuery().use {
                val values = mutableSetOf<ByteArray>()
                while (it.next()) {
                  values.add(it.getBytes("value"))
                }
                values
              }
            }
      }

  override suspend fun add(key: String, value: ByteArray) {
    pool.connection.use { connection ->
      connection.prepareStatement("INSERT INTO $schema.$tableName (key, value) VALUES (?, ?)").use {
        it.setString(1, key)
        it.setBytes(2, value)
        it.executeUpdate()
      }
    }
  }

  override suspend fun remove(key: String, value: ByteArray) {
    pool.connection.use { connection ->
      connection.prepareStatement("DELETE FROM $schema.$tableName WHERE key = ? AND value = ?")
          .use {
            it.setString(1, key)
            it.setBytes(2, value)
            it.executeUpdate()
          }
    }
  }

  override suspend fun get(keys: Set<String>): Map<String, Set<ByteArray>> {
    if (keys.isEmpty()) return emptyMap() // Handle empty case

    return pool.connection.use { connection ->
      connection.prepareStatement(
          """
            SELECT key, value
            FROM ${schema}.${tableName}
            WHERE key = ANY(?)
        """,
      ).use { statement ->
        statement.setArray(1, connection.createArrayOf("VARCHAR", keys.toTypedArray()))
        statement.executeQuery().use { resultSet -> buildResultMap(keys, resultSet) }
      }
    }
  }

  private fun buildResultMap(
    keys: Set<String>,
    resultSet: ResultSet
  ): Map<String, MutableSet<ByteArray>> {
    val results = mutableMapOf<String, MutableSet<ByteArray>>()
    while (resultSet.next()) {
      val key = resultSet.getString("key")
      val value = resultSet.getBytes("value")
      results.computeIfAbsent(key) { mutableSetOf() }.add(value)
    }
    // add missing keys
    keys.forEach { key ->
      results.putIfAbsent(key, mutableSetOf())
    }
    return results
  }

  override suspend fun update(
    add: Map<String, Set<ByteArray>>,
    remove: Map<String, Set<ByteArray>>
  ) {
    pool.connection.use { connection ->
      connection.autoCommit = false
      try {
        if (remove.isNotEmpty()) connection
            .prepareStatement("DELETE FROM $schema.$tableName WHERE key = ? AND value = ?")
            .use { deleteStmt ->
              remove.forEach { (key, values) ->
                values.forEach { value ->
                  deleteStmt.setString(1, key)
                  deleteStmt.setBytes(2, value)
                  deleteStmt.addBatch()
                }
              }
              deleteStmt.executeBatch()
            }

        if (add.isNotEmpty()) connection
            .prepareStatement("INSERT INTO $schema.$tableName (key, value) VALUES (?, ?)")
            .use { insertStmt ->
              add.forEach { (key, values) ->
                values.forEach { value ->
                  insertStmt.setString(1, key)
                  insertStmt.setBytes(2, value)
                  insertStmt.addBatch()
                }
              }
              insertStmt.executeBatch()
            }

        connection.commit()
      } catch (e: Exception) {
        connection.rollback()
        throw e
      } finally {
        connection.autoCommit = true
      }
    }
  }

  @TestOnly
  override fun flush() {
    pool.connection.use { connection ->
      connection.prepareStatement("TRUNCATE $schema.$tableName").use { it.executeUpdate() }
    }
  }

  private fun initKeySetTable() {
    // Here key is typically a tag
    // And value is typically a workflowId
    pool.connection.use { connection ->
      connection.prepareStatement(
          "CREATE TABLE IF NOT EXISTS $schema.$tableName (" +
              "id SERIAL PRIMARY KEY," +
              "key VARCHAR(255) NOT NULL," +
              "value BYTEA NOT NULL" +
              ");",
      ).use { it.executeUpdate() }

      connection.prepareStatement(
          "CREATE INDEX IF NOT EXISTS index_key ON $schema.$tableName (key);",
      ).use { it.executeUpdate() }

      connection.prepareStatement(
          "CREATE INDEX IF NOT EXISTS index_key_value ON $schema.$tableName (key, value);",
      ).use { it.executeUpdate() }
    }
  }
}
