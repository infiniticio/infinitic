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

import com.jayway.jsonpath.Criteria.where
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.exceptions.workflows.FailedDeferredException
import io.infinitic.tests.tasks.ParentInterface
import io.infinitic.tests.tasks.TaskA
import io.infinitic.workflows.Deferred
import io.infinitic.workflows.DeferredStatus
import io.infinitic.workflows.SendChannel
import io.infinitic.workflows.Workflow
import io.infinitic.workflows.and
import io.infinitic.workflows.or
import kotlinx.serialization.Serializable
import java.time.Duration
import java.util.UUID

sealed class Obj
@Serializable
data class Obj1(val foo: String, val bar: Int) : Obj()
@Serializable
data class Obj2(val foo: String, val bar: Int) : Obj()

interface WorkflowA : ParentInterface {
    val channelObj: SendChannel<Obj>
    val channelA: SendChannel<String>
    val channelB: SendChannel<String>

    fun empty(): String
    fun await(duration: Long): Long
    fun wparent(): String
    fun context1(): UUID
    fun context2(): Set<String>
    fun context3(): WorkflowMeta
    fun context4(): UUID?
    fun context5(): String?
    fun context6(): Set<String>
    fun context7(): TaskMeta
    fun seq1(): String
    fun seq2(): String
    fun seq3(): String
    fun seq4(): String
    fun deferred1(): String
    fun or1(): String
    fun or2(): Any
    fun or3(): String
    fun or4(): String
    fun and1(): List<String>
    fun and2(): List<String>
    fun and3(): List<String>
    fun inline1(i: Int): String
    fun inline2(i: Int): String
    fun inline3(i: Int): String
    fun child1(): String
    fun child2(): String
    fun prop1(): String
    fun prop2(): String
    fun prop3(): String
    fun prop4(): String
    fun prop5(): String
    fun prop6(): String
    fun prop7(): String
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
    fun failing1(): String
    fun failing2()
    fun failing2a(): Long
    fun failing3(): Long
    fun failing3b(): Long
    fun failing4(): Long
    fun failing5(): Long
    fun failing6()
    fun failing7(): Long
    fun failing8(): String
    fun failing9(): Boolean
    fun failing10(): String
    fun cancel1()
}

class WorkflowAImpl : Workflow(), WorkflowA {
    override val channelObj = channel<Obj>()
    override val channelA = channel<String>()
    override val channelB = channel<String>()

    private val taskA = newTask<TaskA>(tags = setOf("foo", "bar"), meta = mapOf("foo" to "bar".toByteArray()))
    private var p1 = ""

    override fun empty() = "void"

    override fun await(duration: Long) = taskA.await(duration)

    override fun parent() = taskA.parent()

    override fun wparent(): String {
        val workflowA = newWorkflow<WorkflowA>()
        return workflowA.parent()
    }

    override fun context1(): UUID = context.id

    override fun context2(): Set<String> = context.tags

    override fun context3() = WorkflowMeta(context.meta)

    override fun context4() = taskA.workflowId()

    override fun context5() = taskA.workflowName()

    override fun context6() = taskA.tags()

    override fun context7() = taskA.meta()

    override fun seq1(): String {
        var str = ""

        str = taskA.concat(str, "1")
        str = taskA.concat(str, "2")

        str = taskA.concat(str, "3")

        return str // should be "123"
    }

    override fun seq2(): String {
        var str = ""

        val d = async(taskA) { reverse("ab") }
        str = taskA.concat(str, "2")
        str = taskA.concat(str, "3")

        return str + d.await() // should be "23ba"
    }

    override fun seq3(): String {
        var str = ""

        val d = async { taskA.reverse("ab") }
        str = taskA.concat(str, "2")
        str = taskA.concat(str, "3")

        return str + d.await() // should be "23ba"
    }

    override fun seq4(): String {
        var str = ""

        val d = async {
            val s = taskA.reverse("ab")
            taskA.concat(s, "c")
        }
        str = taskA.concat(str, "2")
        str = taskA.concat(str, "3")

        return str + d.await() // should be "23bac"
    }

    override fun deferred1(): String {
        var str = ""

        val d = async {
            taskA.reverse("X")
        }
        str += d.isOngoing().toString()
        str += d.isCompleted().toString()
        d.await()
        str += d.isOngoing().toString()
        str += d.isCompleted().toString()

        return str // should be "truefalsefalsetrue"
    }

//    override fun deferred1(): String {
//        var str = ""
//
//        val d = async {
//            println("str = $str")
//            str += taskA.reverse("X")
//        }
//        str += d.isOngoing().toString()
//        str += d.isCompleted().toString()
//        str += d.isTerminated().toString()
//        d.await()
//        str += d.isOngoing().toString()
//        str += d.isCompleted().toString()
//        str += d.isTerminated().toString()
//
//        return str  // should be "truefalsefalseXfalsetruetrue"
//    }

