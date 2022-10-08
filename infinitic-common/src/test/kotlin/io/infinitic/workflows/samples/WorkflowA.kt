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

package io.infinitic.workflows.samples

import io.infinitic.workflows.Deferred
import io.infinitic.workflows.SendChannel
import io.infinitic.workflows.Workflow
import kotlinx.serialization.Serializable

sealed class Obj

@Serializable
@Suppress("unused")
data class Obj1(val foo: String, val bar: Int) : Obj()

@Serializable
@Suppress("unused")
data class Obj2(val foo: String, val bar: Int) : Obj()

interface WorkflowA {
    val channelObj: SendChannel<Obj>
    val channelString: SendChannel<String>

    fun empty(): String
    fun syncTask(duration: Long): Long
    fun asyncTask(duration: Long): Deferred<Long>
    fun syncWorkflow(duration: Long): Long
    fun asyncWorkflow(duration: Long): Deferred<Long>
    fun dispatchSelf(): Deferred<String>
}

class WorkflowAImpl : Workflow(), WorkflowA {
    val self: WorkflowA = getWorkflowById(WorkflowA::class.java, "id")
    override val channelObj = channel<Obj>()
    override val channelString = channel<String>()
    private val taskA =
        newService(TaskA::class.java, tags = setOf("foo", "bar"), meta = mapOf("foo" to "bar".toByteArray()))
    private val workflowA =
        newWorkflow(WorkflowA::class.java, tags = setOf("foo", "bar"), meta = mapOf("foo" to "bar".toByteArray()))

    override fun empty() = "void"
    override fun syncTask(duration: Long) = taskA.await(duration)
    override fun asyncTask(duration: Long) = dispatch(taskA::await, duration)
    override fun syncWorkflow(duration: Long) = workflowA.syncTask(duration)
    override fun asyncWorkflow(duration: Long) = dispatch(workflowA::syncTask, duration)

    override fun dispatchSelf(): Deferred<String> = dispatch(self::empty)

    fun seqBis() {
        taskA.await(100)
    }
}
