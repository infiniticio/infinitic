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

import io.infinitic.common.utils.getClass
import io.infinitic.common.utils.getEmptyConstructor
import io.infinitic.common.utils.getInstance
import io.infinitic.tasks.tag.config.TaskTag
import java.lang.reflect.Constructor

data class Service(
    val name: String,
    val `class`: String? = null,
    val concurrency: Int = 1,
    var tagEngine: TaskTag? = null
    // var retryPolicy: RetryPolicy? = null
) {
    private lateinit var _constructor: Constructor<out Any>

    val instance: Any
        get() = _constructor.getInstance()

    init {
        require(name.isNotEmpty()) { "name can not be empty" }

        when (`class`) {
            null -> {
                require(tagEngine != null) { "class and taskTag null for task $name" }
            }

            else -> {
                require(`class`.isNotEmpty()) { "class empty for task $name" }

                _constructor = `class`.getClass(
                    classNotFound = "class \"$`class`\" is unknown (service $name)",
                    errorClass = "Error when trying to get class of name \"$`class`\" (service $name)"
                ).getEmptyConstructor(
                    noEmptyConstructor = "class \"$`class`\" must have an empty constructor (service $name)",
                    constructorError = "Can not access constructor of class \"$`class`\" (service $name)"
                )

                _constructor.getInstance(
                    instanceError = "Error during instantiation of class \"$`class`\" (service $name)"
                )

                require(concurrency >= 0) {
                    "concurrency must be positive for task $name"
                }
            }
        }
    }
}