    override fun or1(): String {
        val d1 = async(taskA) { reverse("ab") }
        val d2 = async(taskA) { reverse("cd") }
        val d3 = async(taskA) { reverse("ef") }

        return (d1 or d2 or d3).await() // should be "ba" or "dc" or "fe"
    }

    override fun or2(): Any {
        val d1 = async(taskA) { reverse("ab") }
        val d2 = async(taskA) { reverse("cd") }
        val d3 = async(taskA) { reverse("ef") }

        return ((d1 and d2) or d3).await() // should be listOf("ba","dc") or "fe"
    }

    override fun or3(): String {
        val list: MutableList<Deferred<String>> = mutableListOf()
        list.add(async(taskA) { reverse("ab") })
        list.add(async(taskA) { reverse("cd") })
        list.add(async(taskA) { reverse("ef") })

        return list.or().await() // should be "ba" or "dc" or "fe"
    }

    override fun or4(): String {
        var s3 = taskA.concat("1", "2")

        val d1 = async(taskA) { reverse("ab") }
        val d2 = timer(Duration.ofMillis(50))
        val d = (d1 or d2)
        if ((d1 or d2).status() != DeferredStatus.COMPLETED) {
            s3 = taskA.reverse("ab")
        }
        d.await()

        return d1.await() + s3 // should be "baba"
    }

    override fun and1(): List<String> {
        val d1 = async(taskA) { reverse("ab") }
        val d2 = async(taskA) { reverse("cd") }
        val d3 = async(taskA) { reverse("ef") }

        return (d1 and d2 and d3).await() // should be listOf("ba","dc","fe")
    }

    override fun and2(): List<String> {

        val list: MutableList<Deferred<String>> = mutableListOf()
        list.add(async(taskA) { reverse("ab") })
        list.add(async(taskA) { reverse("cd") })
        list.add(async(taskA) { reverse("ef") })

        return list.and().await() // should be listOf("ba","dc","fe")
    }

    override fun and3(): List<String> {

        val list: MutableList<Deferred<String>> = mutableListOf()
        for (i in 1..1_00) {
            list.add(async(taskA) { reverse("ab") })
        }
        return list.and().await() // should be listOf("ba","dc","fe")
    }

    override fun inline1(i: Int): String {
        val result = inline { 2 * i }
        return taskA.concat("2 * $i = ", "$result")
    }

    override fun inline2(i: Int): String {
        val result = inline {
            async(taskA) { reverse("ab") }
            2 * i
        }

        return taskA.concat("2 * $i = ", "$result")
    }

    override fun inline3(i: Int): String {
        val result = inline {
            taskA.concat("1", "2")
            2 * i
        }
        return taskA.concat("2 * $i = ", "$result") // should throw
    }

    override fun child1(): String {
        val workflowB = newWorkflow<WorkflowB>()
        var str: String = workflowB.concat("-")
        str = taskA.concat(str, "-")

        return str // should be "-abc-"
    }

    override fun child2(): String {
        val workflowB = newWorkflow<WorkflowB>()
        val str = taskA.reverse("12")
        val d = async(workflowB) { concat(str) }

        return taskA.concat(d.await(), str) // should be "21abc21"
    }

    override fun prop1(): String {
        p1 = "a"

        async {
            p1 += "b"
        }
        p1 += "c"

        return p1 // should be "ac"
    }

    override fun prop2(): String {
        p1 = "a"

        async {
            p1 += "b"
        }
        p1 += "c"
        taskA.await(100)
        p1 += "d"

        return p1 // should be "acbd"
    }

    override fun prop3(): String {
        p1 = "a"

        async {
            taskA.await(50)
            p1 += "b"
        }
        p1 += "c"
        taskA.await(200)
        p1 += "d"

        return p1 // should be "acbd"
    }

    override fun prop4(): String {
        p1 = "a"

        async {
            taskA.await(200)
            p1 += "b"
        }
        p1 += "c"
        taskA.await(100)
        p1 += "d"

        return p1 // should be "acd"
    }

    override fun prop5(): String {
        p1 = "a"

        async {
            p1 += "b"
        }

        async {
            p1 += "c"
        }
        p1 += "d"

        taskA.await(100)

        return p1 // should be "adbc"
    }

