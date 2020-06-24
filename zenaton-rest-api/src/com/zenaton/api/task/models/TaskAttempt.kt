package com.zenaton.api.task.models

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
