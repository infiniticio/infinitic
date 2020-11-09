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

package io.infinitic.storage.inmemory

import io.infinitic.storage.api.Flushable
import io.infinitic.storage.api.KeyValueStorage
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.LongAdder

class InMemoryStorage internal constructor() : KeyValueStorage, Flushable {
    private val stateStorage = ConcurrentHashMap<String, ByteBuffer>()
    private val counterStorage = ConcurrentHashMap<String, LongAdder>()

    override fun getState(key: String): ByteBuffer? = stateStorage[key]

    override fun putState(key: String, value: ByteBuffer) {
        stateStorage[key] = value
    }

    override fun updateState(key: String, value: ByteBuffer) = putState(key, value)

    override fun deleteState(key: String) {
        stateStorage.remove(key)
    }

    override fun incrementCounter(key: String, amount: Long) = counterStorage.computeIfAbsent(key) { LongAdder() }.add(amount)

    override fun getCounter(key: String): Long = counterStorage.computeIfAbsent(key) { LongAdder() }.sum()

    override fun flush() {
        stateStorage.clear()
        counterStorage.clear()
    }
}

fun inMemory(): InMemoryStorage = InMemoryStorage()
