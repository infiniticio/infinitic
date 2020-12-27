/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.storage.kodein

import io.infinitic.common.storage.Flushable
import io.infinitic.common.storage.keyValue.KeyValueStorage
import org.kodein.db.DB
import org.kodein.db.Key
import org.kodein.db.delete
import org.kodein.db.deleteAll
import org.kodein.db.find
import org.kodein.db.impl.open
import org.kodein.db.key
import org.kodein.db.ldb.LevelDBOptions
import java.nio.ByteBuffer

class KodeinStorage(private val kodein: Kodein) : KeyValueStorage, Flushable {

    private val db = DB.open(kodein.path, LevelDBOptions.PrintLogs(true))

    init {
        Runtime.getRuntime().addShutdownHook(
            Thread() {
                db.close()
            }
        )
    }

    override fun flush() {
        val counters = db.find<Counter>().all()
        db.deleteAll(counters)

        val states = db.find<State>().all()
        db.deleteAll(states)
    }

    override suspend fun getState(key: String): ByteBuffer? {
        val kodeinKey: Key<State> = db.key<State>(key)

        return db[State::class, kodeinKey]?.value
    }

    override suspend fun putState(key: String, value: ByteBuffer) = updateState(key, value)

    override suspend fun updateState(key: String, value: ByteBuffer) {
        db.put(State(key, value))
    }

    override suspend fun deleteState(key: String) {
        val kodeinKey = db.key<State>(key)

        db.delete(kodeinKey)
    }

    override suspend fun incrementCounter(key: String, amount: Long) {
        val value = getCounter(key) + amount

        db.put(Counter(key, value))
    }

    override suspend fun getCounter(key: String): Long {
        val kodeinKey = db.key<Counter>(key)

        return db[Counter::class, kodeinKey]?.value ?: 0L
    }
}
