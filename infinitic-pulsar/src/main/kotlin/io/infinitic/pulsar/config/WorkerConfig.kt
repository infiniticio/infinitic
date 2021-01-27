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

) {
    init {
        workflowEngine?.let {
            // apply default mode and stateStorage, if not set
            it.mode = it.mode ?: mode
            it.stateStorage = it.stateStorage ?: stateStorage
            checkStateStorage(it.stateStorage, "workflowEngine.stateStorage")
        }

        taskEngine?.let {
            // apply default mode and stateStorage, if not set
            it.mode = it.mode ?: mode
            it.stateStorage = it.stateStorage ?: stateStorage
            checkStateStorage(it.stateStorage, "taskEngine.stateStorage")
        }

        monitoring?.let {
            // apply default mode and stateStorage, if not set
            it.mode = it.mode ?: mode
            it.stateStorage = it.stateStorage ?: stateStorage
            checkStateStorage(it.stateStorage, "monitoring.stateStorage")
        }

        // apply default mode, if not set
        tasks.map { it.mode = it.mode ?: mode }
        workflows.map { it.mode = it.mode ?: mode }
    }

    private fun checkStateStorage(stateStorage: StateStorage?, name: String) {
        require(stateStorage != null) { "`stateStorage` can not be null for $name" }

        if (stateStorage == StateStorage.redis) {
            require(redis != null) { "`${StateStorage.redis}` is used for $name but not configured" }
        }
    }
}
