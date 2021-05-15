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

import io.infinitic.exceptions.UserException

sealed class TaskUserException(
    msg: String,
    help: String
) : UserException("$msg.\n$help")

class ClassNotFoundException(
    name: String
) : TaskUserException(
    msg = "No implementation class provided for $name",
    help = "Use \"register\" method to provide an instance that will be used associated to $name"
)

class NoMethodFoundWithParameterTypesException(
    klass: String,
    method: String,
    parameterTypes: List<String>
) : TaskUserException(
    msg = "No method \"$method(${ parameterTypes.joinToString() })\" found in \"$klass\" class",
    help = "Make sure parameter types are consistent with your method definition"
)

class NoMethodFoundWithParameterCountException(
    klass: String,
    method: String,
    parameterCount: Int
) : TaskUserException(
    msg = "No method \"$method\" with $parameterCount parameters found in \"$klass\" class",
    help = ""
)

class TooManyMethodsFoundWithParameterTypesException(
    klass: String,
    method: String,
    parameterTypes: List<String>
) : TaskUserException(
    msg = "Unable to decide which method \"$method\" with ${parameterTypes.joinToString()} parameters to use in \"$klass\" class",
    help = ""
)

class TooManyMethodsFoundWithParameterCountException(
    klass: String,
    method: String,
    parameterCount: Int
) : TaskUserException(
    msg = "Unable to decide which method \"$method\" with $parameterCount parameters to use in \"$klass\" class",
    help = ""
)
