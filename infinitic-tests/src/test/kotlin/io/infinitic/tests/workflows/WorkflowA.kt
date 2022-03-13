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
import io.infinitic.annotations.Ignore
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.exceptions.FailedDeferredException
import io.infinitic.exceptions.FailedTaskException
import io.infinitic.exceptions.FailedWorkflowException
import io.infinitic.exceptions.UnknownWorkflowException
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
    fun context1(): String
    fun context2(): Set<String>
    fun context3(): WorkflowMeta
    fun context4(): String?
    fun context5(): String?
    fun context6(): Set<String>
    fun context7(): TaskMeta
    fun seq1(): String
    fun seq2(): String
    fun seq3(): String
    fun seq3bis(): String
    fun seq4(): String
    fun seq4bis(): String
    fun seq5(): Long
    fun seq6(): Long
    fun deferred1(): String
    fun deferred1bis(): String
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
    fun prop1bis()
    fun prop2(): String
    fun prop2bis()
    fun prop3(): String
    fun prop3bis()
    fun prop4(): String
    fun prop4bis()
    fun prop5(): String
    fun prop5bis()
    fun prop5ter()
    fun prop6(): String
    fun prop6bis(deferred: Deferred<String>): String
    fun prop7(): String
    fun prop7bis(): String
    fun prop8(): String
    fun prop8bis()
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
    fun failing3bis()
    fun failing3bException()
    fun failing3b(): Long
//    fun failing4(): Long
//    fun failing5(): Long
    fun failing5bis(deferred: Deferred<Long>): Long
    fun failing6()
    fun failing7(): Long
    fun failing7bis()
    fun failing7ter(): String
    fun failing8(): String
    fun failing9(): Boolean
    fun failing10(): String
    fun failing10bis()
    fun failing11()
    fun failing12(): String
    fun cancel1()
}

@Suppress("unused")
class WorkflowAImpl : Workflow(), WorkflowA {

    @Ignore private val self by lazy { getWorkflowById(WorkflowA::class.java, context.id) }

    lateinit var deferred: Deferred<String>

    override val channelObj = channel<Obj>()
    override val channelA = channel<String>()
    override val channelB = channel<String>()

    private val taskA = newTask(TaskA::class.java, tags = setOf("foo", "bar"), meta = mapOf("foo" to "bar".toByteArray()))
    private val workflowA = newWorkflow(WorkflowA::class.java)

    private var p1 = ""

    override fun empty() = "void"

    override fun await(duration: Long) = taskA.await(duration)

    override fun parent() = taskA.parent()

    override fun wparent(): String = workflowA.parent()

    override fun context1(): String = context.id

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
        val d = dispatch(taskA::reverse, "ab")
        str = taskA.concat(str, "2")
        str = taskA.concat(str, "3")

