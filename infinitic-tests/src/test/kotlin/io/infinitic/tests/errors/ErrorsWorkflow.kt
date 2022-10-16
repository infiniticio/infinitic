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

package io.infinitic.tests.errors

import io.infinitic.annotations.Ignore
import io.infinitic.exceptions.FailedDeferredException
import io.infinitic.exceptions.FailedTaskException
import io.infinitic.exceptions.FailedWorkflowException
import io.infinitic.exceptions.UnknownWorkflowException
import io.infinitic.tests.utils.UtilService
import io.infinitic.workflows.Deferred
import io.infinitic.workflows.Workflow
import java.time.Duration

interface ErrorsWorkflow {
    fun waiting(): String
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
    fun failing13()
}

@Suppress("unused")
class ErrorsWorkflowImpl : Workflow(), ErrorsWorkflow {

    @Ignore
    private val self by lazy { getWorkflowById(ErrorsWorkflow::class.java, workflowId) }

    lateinit var deferred: Deferred<String>

    private val utilService =
        newService(UtilService::class.java, tags = setOf("foo", "bar"), meta = mapOf("foo" to "bar".toByteArray()))
    private val errorsWorkflow = newWorkflow(ErrorsWorkflow::class.java, tags = setOf("foo", "bar"))

    private var p1 = ""

    override fun waiting(): String {
        timer(Duration.ofSeconds(60)).await()

        return "ok"
    }

    override fun failing1() = try {
        utilService.failing()
        "ok"
    } catch (e: FailedTaskException) {
        utilService.reverse("ok")
    }

    override fun failing2() = utilService.failing()

    override fun failing2a(): Long {
        dispatch(utilService::failing)

        return utilService.await(100)
    }

    override fun failing3(): Long {
        dispatch(self::failing3bis)

        return utilService.await(100)
    }

    override fun failing3bis() {
        utilService.failing()
    }

    override fun failing3b(): Long {
        dispatch(self::failing3bException)

        return utilService.await(100)
    }

    override fun failing3bException() {
        throw Exception()
    }

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

    override fun failing5bis(deferred: Deferred<Long>): Long {
        return deferred.await()
    }

    override fun failing6() = errorsWorkflow.failing2()

    override fun failing7(): Long {
        dispatch(self::failing7bis)

        return utilService.await(100)
    }

    override fun failing7bis() {
        errorsWorkflow.failing2()
    }

    override fun failing7ter(): String = try {
        errorsWorkflow.failing2()
        "ok"
    } catch (e: FailedWorkflowException) {
        val deferredException = e.deferredException as FailedTaskException
        utilService.await(100)
        deferredException.workerException.name
    }

    override fun failing8() = utilService.successAtRetry()

    override fun failing9(): Boolean {
        // this method will success only after retry
        val deferred = dispatch(utilService::successAtRetry)

        val result = try {
            deferred.await()
        } catch (e: FailedDeferredException) {
            "caught"
        }

        // trigger the retry of the previous task
        utilService.retryFailedTasks(ErrorsWorkflow::class.java.name, workflowId)

        return deferred.await() == "ok" && result == "caught"
    }

    override fun failing10(): String {
        p1 = "o"

        val deferred = dispatch(utilService::successAtRetry)

        dispatch(self::failing10bis)

        try {
            deferred.await()
        } catch (e: FailedDeferredException) {
            // continue
        }

        return p1 // should be "ok"
    }

    override fun failing10bis() {
        p1 += "k"
    }

    override fun failing11() {
        getWorkflowById(ErrorsWorkflow::class.java, "unknown").waiting()
    }

    override fun failing12(): String {
        return try {
            getWorkflowById(ErrorsWorkflow::class.java, "unknown").waiting()
        } catch (e: UnknownWorkflowException) {
            utilService.reverse("caught".reversed())
        }
    }

    override fun failing13() {
        newWorkflow(ErrorsWorkflow::class.java, tags = tags).waiting()
    }
}
