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

package io.infinitic.config.data

import io.infinitic.tasks.Task as TaskInstance

data class Task(
    @JvmField val name: String,
    @JvmField val `class`: String? = null,
    @JvmField val concurrency: Int = 1,
    @JvmField var tagEngine: TagEngine? = TagEngine().apply { default = true },
    @JvmField var taskEngine: TaskEngine? = TaskEngine().apply { default = true },
    @JvmField var metrics: Metrics? = Metrics()
) {
    val instance: TaskInstance
        get() = Class.forName(`class`).getDeclaredConstructor().newInstance() as TaskInstance

    init {
        require(name.isNotEmpty()) { "name can not be empty" }

        `class`?.let {
            require(`class`.isNotEmpty()) { "class empty for task $name" }

            require(try { instance; true } catch (e: ClassNotFoundException) { false }) {
                "class \"$it\" is unknown (task $name)"
            }
            require(try { instance; true } catch (e: ClassCastException) { false }) {
                "class \"$it\" is not a task as it does not extend ${TaskInstance::class.java.name}"
            }
            require(try { instance; true } catch (e: Exception) { false }) {
                "class \"$it\" can not be instantiated using .getDeclaredConstructor().newInstance(). " +
                    "This class must be public and have an empty constructor"
            }

            require(concurrency >= 0) { "concurrency must be positive (task $name)" }
        }
    }
}