        return str + d.await() // should be "23ba"
    }

    override fun seq3(): String {
        var str = ""
        val d = dispatch(self::seq3bis)
        str = taskA.concat(str, "2")
        str = taskA.concat(str, "3")

        return str + d.await() // should be "23ba"
    }

    override fun seq3bis(): String { return taskA.reverse("ab") }

    override fun seq4(): String {
        var str = ""
        val d = dispatch(self::seq4bis)
        str = taskA.concat(str, "2")
        str = taskA.concat(str, "3")

        return str + d.await() // should be "23bac"
    }

    override fun seq4bis(): String {
        val s = taskA.reverse("ab")
        return taskA.concat(s, "c")
    }

    override fun seq5(): Long {
        var l = 0L
        val d1 = dispatch(taskA::await, 500)
        val d2 = dispatch(taskA::await, 100)
        l += d1.await()
        l += d2.await()

        return l // should be 600
    }

    override fun seq6(): Long {
        var l = 0L
        val d1 = dispatch(taskA::await, 500)
        val d2 = dispatch(taskA::await, 100)
        l += d1.await()
        l += d2.await()

        // a new step triggers an additional workflowTask
        taskA.workflowId()

        return l // should be 600
    }

    override fun deferred1(): String {
        var str = ""
        val d = dispatch(self::deferred1bis)
        str += d.isOngoing().toString()
        str += d.isCompleted().toString()
        d.await()
        str += d.isOngoing().toString()
        str += d.isCompleted().toString()

        return str // should be "truefalsefalsetrue"
    }

    override fun deferred1bis(): String { return taskA.reverse("X") }

    override fun or1(): String {
        val d1 = dispatch(taskA::reverse, "ab")
        val d2 = dispatch(taskA::reverse, "cd")
        val d3 = dispatch(taskA::reverse, "ef")

        return (d1 or d2 or d3).await() // should be "ba" or "dc" or "fe"
    }

    override fun or2(): Any {
        val d1 = dispatch(taskA::reverse, "ab")
        val d2 = dispatch(taskA::reverse, "cd")
        val d3 = dispatch(taskA::reverse, "ef")

        return ((d1 and d2) or d3).await() // should be listOf("ba","dc") or "fe"
    }

    override fun or3(): String {
        val list: MutableList<Deferred<String>> = mutableListOf()
        list.add(dispatch(taskA::reverse, "ab"))
        list.add(dispatch(taskA::reverse, "cd"))
        list.add(dispatch(taskA::reverse, "ef"))

        return list.or().await() // should be "ba" or "dc" or "fe"
    }

    override fun or4(): String {
        var s3 = taskA.concat("1", "2")

        val d1 = dispatch(taskA::reverse, "ab")
        val d2 = timer(Duration.ofMillis(2000))
        val d: Deferred<Any> = (d1 or d2)
        if (d.status() != DeferredStatus.COMPLETED) {
            s3 = taskA.reverse("ab")
        }

        return (d.await() as String) + s3 // should be "baba"
    }

    override fun and1(): List<String> {
        val d1 = dispatch(taskA::reverse, "ab")
        val d2 = dispatch(taskA::reverse, "cd")
        val d3 = dispatch(taskA::reverse, "ef")

        return (d1 and d2 and d3).await() // should be listOf("ba","dc","fe")
    }

    override fun and2(): List<String> {

        val list: MutableList<Deferred<String>> = mutableListOf()
        list.add(dispatch(taskA::reverse, "ab"))
        list.add(dispatch(taskA::reverse, "cd"))
        list.add(dispatch(taskA::reverse, "ef"))

        return list.and().await() // should be listOf("ba","dc","fe")
    }

    override fun and3(): List<String> {

        val list: MutableList<Deferred<String>> = mutableListOf()
        for (i in 1..20) {
            list.add(dispatch(taskA::reverse, "ab"))
        }
        return list.and().await() // should be listOf("ba","dc","fe")
    }

    override fun inline1(i: Int): String {
        val result = inline { 2 * i }
        return taskA.concat("2 * $i = ", "$result")
    }

    override fun inline2(i: Int): String {
        val result = inline {
            dispatch(taskA::reverse, "ab")
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
        val workflowB = newWorkflow(WorkflowB::class.java)
        var str: String = workflowB.concat("-")
        str = taskA.concat(str, "-")

        return str // should be "-abc-"
    }

    override fun child2(): String {
        val workflowB = newWorkflow(WorkflowB::class.java)
        val str = taskA.reverse("12")
        val d = dispatch(workflowB::concat, str)

        return taskA.concat(d.await(), str) // should be "21abc21"
    }

    override fun prop1(): String {
        p1 = "a"
        dispatch(self::prop1bis)
        p1 += "c"

        return p1 // should be "ac"
    }

    override fun prop1bis() { p1 += "b" }

    override fun prop2(): String {
        p1 = "a"
        dispatch(self::prop2bis)
        p1 += "c"
        taskA.await(100)
        p1 += "d"

        return p1 // should be "acbd"
    }

    override fun prop2bis() { p1 += "b" }

    override fun prop3(): String {
        p1 = "a"
        dispatch(self::prop3bis)
        p1 += "c"
        taskA.await(2000)
        p1 += "d"

        return p1 // should be "acbd"
    }

    override fun prop3bis() { taskA.await(50); p1 += "b" }

    override fun prop4(): String {
        p1 = "a"
        dispatch(self::prop4bis)
        p1 += "c"
        taskA.await(100)
        p1 += "d"

        return p1 // should be "acd"
    }

    override fun prop4bis() { taskA.await(200); p1 += "b" }

    override fun prop5(): String {
        p1 = "a"
        dispatch(self::prop5bis)
        dispatch(self::prop5ter)
        p1 += "d"
        taskA.await(100)

        return p1 // should be "adbc"
    }

    override fun prop5bis() { p1 += "b" }
    override fun prop5ter() { p1 += "c" }

    override fun prop6(): String {
        val d1 = dispatch(taskA::reverse, "12")
        val d2 = dispatch(self::prop6bis, d1)
        d1.await()
        p1 += "a"
        p1 = d2.await() + p1
        // unfortunately p1 = p1 + d2.await() would fail the test
        // because d2.await() updates p1 value too lately in the expression
        // not sure, how to fix that or if it should be fixed

        return p1 // should be "abab"
    }

    override fun prop6bis(deferred: Deferred<String>): String { deferred.await(); p1 += "b"; return p1 }

    override fun prop7(): String {
        deferred = dispatch(taskA::reverse, "12")
        val d2 = dispatch(self::prop7bis)
        deferred.await()
        p1 += "a"
        p1 = d2.await() + p1
        // unfortunately p1 = p1 + d2.await() would fail the test
        // because d2.await() updates p1 value too lately in the expression
        // not sure, how to fix that or if it should be fixed

        return p1 // should be "abab"
    }

    override fun prop7bis(): String { deferred.await(); p1 += "b"; return p1 }

    override fun prop8(): String {
        p1 = taskA.reverse("a")
        dispatch(self::prop8bis)
        p1 += "c"
        taskA.await(200)
        p1 += "d"

        return p1 // should be "acbd"
    }

    override fun prop8bis() { p1 += taskA.reverse("b") }

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
        val deferred: Deferred<Obj1> = channelObj.receive(Obj1::class.java)

        return deferred.await()
    }

    override fun channel5bis(): Obj1 {
        val deferred: Deferred<Obj1> = channelObj.receive(Obj1::class.java, "[?(\$.foo == \"foo\")]")

        return deferred.await()
    }

    override fun channel5ter(): Obj1 {
        val deferred: Deferred<Obj1> = channelObj.receive(Obj1::class.java, "[?]", where("foo").eq("foo"))

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
        val deferred1: Deferred<Obj1> = channelObj.receive(Obj1::class.java, "[?(\$.foo == \"foo\")]")
        val deferred2: Deferred<Obj2> = channelObj.receive(Obj2::class.java, "[?(\$.foo == \"foo\")]")
        val obj1 = deferred1.await()
        val obj2 = deferred2.await()

        return obj1.foo + obj2.foo + obj1.bar * obj2.bar
    }

    override fun channel6ter(): String {
        val deferred1: Deferred<Obj1> = channelObj.receive(Obj1::class.java, "[?]", where("foo").eq("foo"))
        val deferred2: Deferred<Obj2> = channelObj.receive(Obj2::class.java, "[?]", where("foo").eq("foo"))
        val obj1 = deferred1.await()
        val obj2 = deferred2.await()

        return obj1.foo + obj2.foo + obj1.bar * obj2.bar
    }

    override fun failing1() = try {
        taskA.failing()
        "ok"
    } catch (e: FailedTaskException) {
        taskA.reverse("ok")
    }

    override fun failing2() = taskA.failing()

    override fun failing2a(): Long {
        dispatch(taskA::failing)

        return taskA.await(100)
    }

    override fun failing3(): Long {
        dispatch(self::failing3bis)

        return taskA.await(100)
    }

    override fun failing3bis() { taskA.failing() }

    override fun failing3b(): Long {
        dispatch(self::failing3bException)

        return taskA.await(100)
    }

    override fun failing3bException() { throw Exception() }

