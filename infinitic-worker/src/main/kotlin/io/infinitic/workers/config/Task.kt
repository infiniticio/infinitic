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

package io.infinitic.workers.config

import io.infinitic.tasks.tag.config.TaskTag
import java.lang.reflect.Constructor
import io.infinitic.tasks.Task as TaskInstance

data class Task(
    val name: String,
    val `class`: String? = null,
    val concurrency: Int = 1,
    var tagEngine: TaskTag? = null,
    var retryPolicy: RetryPolicy? = null
) {
    private lateinit var _constructor: Constructor<out Any>

    val instance
        get() = _constructor.newInstance() as TaskInstance

    init {
        require(name.isNotEmpty()) { "name can not be empty" }

        when (`class`) {
            null -> {
                require(tagEngine != null) { "class and taskTag null for task $name" }
            }

            else -> {
                require(`class`.isNotEmpty()) { "class empty for task $name" }

                val klass = try {
                    Class.forName(`class`)
                } catch (e: ClassNotFoundException) {
                    throw IllegalArgumentException("class \"$`class`\" is unknown for task $name")
                } catch (e: Exception) {
                    throw IllegalArgumentException(
                        "Error when trying to get class of name \"$`class`\" for task $name",
                        e
                    )
                }

                _constructor = try {
                    klass.getDeclaredConstructor()
                } catch (e: NoSuchMethodException) {
                    throw IllegalArgumentException("class \"$`class`\" must have an empty constructor for task $name")
                } catch (e: Exception) {
                    throw IllegalArgumentException(
                        "Error when trying to get constructor of class \"$`class`\" for task $name",
                        e
                    )
                }

                val instance = try {
                    _constructor.newInstance()
                } catch (e: Exception) {
                    throw IllegalArgumentException(
                        "Error when trying to instantiate class \"$`class`\" for task $name",
                        e
                    )
                }

                require(instance is TaskInstance) {
                    "class \"$`class`\" must extend ${TaskInstance::class.java.name} to be used for task $name"
                }

                require(concurrency >= 0) {
                    "concurrency must be positive for task $name"
                }
            }
        }
    }
}
