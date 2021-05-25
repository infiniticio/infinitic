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

package io.infinitic.api.task.models

import java.time.Instant

data class Task(
    val id: String,
    val name: String,
    val status: String,
    val dispatchedAt: Instant,
    val startedAt: Instant? = null,
    val completedAt: Instant? = null,
    val failedAt: Instant? = null,
    val attempts: List<TaskAttempt> = listOf()
) {

    class Builder {
        var id: String? = null
        var name: String? = null
        var dispatchedAt: Instant? = null
        var startedAt: Instant? = null
        var completedAt: Instant? = null
        var failedAt: Instant? = null
        var attempts: MutableList<TaskAttempt.Builder> = mutableListOf()

        fun build(): Task? {
            val attempts = attempts.build()
            val status = resolveStatus(attempts)

            return Task(
                id = id ?: return null,
                name = name ?: return null,
                status = status,
                dispatchedAt = dispatchedAt ?: return null,
                startedAt = startedAt,
                completedAt = completedAt,
                failedAt = failedAt,
                attempts = attempts
            )
        }

        private fun resolveStatus(attempts: List<TaskAttempt>): String {
            // If any of the task attempt tries is completed, the task is ok
            if (attempts.flatMap { it.tries }.any { completedAt != null }) {
                return "ok"
            }

            // If the task has an empty list of attempts, it was just dispatched so the task is ok
            if (attempts.isEmpty()) {
                return "ok"
            }

            // If the task has only one try for every attempt and they are not failed, the task is ok
            if (attempts.mapNotNull { it.tries.maxByOrNull { item -> item.retry } }.all { it.retry == 0 } && attempts.flatMap { it.tries }.all { it.failedAt == null }) {
                return "ok"
            }

            // If all the task attempts tries are failed and don't have a retry planed, the task is in error
            if (attempts.flatMap { it.tries }.all { failedAt != null } && attempts.mapNotNull { it.tries.last().delayBeforeRetry }.all { it < 0.0 }) {
                return "error"
            }

            return "warning"
        }
    }
}