//    override fun failing4(): Long {
//        val deferred = dispatch(taskA::await, 1000)
//
//        taskA.cancelTaskA(deferred.id!!)
//
//        return deferred.await()
//    }
//
//    override fun failing5(): Long {
//        val deferred = dispatch(taskA::await, 1000)
//
//        taskA.cancelTaskA(deferred.id!!)
//
//        dispatch(self::failing5bis, deferred)
//
//        return taskA.await(100)
//    }

    override fun failing5bis(deferred: Deferred<Long>): Long { return deferred.await() }

    override fun failing6() = workflowA.failing2()

    override fun failing7(): Long {
        dispatch(self::failing7bis)

        return taskA.await(100)
    }

    override fun failing7bis() { workflowA.failing2() }

    override fun failing7ter(): String = try {
        workflowA.failing2()
        "ok"
    } catch (e: FailedWorkflowException) {
        val deferredException = e.deferredException as FailedTaskException
        taskA.await(100)
        deferredException.workerException.name
    }

    override fun failing8() = taskA.successAtRetry()

    override fun failing9(): Boolean {
        // this method will success only after retry
        val deferred = dispatch(taskA::successAtRetry)

        val result = try {
            deferred.await()
        } catch (e: FailedDeferredException) {
            "caught"
        }

        // trigger the retry of the previous task
        taskA.retryFailedTasks(WorkflowA::class.java.name, context.id)

        return deferred.await() == "ok" && result == "caught"
    }

    override fun failing10(): String {
        p1 = "o"

        val deferred = dispatch(taskA::successAtRetry)

        dispatch(self::failing10bis)

        try {
            deferred.await()
        } catch (e: FailedDeferredException) {
            // continue
        }

        return p1 // should be "ok"
    }

    override fun failing10bis() { p1 += "k" }

    override fun failing11() {
        getWorkflowById(WorkflowA::class.java, "unknown").empty()
    }

    override fun failing12(): String {
        return try {
            getWorkflowById(WorkflowA::class.java, "unknown").empty()
        } catch (e: UnknownWorkflowException) {
            taskA.reverse("caught".reversed())
        }
    }

    override fun cancel1() {
        val taggedChild = newWorkflow(WorkflowA::class.java, tags = setOf("foo", "bar"))

        taggedChild.channel1()
    }
}
