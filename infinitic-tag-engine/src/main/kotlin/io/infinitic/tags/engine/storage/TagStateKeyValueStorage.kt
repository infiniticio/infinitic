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

package io.infinitic.tags.engine.storage

/**
 * This StateStorage implementation converts state objects used by the engine to Avro objects, and saves
 * them in a persistent key value storage.
 */
// open class TagStateKeyValueStorage(
//    private val storage: KeyValueStorage,
//    private val cache: KeyValueCache<TagState>
// ) : TagStateStorage {
//
//    override val getStateFn: GetTagState = { tag: Tag ->
//        val key = getTagStateKey(tag)
//        cache.get(key) ?: run {
//            logger.debug("taskName {} - getStateFn - absent from cache, get from storage")
//            storage.getState(key)?.let { TagState.fromByteArray(it) }
//        }
//    }
//
//    override val putStateFn: PutTagState = { tag: Tag, state: TagState ->
//        val key = getTagStateKey(tag)
//        cache.put(key, state)
//        storage.putState(key, state.toByteArray())
//    }
//
//    override val delStateFn: DelTagState = { tag: Tag ->
//        val key = getTagStateKey(tag)
//        cache.del(key)
//        storage.delState(key)
//    }
//
//    private fun getTagStateKey(tag: Tag) = "tag.state.$tag"
//
//    /*
//    Use for tests
//     */
//    fun flush() {
//        if (storage is Flushable) {
//            storage.flush()
//        } else {
//            throw RuntimeException("Storage non flushable")
//        }
//        if (cache is Flushable) {
//            cache.flush()
//        } else {
//            throw Exception("Cache non flushable")
//        }
//    }
// }
