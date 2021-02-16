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

data class Task(
    @JvmField val name: String,
    @JvmField val `class`: String? = null,
    @JvmField var mode: Mode? = null,
    @JvmField val consumers: Int = 1,
    @JvmField val concurrency: Int = 1,
    @JvmField val shared: Boolean = true,
    @JvmField var taskEngine: TaskEngine? = null
) {
    private lateinit var _instance: Any

    val instance: Any
        get() {
            if (! shared) return Class.forName(`class`).newInstance()
            if (! this::_instance.isInitialized) _instance = Class.forName(`class`).newInstance()
            return _instance
        }

    val modeOrDefault: Mode
        get() = mode ?: Mode.worker

    init {
        require(name.isNotEmpty()) { "name can NOT be empty" }
        require(`class` != null || taskEngine != null) {
            "executor and engine not defined for $name, " +
                "you should have at least \"class\" or \"taskEngine\" defined"
        }
        `class`?.let {
            require(`class`.isNotEmpty()) { "class empty for task $name" }

            require(try { instance; true } catch (e: ClassNotFoundException) { false }) {
                "class \"$it\" is unknown (task $name)"
            }
            require(try { instance; true } catch (e: Exception) { false }) {
                "class \"$it\" can not be instantiated using newInstance(). " +
                    "This class must be public and have an empty constructor"
            }
            require(consumers >= 1) { "consumers MUST be strictly positive (task $name)" }
            require(concurrency >= 1) { "concurrency MUST strictly be positive (task $name)" }
        }
    }
}
