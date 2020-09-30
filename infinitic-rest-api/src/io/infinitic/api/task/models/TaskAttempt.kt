// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.api.task.models

data class TaskAttempt(
    val id: String,
    val index: Int,
    val tries: List<TaskAttemptTry> = listOf()
) {

    class Builder {
        var id: String? = null
        var index: Int? = null
        var tries = mutableListOf<TaskAttemptTry.Builder>()

        fun build(): TaskAttempt {
            val tries = tries.build()

            return TaskAttempt(
                id = id ?: throw Exceptions.IncompleteStateException("Id is mandatory to build a TaskAttempt object."),
                index = index ?: throw Exceptions.IncompleteStateException("Index is mandatory to build a TaskAttempt object."),
                tries = tries
            )
        }

        object Exceptions {
            class IncompleteStateException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
        }
    }
}

/**
 * Transforms a list of `TaskAttempt.Builder` to a list of `TaskAttempt`.
 */
fun List<TaskAttempt.Builder>.build(): List<TaskAttempt> = map { it.build() }
//
// /**
// * Returns the corresponding item identified by [id] or create a new one if it does not exist yet.
// */
// fun MutableList<TaskAttempt.Builder>.getOrNew(id: String): TaskAttempt.Builder =
//    firstOrNull { it.id == id } ?: TaskAttempt.Builder().also {
//        it.id = id
//        this.add(it)
//    }
//
// /**
// * Returns the corresponding item identified by [id] or create a new one if it does not exist yet.
// */
// fun List<TaskAttempt.Builder>.get(id: String): TaskAttempt.Builder =
//    firstOrNull { it.id == id } ?: TaskAttempt.Builder().also {
//        it.id = id
//        this.add(it)
//    }

fun List<TaskAttempt.Builder>.get(id: String): TaskAttempt.Builder? = firstOrNull { it.id == id }
