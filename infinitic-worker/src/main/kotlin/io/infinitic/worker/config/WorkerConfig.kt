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

package io.infinitic.worker.config

import io.infinitic.cache.CacheConfig
import io.infinitic.cache.StateCache
import io.infinitic.cache.caffeine.Caffeine
import io.infinitic.common.config.loadConfigFromFile
import io.infinitic.common.config.loadConfigFromResource
import io.infinitic.storage.StateStorage
import io.infinitic.storage.StorageConfig
import io.infinitic.storage.redis.Redis
import io.infinitic.transport.Transport
import io.infinitic.transport.TransportConfig
import io.infinitic.transport.pulsar.Pulsar

data class WorkerConfig(
    /*
    Worker name
     */
    val name: String? = null,

    /*
    Transport configuration
     */
    override val transport: Transport = Transport.pulsar,

    /*
    Pulsar configuration
     */
    override val pulsar: Pulsar?,

    /*
    Default state storage
     */
    override var stateStorage: StateStorage? = null,

    /*
    Redis configuration
     */
    override val redis: Redis? = null,

    /*
    Default state cache
     */
    override var stateCache: StateCache = StateCache.caffeine,

    /*
    Caffeine configuration
     */
    override val caffeine: Caffeine? = null,

    /*
    Tasks configuration
     */
    val tasks: List<Task> = listOf(),

    /*
    Workflows configuration
     */
    val workflows: List<Workflow> = listOf(),

) : CacheConfig, StorageConfig, TransportConfig {

    companion object {
        /**
         * Create WorkerConfig from file in file system
         */
        @JvmStatic
        fun fromFile(vararg files: String): WorkerConfig = loadConfigFromFile(files.toList())

        /**
         * Create WorkerConfig from file in resources directory
         */
        @JvmStatic
        fun fromResource(vararg resources: String): WorkerConfig = loadConfigFromResource(resources.toList())
    }

    init {
        require(stateStorage != null) { "`stateStorage` can not be null for $name" }

        if (transport == Transport.pulsar) {
            require(pulsar != null) { "`${Transport.pulsar}` is used but not configured" }
        }

        // apply default, if not set
        tasks.map { task ->
            task.tagEngine?.let {
                it.stateStorage = it.stateStorage ?: stateStorage
                checkStateStorage(it.stateStorage, "tasks.${task.name}.tagEngine.stateStorage")
                it.stateCache = it.stateCache ?: stateCache
                if (it.default) it.concurrency = task.concurrency
            }
            task.taskEngine?.let {
                it.stateStorage = it.stateStorage ?: stateStorage
                checkStateStorage(it.stateStorage, "tasks.${task.name}.taskEngine.stateStorage")
                it.stateCache = it.stateCache ?: stateCache
                if (it.default) it.concurrency = task.concurrency
            }
            task.metrics?.let {
                it.stateStorage = it.stateStorage ?: stateStorage
                checkStateStorage(it.stateStorage, "tasks.${task.name}.metrics.stateStorage")
                it.stateCache = it.stateCache ?: stateCache
            }
        }

        workflows.map { workflow ->
            workflow.tagEngine?.let {
                it.stateStorage = it.stateStorage ?: stateStorage
                checkStateStorage(it.stateStorage, "workflows.${workflow.name}.tagEngine.stateStorage")
                it.stateCache = it.stateCache ?: stateCache
                if (it.default) it.concurrency = workflow.concurrency
            }
            workflow.taskEngine?.let {
                it.stateStorage = it.stateStorage ?: stateStorage
                checkStateStorage(it.stateStorage, "workflows.${workflow.name}.taskEngine.stateStorage")
                it.stateCache = it.stateCache ?: stateCache
                if (it.default) it.concurrency = workflow.concurrency
            }
            workflow.workflowEngine?.let {
                it.stateStorage = it.stateStorage ?: stateStorage
                checkStateStorage(it.stateStorage, "workflows.${workflow.name}.workflowEngine.stateStorage")
                it.stateCache = it.stateCache ?: stateCache
                if (it.default) it.concurrency = workflow.concurrency
            }
        }
    }

    private fun checkStateStorage(stateStorage: StateStorage?, name: String) {
        require(stateStorage != null) { "`stateStorage` can not be null for $name" }

        if (stateStorage == StateStorage.redis) {
            require(redis != null) { "`${StateStorage.redis}` is used for $name but not configured" }
        }
    }
}
