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

package io.infinitic.config

import io.infinitic.cache.StateCache
import io.infinitic.cache.caffeine.Caffeine
import io.infinitic.config.data.Pulsar
import io.infinitic.config.data.Task
import io.infinitic.config.data.Transport
import io.infinitic.config.data.Workflow
import io.infinitic.storage.StateStorage
import io.infinitic.storage.redis.Redis

data class WorkerConfig(
    /*
    Worker name - used to identify multiple workers
     */
    @JvmField val name: String? = null,

    /*
    Transport configuration
     */
    @JvmField val transport: Transport = Transport.pulsar,

    /*
    Default state storage
     */
    @JvmField var stateStorage: StateStorage? = null,

    /*
    Default state cache
     */
    @JvmField var stateCache: StateCache = StateCache.caffeine,

    /*
    Pulsar configuration
     */
    @JvmField val pulsar: Pulsar,

    /*
    Tasks configuration
     */
    @JvmField val tasks: List<Task> = listOf(),

    /*
    Workflows configuration
     */
    @JvmField val workflows: List<Workflow> = listOf(),

    /*
    Redis configuration
     */
    @JvmField val redis: Redis? = null,

    /*
    Caffeine configuration
     */
    @JvmField val caffeine: Caffeine? = null,

) {
    init {
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
