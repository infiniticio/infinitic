/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */
package io.infinitic.tests.errors

import io.infinitic.annotations.Ignore
import io.infinitic.exceptions.DeferredFailedException
import io.infinitic.exceptions.TaskFailedException
import io.infinitic.exceptions.WorkflowFailedException
import io.infinitic.exceptions.WorkflowUnknownException
import io.infinitic.utils.UtilService
import io.infinitic.workflows.Deferred
import io.infinitic.workflows.Workflow
import java.time.Duration

interface ErrorsWorkflow {
  fun waiting(): String

  fun failing0(): String

  fun failing1(): String

  fun failingWithException()

  fun failingWithThrowable()

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

  private val utilService = newService(
      UtilService::class.java,
      tags = setOf("foo", "bar"),
      meta = mutableMapOf("foo" to "bar".toByteArray()),
  )

  private val errorsWorkflow = newWorkflow(ErrorsWorkflow::class.java, tags = setOf("foo", "bar"))

  private var p1 = ""

  override fun waiting(): String {
    timer(Duration.ofSeconds(60)).await()

    return "ok"
  }

  override fun failing0(): String {
    utilService.reverse("ok")
    throw Exception()
  }

  override fun failing1() = try {
    utilService.failingWithException()
    "ok"
  } catch (e: TaskFailedException) {
    utilService.reverse("ok")
  }

  override fun failingWithException() = utilService.failingWithException()

  override fun failingWithThrowable() = utilService.failingWithThrowable()

  override fun failing2a(): Long {
    dispatch(utilService::failingWithException)

    return utilService.await(100)
  }

  override fun failing3(): Long {
    dispatch(self::failing3bis)

    return utilService.await(100)
  }

  override fun failing3bis() {
    utilService.failingWithException()
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

  override fun failing6() = errorsWorkflow.failingWithException()

  override fun failing7(): Long {
    dispatch(self::failing7bis)

    return utilService.await(100)
  }

  override fun failing7bis() {
    errorsWorkflow.failingWithException()
  }

  override fun failing7ter(): String =
      try {
        errorsWorkflow.failingWithException()
        "ok"
      } catch (e: WorkflowFailedException) {
        val deferredException = e.deferredException as TaskFailedException
        utilService.await(100)
        deferredException.workerException.name
      }

  override fun failing8() = utilService.successAtRetry()

  override fun failing9(): Boolean {
    // this method will success only after retry
    val deferred = dispatch(utilService::successAtRetry)

    val result = try {
      deferred.await()
    } catch (e: DeferredFailedException) {
      "caught"
    }

    // trigger the retry of the previous task
    utilService.retryFailedTasks(ErrorsWorkflow::class.java.name, workflowId)

    // ensure that workflow received the retryFailedTasks request
    utilService.await(200)

    return deferred.await() == "ok" && result == "caught"
  }

  override fun failing10(): String {
    p1 = "o"

    val deferred = dispatch(utilService::successAtRetry)

    dispatch(self::failing10bis)

    try {
      deferred.await()
    } catch (e: DeferredFailedException) {
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
    } catch (e: WorkflowUnknownException) {
      utilService.reverse("caught".reversed())
    }
  }

  override fun failing13() {
    newWorkflow(ErrorsWorkflow::class.java, tags = tags).waiting()
  }
}
