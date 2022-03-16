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

package io.infinitic.tasks.executor.samples

import io.infinitic.exceptions.tasks.MaxRunDurationException
import io.infinitic.tasks.Task
import java.time.Duration

interface SampleTask {
    fun handle(i: Int, j: String): String
    fun handle(i: Int, j: Int): Int
    fun other(i: Int, j: String): String
}

class SampleTaskImpl() : Task(), SampleTask {
    override fun handle(i: Int, j: String) = (i * j.toInt()).toString()
    override fun handle(i: Int, j: Int) = (i * j)
    override fun other(i: Int, j: String) = (i * j.toInt()).toString()
}

internal class SampleTaskWithContext : Task() {
    fun handle(i: Int, j: String) = (i * j.toInt() * context.retrySequence).toString()
}

internal class SampleTaskWithRetry : Task() {
    fun handle(i: Int, j: String): String = if (i < 0) (i * j.toInt()).toString() else throw IllegalStateException()

    override fun getDurationBeforeRetry(e: Exception): Duration? =
        if (e is IllegalStateException) Duration.ofSeconds(3L) else null
}

internal class SampleTaskWithBadTypeRetry : Task() {
    fun handle(i: Int, j: String): String = if (i < 0) (i * j.toInt()).toString() else throw IllegalStateException()

    override fun getDurationBeforeRetry(e: Exception): Duration? =
        Duration.ofSeconds(3L)
}

internal class SampleTaskWithBuggyRetry : Task() {
    fun handle(i: Int, j: String): String = if (i < 0) (i * j.toInt()).toString() else throw IllegalStateException()

    override fun getDurationBeforeRetry(e: Exception): Duration? =
        if (e is IllegalStateException) throw IllegalArgumentException() else Duration.ofSeconds(3L)
}

internal class SampleTaskWithTimeout() : Task() {
    fun handle(i: Int, j: String): String {
        Thread.sleep(400)

        return (i * j.toInt() * context.retrySequence).toString()
    }

    override fun getDurationBeforeRetry(e: Exception): Duration? =
        if (e is MaxRunDurationException) null else Duration.ofSeconds(3L)
}
