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

package io.infinitic.tests.workflows

import io.infinitic.tests.tasks.TaskA
import io.infinitic.workflows.SendChannel
import io.infinitic.workflows.Workflow

interface WorkflowC {
    val log: String
    val channelA: SendChannel<String>
    fun receive(str: String): String
    fun concat(str: String): String
    fun add(str: String): String
}

class WorkflowCImpl : Workflow(), WorkflowC {
    override val channelA = channel<String>()
    val taskA = newTask(TaskA::class.java)

    override var log = ""

    override fun receive(str: String): String {
        log += str

        val r = channelA.receive().await()

        log += r

        return log
    }

    override fun concat(str: String): String {
        Thread.sleep(50)

        log = taskA.concat(log, str)

        return log
    }

    override fun add(str: String): String {

        log += str

        return log
    }
}
