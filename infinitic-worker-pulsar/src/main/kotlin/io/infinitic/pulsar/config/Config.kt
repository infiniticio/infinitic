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

import io.infinitic.worker.config.Redis

data class Config(
    /*
    Worker name - used to identify
     */
    val name: String,

    /*
    Default running mode
     */
    var mode: Mode,
    /*
    Pulsar configuration
     */
    val pulsar: Pulsar,

    /*
    Redis configuration
     */
    val redis: Redis? = null,

    /*
    Infinitic workflow engine configuration
     */
    val workflowEngine: WorkflowEngine = WorkflowEngine(Mode.function, 5, StateStorage.pulsarState),

    /*
    Infinitic task engine configuration
     */
    val taskEngine: TaskEngine = TaskEngine(Mode.function, 10, StateStorage.pulsarState),

    /*
    Infinitic monitoring configuration
     */
    val monitoring: Monitoring = Monitoring(Mode.function, 4, StateStorage.pulsarState),

    /*
    Tasks configuration
     */
    val tasks: List<Task> = listOf(),

    /*
    Workflows configuration
     */
    val workflows: List<Workflow> = listOf()
) {
    init {
        // apply default mode, if mode not set
        workflowEngine.mode = workflowEngine.mode ?: mode
        taskEngine.mode = taskEngine.mode ?: mode
        monitoring.mode = monitoring.mode ?: mode
        tasks.map { it.mode = it.mode ?: mode }
        workflows.map { it.mode = it.mode ?: mode }

        // consistency check
        checkStateStorage(workflowEngine.stateStorage, workflowEngine.mode!!, "workflowEngine.stateStorage")
        checkStateStorage(taskEngine.stateStorage, taskEngine.mode!!, "taskEngine.stateStorage")
        checkStateStorage(monitoring.stateStorage, monitoring.mode!!, "monitoring.stateStorage")
    }

    private fun checkStateStorage(stateStorage: StateStorage?, mode: Mode, name: String) {
        if (stateStorage == StateStorage.redis) {
            require(redis != null) { "`${StateStorage.redis}` is used for $name but not configured" }
        }

        if (stateStorage == StateStorage.pulsarState) {
            require(mode == Mode.function) {
                "stateStorage `${StateStorage.pulsarState}` - set for $name - can only be used with mode `${Mode.function}`"
            }
        }
    }
}
