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

package io.infinitic.exceptions.tasks

import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.exceptions.UserException
import kotlinx.serialization.Serializable

@Serializable
sealed class TaskException(
    val msg: String,
    val help: String
) : UserException() {
    override val message = "$msg.\n$help"
}

@Serializable
data class ClassNotFoundDuringInstantiation(
    val name: String
) : TaskException(
    msg = "Impossible to find a Class associated to $name",
    help = "Use \"register\" method to provide an instance that will be used associated to $name"
)

@Serializable
data class NoMethodFoundWithParameterTypes(
    val klass: String,
    val method: String,
    val parameterTypes: List<String>
) : TaskException(
    msg = "No method \"$method(${ parameterTypes.joinToString() })\" found in \"$klass\" class",
    help = "Make sure parameter types are consistent with your method definition"
)

@Serializable
data class NoMethodFoundWithParameterCount(
    val klass: String,
    val method: String,
    val parameterCount: Int
) : TaskException(
    msg = "No method \"$method\" with $parameterCount parameters found in \"$klass\" class",
    help = ""
)

@Serializable
data class TooManyMethodsFoundWithParameterCount(
    val klass: String,
    val method: String,
    val parameterCount: Int
) : TaskException(
    msg = "Unable to decide which method \"$method\" with $parameterCount parameters to use in \"$klass\" class",
    help = ""
)

@Serializable
data class ProcessingTimeout(
    val klass: String,
    val delay: Float
) : TaskException(
    msg = "The processing of task \"$klass\" took more than $delay seconds",
    help = "You can increase (or remove entirely) this constraint in the options ${TaskOptions::javaClass.name}"
)
