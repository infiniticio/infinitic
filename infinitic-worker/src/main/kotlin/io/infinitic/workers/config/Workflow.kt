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

import io.infinitic.common.workers.config.RetryPolicy
import io.infinitic.common.workers.config.WorkflowCheckMode
import io.infinitic.workers.register.WorkerRegister
import io.infinitic.workflows.engine.config.WorkflowEngine
import io.infinitic.workflows.tag.config.WorkflowTag
import java.lang.reflect.Constructor
import io.infinitic.workflows.Workflow as WorkflowInstance

data class Workflow(
    val name: String,
    val `class`: String? = null,
    var concurrency: Int? = null,
    var timeoutInSeconds: Double? = null,
    var retry: RetryPolicy? = null,
    var checkMode: WorkflowCheckMode? = null,
    var tagEngine: WorkflowTag? = WorkerRegister.DEFAULT_WORKFLOW_TAG,
    var workflowEngine: WorkflowEngine? = WorkerRegister.DEFAULT_WORKFLOW_ENGINE
) {
    private lateinit var _constructor: Constructor<out Any>

    fun getInstance() = _constructor.newInstance() as WorkflowInstance

    init {
        require(name.isNotEmpty()) { "name can not be empty" }

        when (`class`) {
            null -> {
                require(tagEngine != null || workflowEngine != null) { "class, workflowTag and workflowEngine are null for workflow $name" }
            }

            else -> {
                require(`class`.isNotEmpty()) { "class empty for workflow $name" }

                val klass = try {
                    Class.forName(`class`)
                } catch (e: ClassNotFoundException) {
                    throw IllegalArgumentException("class \"$`class`\" unknown for workflow $name")
                } catch (e: Exception) {
                    throw IllegalArgumentException(
                        "Error when trying to get class of name \"$`class`\" for workflow $name",
                        e
                    )
                }

                _constructor = try {
                    klass.getDeclaredConstructor()
                } catch (e: NoSuchMethodException) {
                    throw IllegalArgumentException("class \"$`class`\" must have an empty constructor for workflow $name")
                } catch (e: Exception) {
                    throw IllegalArgumentException(
                        "Error when trying to get constructor of class \"$`class`\" for workflow $name",
                        e
                    )
                }

                val instance = try {
                    _constructor.newInstance()
                } catch (e: Exception) {
                    throw IllegalArgumentException(
                        "Error when trying to instantiate class \"$`class`\" for workflow $name",
                        e
                    )
                }

                require(instance is WorkflowInstance) {
                    "class \"$`class`\" must extend ${WorkflowInstance::class.java.name} to be used as a workflow"
                }

                if (concurrency != null) {
                    require(concurrency!! >= 0) { "concurrency must be positive (workflow $name)" }
                }

                if (timeoutInSeconds != null) {
                    require(timeoutInSeconds!! > 0) { "timeoutSeconds must be positive (workflow $name)" }
                }
            }
        }
    }
}
