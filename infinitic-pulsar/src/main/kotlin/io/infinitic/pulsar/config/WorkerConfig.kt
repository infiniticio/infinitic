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
import io.infinitic.storage.kodein.Kodein
import io.infinitic.storage.redis.Redis

data class WorkerConfig(
    /*
    Worker name - used to identify multiple workers
     */
    @JvmField val name: String? = null,

    /*
    Default running mode
     */
    @JvmField var mode: Mode,

    /*
    Default state storage
     */
    @JvmField var stateStorage: StateStorage,

    /*
    Pulsar configuration
     */
    @JvmField val pulsar: Pulsar,

    /*
    Infinitic workflow engine configuration
     */
    @JvmField val workflowEngine: WorkflowEngine?,

    /*
    Infinitic task engine configuration
     */
    @JvmField val taskEngine: TaskEngine?,

    /*
    Infinitic monitoring configuration
     */
    @JvmField val monitoring: Monitoring?,

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
    Kodein configuration
     */
    @JvmField val kodein: Kodein? = null
) {
    init {
        workflowEngine?.let {
            // apply default mode and stateStorage, if not set
            it.mode = it.mode ?: mode
            it.stateStorage = it.stateStorage ?: stateStorage
            checkStateStorage(it.stateStorage, it.mode!!, "workflowEngine.stateStorage")
        }

        taskEngine?.let {
            // apply default mode and stateStorage, if not set
            it.mode = it.mode ?: mode
            it.stateStorage = it.stateStorage ?: stateStorage
            checkStateStorage(it.stateStorage, it.mode!!, "taskEngine.stateStorage")
        }

        monitoring?.let {
            // apply default mode and stateStorage, if not set
            it.mode = it.mode ?: mode
            it.stateStorage = it.stateStorage ?: stateStorage
            checkStateStorage(it.stateStorage, it.mode!!, "monitoring.stateStorage")
        }

        // apply default mode, if not set
        tasks.map { it.mode = it.mode ?: mode }
        workflows.map { it.mode = it.mode ?: mode }
    }

    private fun checkStateStorage(stateStorage: StateStorage?, mode: Mode, name: String) {
        if (stateStorage == StateStorage.redis) {
            require(redis != null) { "`${StateStorage.redis}` is used for $name but not configured" }
        }

        if (stateStorage == StateStorage.kodein) {
            require(kodein != null) { "`${StateStorage.kodein}` is used for $name but not configured" }
        }

        if (stateStorage == StateStorage.pulsarState) {
            require(mode == Mode.function) {
                "stateStorage `${StateStorage.pulsarState}` - set for $name - can only be used with mode `${Mode.function}`"
            }
        }
    }
}
