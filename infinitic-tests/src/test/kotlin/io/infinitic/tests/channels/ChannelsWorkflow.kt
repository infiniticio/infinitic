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

package io.infinitic.tests.channels

import com.jayway.jsonpath.Criteria.where
import io.infinitic.annotations.Ignore
import io.infinitic.tests.utils.Obj
import io.infinitic.tests.utils.Obj1
import io.infinitic.tests.utils.Obj2
import io.infinitic.tests.utils.UtilTask
import io.infinitic.workflows.Deferred
import io.infinitic.workflows.SendChannel
import io.infinitic.workflows.Workflow
import io.infinitic.workflows.or
import java.time.Duration

interface ChannelsWorkflow {
    val channelObj: SendChannel<Obj>
    val channelA: SendChannel<String>
    val channelB: SendChannel<String>

    fun channel1(): String
    fun channel2(): Any
    fun channel3(): Any
    fun channel4(): Obj
    fun channel4bis(): Obj
    fun channel4ter(): Obj
    fun channel5(): Obj1
    fun channel5bis(): Obj1
    fun channel5ter(): Obj1
    fun channel6(): String
    fun channel6bis(): String
    fun channel6ter(): String
    fun channel7(): String
}

@Suppress("unused")
class ChannelsWorkflowImpl : Workflow(), ChannelsWorkflow {

    @Ignore private val self by lazy { getWorkflowById(ChannelsWorkflow::class.java, context.id) }

    lateinit var deferred: Deferred<String>

    override val channelObj = channel<Obj>()
    override val channelA = channel<String>()
    override val channelB = channel<String>()

    private val utilTask = newTask(UtilTask::class.java, tags = setOf("foo", "bar"), meta = mapOf("foo" to "bar".toByteArray()))
    private val workflowA = newWorkflow(ChannelsWorkflow::class.java)

    private var p1 = ""

    override fun channel1(): String {
        val deferred: Deferred<String> = channelA.receive()

        return deferred.await()
    }

    override fun channel2(): Any {
        val deferredChannel = channelA.receive()

        val deferredTimer = timer(Duration.ofMillis(1000))

        return (deferredChannel or deferredTimer).await()
    }

    override fun channel3(): Any {
        timer(Duration.ofMillis(1000)).await()
        val deferredChannel = channelA.receive()
        val deferredTimer = timer(Duration.ofMillis(100))

        return (deferredChannel or deferredTimer).await()
    }

    override fun channel4(): Obj {
        val deferred: Deferred<Obj> = channelObj.receive()

        return deferred.await()
    }

    override fun channel4bis(): Obj {
        val deferred: Deferred<Obj> = channelObj.receive(jsonPath = "[?(\$.foo == \"foo\")]")

        return deferred.await()
    }

    override fun channel4ter(): Obj {
        val deferred: Deferred<Obj> = channelObj.receive(jsonPath = "[?]", criteria = where("foo").eq("foo"))

        return deferred.await()
    }

    override fun channel5(): Obj1 {
        val deferred: Deferred<Obj1> = channelObj.receive(Obj1::class.java)

        return deferred.await()
    }

    override fun channel5bis(): Obj1 {
        val deferred: Deferred<Obj1> = channelObj.receive(Obj1::class.java, jsonPath = "[?(\$.foo == \"foo\")]")

        return deferred.await()
    }

    override fun channel5ter(): Obj1 {
        val deferred: Deferred<Obj1> = channelObj.receive(Obj1::class.java, jsonPath = "[?]", criteria = where("foo").eq("foo"))

        return deferred.await()
    }

    override fun channel6(): String {
        val deferred1: Deferred<Obj1> = channelObj.receive(Obj1::class.java)
        val deferred2: Deferred<Obj2> = channelObj.receive(Obj2::class.java)
        val obj1 = deferred1.await()
        val obj2 = deferred2.await()

        return obj1.foo + obj2.foo + obj1.bar * obj2.bar
    }

    override fun channel6bis(): String {
        val deferred1: Deferred<Obj1> = channelObj.receive(Obj1::class.java, jsonPath = "[?(\$.foo == \"foo\")]")
        val deferred2: Deferred<Obj2> = channelObj.receive(Obj2::class.java, jsonPath = "[?(\$.foo == \"foo\")]")
        val obj1 = deferred1.await()
        val obj2 = deferred2.await()

        return obj1.foo + obj2.foo + obj1.bar * obj2.bar
    }

    override fun channel6ter(): String {
        val deferred1: Deferred<Obj1> = channelObj.receive(Obj1::class.java, jsonPath = "[?]", criteria = where("foo").eq("foo"))
        val deferred2: Deferred<Obj2> = channelObj.receive(Obj2::class.java, jsonPath = "[?]", criteria = where("foo").eq("foo"))
        val obj1 = deferred1.await()
        val obj2 = deferred2.await()

        return obj1.foo + obj2.foo + obj1.bar * obj2.bar
    }

    override fun channel7(): String {
        val deferred1 = channelA.receive()
        utilTask.await(100)
        val s1 = deferred1.await()
        val deferred2 = channelA.receive()
        utilTask.await(100)
        val s2 = deferred2.await()
        val deferred3 = channelA.receive()
        utilTask.await(100)
        val s3 = deferred3.await()

        return "$s1$s2$s3"
    }
}
