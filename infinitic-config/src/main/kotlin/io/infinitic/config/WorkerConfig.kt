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
import io.infinitic.config.data.Metrics
import io.infinitic.config.data.Pulsar
import io.infinitic.config.data.TagEngine
import io.infinitic.config.data.Task
import io.infinitic.config.data.TaskEngine
import io.infinitic.config.data.Workflow
import io.infinitic.config.data.WorkflowEngine
import io.infinitic.config.merge.merge
import io.infinitic.storage.StateStorage
import io.infinitic.storage.redis.Redis

data class WorkerConfig(
    /*
    Worker name - used to identify multiple workers
     */
    @JvmField val name: String? = null,

    /*
    Default state storage
     */
    @JvmField var stateStorage: StateStorage? = null,

    /*
    Default state cache
     */
    @JvmField var stateCache: StateCache? = null,

    /*
    Pulsar configuration
     */
    @JvmField val pulsar: Pulsar,

    /*
    Default workflow engine configuration
     */
    @JvmField val workflowEngine: WorkflowEngine? = null,

    /*
    Default tag engine configuration
     */
    @JvmField val tagEngine: TagEngine? = null,

    /*
    Default task engine configuration
     */
    @JvmField val taskEngine: TaskEngine? = null,

    /*
    Default metrics configuration
     */
    @JvmField val metrics: Metrics? = null,

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
        tagEngine?.let {
            // apply default, if not set
            it.stateStorage = it.stateStorage ?: stateStorage
            checkStateStorage(it.stateStorage, "tagEngine.stateStorage")
            it.stateCache = it.stateCache ?: stateCache
        }

        taskEngine?.let {
            // apply default, if not set
            it.stateStorage = it.stateStorage ?: stateStorage
            checkStateStorage(it.stateStorage, "taskEngine.stateStorage")
            it.stateCache = it.stateCache ?: stateCache
        }

        workflowEngine?.let {
            // apply default, if not set
            it.stateStorage = it.stateStorage ?: stateStorage
            checkStateStorage(it.stateStorage, "workflowEngine.stateStorage")
            it.stateCache = it.stateCache ?: stateCache
        }

        metrics?.let {
            // apply default, if not set
            it.stateStorage = it.stateStorage ?: stateStorage
            checkStateStorage(it.stateStorage, "metrics.stateStorage")
            it.stateCache = it.stateCache ?: stateCache
        }

        // apply default, if not set
        tasks.map {
            it.tagEngine = when (it.tagEngine) {
                null -> tagEngine
                else -> it.tagEngine!! merge tagEngine
            }
            it.taskEngine = when (it.taskEngine) {
                null -> taskEngine
                else -> it.taskEngine!! merge taskEngine
            }
        }

        workflows.map {
            it.tagEngine = when (it.tagEngine) {
                null -> tagEngine
                else -> it.tagEngine!! merge tagEngine
            }
            it.taskEngine = when (it.taskEngine) {
                null -> taskEngine
                else -> it.taskEngine!! merge taskEngine
            }
            it.workflowEngine = when (it.workflowEngine) {
                null -> workflowEngine
                else -> it.workflowEngine!! merge workflowEngine
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
