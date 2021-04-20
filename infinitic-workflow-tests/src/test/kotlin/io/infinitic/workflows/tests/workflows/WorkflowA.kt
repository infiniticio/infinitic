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

package io.infinitic.workflows.tests.workflows

import com.jayway.jsonpath.Criteria.where
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.workflows.Deferred
import io.infinitic.workflows.DeferredStatus
import io.infinitic.workflows.SendChannel
import io.infinitic.workflows.Workflow
import io.infinitic.workflows.and
import io.infinitic.workflows.or
import io.infinitic.workflows.tests.tasks.TaskA
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

sealed class Obj
@Serializable
data class Obj1(val foo: String, val bar: Int) : Obj()
@Serializable
data class Obj2(val foo: String, val bar: Int) : Obj()
@Serializable
data class Obj3(val foo: String) : Obj()

interface WorkflowA {
    val channelObj: SendChannel<Obj>
    val channelA: SendChannel<String>
    val channelB: SendChannel<String>

    fun empty(): String
    fun context1(): UUID
    fun context2(): Set<String>
    fun context3(): WorkflowMeta
    fun context4(): UUID?
    fun context5(): String?
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
    fun inline1(): String
    fun inline2(): String
    fun inline3(): String
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
}

class WorkflowAImpl : Workflow(), WorkflowA {
    override val channelObj = channel<Obj>()
    override val channelA = channel<String>()
    override val channelB = channel<String>()

    private val taskA = newTask<TaskA>()
    private val workflowB = newWorkflow<WorkflowB>()
    private var p1 = ""

    override fun empty() = "void"

    override fun context1(): UUID = context.id

    override fun context2(): Set<String> = context.tags

    override fun context3() = WorkflowMeta(context.meta)

    override fun context4() = taskA.workflowId()

    override fun context5() = taskA.workflowName()

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

    override fun inline1(): String {
        val date = inline { LocalDateTime.now() }
        return taskA.concat("Current Date and Time is: ", "$date") // should not throw
    }

    override fun inline2(): String {
        val date = inline {
            async(taskA) { reverse("ab") }
            LocalDateTime.now()
        }

        return taskA.concat("Current Date and Time is: ", "$date") // should not throw
    }

    override fun inline3(): String {
        val date = inline {
            taskA.concat("1", "2")
            LocalDateTime.now()
        }
        return taskA.concat("Current Date and Time is: ", "$date") // should throw
    }

    override fun child1(): String {

        var str: String = workflowB.concat("-")
        str = taskA.concat(str, "-")

        return str // should be "-abc-"
    }

    override fun child2(): String {
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
        val deferredTimer = timer(Duration.ofMillis(100))

        return (deferredChannel or deferredTimer).await()
    }

    override fun channel3(): Any {
        timer(Duration.ofMillis(100)).await()
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
}
