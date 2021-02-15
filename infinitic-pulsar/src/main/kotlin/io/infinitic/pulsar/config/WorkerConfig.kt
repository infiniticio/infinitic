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

package io.infinitic.pulsar.config

import io.infinitic.cache.StateCache
import io.infinitic.cache.caffeine.Caffeine
import io.infinitic.pulsar.config.data.Mode
import io.infinitic.pulsar.config.data.Monitoring
import io.infinitic.pulsar.config.data.Pulsar
import io.infinitic.pulsar.config.data.Task
import io.infinitic.pulsar.config.data.TaskEngine
import io.infinitic.pulsar.config.data.Workflow
import io.infinitic.pulsar.config.data.WorkflowEngine
import io.infinitic.pulsar.config.merge.merge
import io.infinitic.storage.StateStorage
import io.infinitic.storage.redis.Redis

data class WorkerConfig(
    /*
    Worker name - used to identify multiple workers
     */
    @JvmField val name: String? = null,

    /*
    Default running mode
     */
    @JvmField var mode: Mode = Mode.worker,

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
    Infinitic workflow engine configuration
     */
    @JvmField val workflowEngine: WorkflowEngine? = null,

    /*
    Infinitic task engine configuration
     */
    @JvmField val taskEngine: TaskEngine? = null,

    /*
    Infinitic monitoring configuration
     */
    @JvmField val monitoring: Monitoring? = null,

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
        workflowEngine?.let {
            // apply default, if not set
            it.mode = it.mode ?: mode
            it.stateStorage = it.stateStorage ?: stateStorage
            checkStateStorage(it.stateStorage, "workflowEngine.stateStorage")
            it.stateCache = it.stateCache ?: stateCache

            require(workflows.isNotEmpty()) { "You have defined workflowEngine, but no workflow" }
        }

        taskEngine?.let {
            // apply default, if not set
            it.mode = it.mode ?: mode
            it.stateStorage = it.stateStorage ?: stateStorage
            checkStateStorage(it.stateStorage, "taskEngine.stateStorage")
            it.stateCache = it.stateCache ?: stateCache

            require(tasks.isNotEmpty()) { "You have defined taskEngine, but no task" }
        }

        monitoring?.let {
            // apply default, if not set
            it.mode = it.mode ?: mode
            it.stateStorage = it.stateStorage ?: stateStorage
            checkStateStorage(it.stateStorage, "monitoring.stateStorage")
            it.stateCache = it.stateCache ?: stateCache
        }

        // apply default, if not set
        tasks.map {
            it.mode = it.mode ?: mode
            it.taskEngine = when (it.taskEngine) {
                null -> taskEngine
                else -> it.taskEngine!! merge taskEngine
            }
        }

        workflows.map {
            it.mode = it.mode ?: mode
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