    override fun prop6(): String {
        val d1 = async(taskA) { reverse("12") }

        val d2 = async {
            d1.await()
            p1 += "b"
            p1
        }
        d1.await()
        p1 += "a"
        p1 = d2.await() + p1
        // unfortunately p1 = p1 + d2.await() would fail the test
        // because d2.await() updates p1 value too lately in the expression
        // not sure, how to avoid that

        return p1 // should be "abab"
    }

    override fun prop7(): String {
        p1 = taskA.reverse("a")

        async {
            p1 += taskA.reverse("b")
        }
        p1 += "c"
        taskA.await(200)
        p1 += "d"

        return p1 // should be "acbd"
    }

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
        val deferred: Deferred<Obj> = channelObj.receive("[?(\$.foo == \"foo\")]")

        return deferred.await()
    }

    override fun channel4ter(): Obj {
        val deferred: Deferred<Obj> = channelObj.receive("[?]", where("foo").eq("foo"))

        return deferred.await()
    }

    override fun channel5(): Obj1 {
        val deferred: Deferred<Obj1> = channelObj.receive(Obj1::class)

        return deferred.await()
    }

    override fun channel5bis(): Obj1 {
        val deferred: Deferred<Obj1> = channelObj.receive(Obj1::class, "[?(\$.foo == \"foo\")]")

        return deferred.await()
    }

    override fun channel5ter(): Obj1 {
        val deferred: Deferred<Obj1> = channelObj.receive(Obj1::class, "[?]", where("foo").eq("foo"))

        return deferred.await()
    }

    override fun channel6(): String {
        val deferred1: Deferred<Obj1> = channelObj.receive(Obj1::class)
        val deferred2: Deferred<Obj2> = channelObj.receive(Obj2::class)
        val obj1 = deferred1.await()
        val obj2 = deferred2.await()

        return obj1.foo + obj2.foo + obj1.bar * obj2.bar
    }

    override fun channel6bis(): String {
        val deferred1: Deferred<Obj1> = channelObj.receive(Obj1::class, "[?(\$.foo == \"foo\")]")
        val deferred2: Deferred<Obj2> = channelObj.receive(Obj2::class, "[?(\$.foo == \"foo\")]")
        val obj1 = deferred1.await()
        val obj2 = deferred2.await()

        return obj1.foo + obj2.foo + obj1.bar * obj2.bar
    }

    override fun channel6ter(): String {
        val deferred1: Deferred<Obj1> = channelObj.receive(Obj1::class, "[?]", where("foo").eq("foo"))
        val deferred2: Deferred<Obj2> = channelObj.receive(Obj2::class, "[?]", where("foo").eq("foo"))
        val obj1 = deferred1.await()
        val obj2 = deferred2.await()

        return obj1.foo + obj2.foo + obj1.bar * obj2.bar
    }

    override fun failing1() = try {
        taskA.failing()
        "ok"
    } catch (e: FailedDeferredException) {
        taskA.reverse("ok")
    }

    override fun failing2() = taskA.failing()

    override fun failing2a(): Long {
        async(taskA) { failing() }

        return taskA.await(100)
    }

    override fun failing3(): Long {
        async {
            taskA.failing()
        }

        return taskA.await(100)
    }

    override fun failing3b(): Long {
        async {
            throw Exception()
        }

        return taskA.await(100)
    }

    override fun failing4(): Long {
        val deferred = async(taskA) { await(1000) }
        taskA.cancelTaskA(deferred.id!!)

        return deferred.await()
    }

    override fun failing5(): Long {
        val deferred = async(taskA) { await(1000) }
        taskA.cancelTaskA(deferred.id!!)

        async { deferred.await() }

        return taskA.await(100)
    }

    override fun failing6() {
        val workflowABis = newWorkflow<WorkflowA>()

        return workflowABis.failing2()
    }

    override fun failing7(): Long {
        val workflowABis = newWorkflow<WorkflowA>()

        async {
            workflowABis.failing2()
        }
        return taskA.await(100)
    }

    override fun failing8() = taskA.successAtRetry()

    override fun failing9(): Boolean {
        // this method will complete only after retry
        val deferred = async(taskA) { successAtRetry() }

        val result = try {
            deferred.await()
        } catch (e: FailedDeferredException) {
            "caught"
        }

        taskA.retryTaskA(deferred.id!!)

        // we wait here only on avoid to make sure the previous retry is completed
        taskA.await(100)

        return deferred.await() == "ok" && result == "caught"
    }

    override fun failing10(): String {
        return "ok"
    }

    override fun cancel1() {
        val taggedChild = newWorkflow<WorkflowA>(tags = setOf("foo", "bar"))

        taggedChild.channel1()
    }
}
