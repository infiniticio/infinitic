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

package io.infinitic.pulsar.config.data

import io.infinitic.workflows.Workflow

data class Workflow(
    @JvmField val name: String,
    @JvmField val `class`: String? = null,
    @JvmField var mode: Mode? = null,
    @JvmField val consumers: Int = 1,
    @JvmField val concurrency: Int = 1,
    @JvmField var taskEngine: TaskEngine? = null,
    @JvmField var workflowEngine: WorkflowEngine? = null
) {
    val instance: Any
        get() = Class.forName(`class`).newInstance()

    val modeOrDefault: Mode
        get() = mode ?: Mode.worker

    init {
        require(name.isNotEmpty()) { "name can NOT be empty" }
        require(`class` != null || workflowEngine != null) {
            "executor and engine not defined for $name, " +
                "you should have at least \"class\" or \"workflowEngine\" defined"
        }
        `class`?.let {
            require(`class`.isNotEmpty()) { "class empty for workflow $name" }

            require(try { instance; true } catch (e: ClassNotFoundException) { false }) {
                "class $`class` is unknown (workflow $name)"
            }
            require(try { instance; true } catch (e: Exception) { false }) {
                "class \"$it\" can not be instantiated using newInstance(). " +
                    "Checks it's public and has an empty constructor (workflow $name)"
            }
            require(instance is Workflow) {
                "class \"$it\" is not a workflow as it does not extend ${Workflow::class.java.name}"
            }
            require(consumers >= 1) { "consumers MUST be strictly positive (workflow $name)" }
            require(concurrency >= 1) { "concurrency MUST be strictly positive (workflow $name)" }
        }
    }
}
