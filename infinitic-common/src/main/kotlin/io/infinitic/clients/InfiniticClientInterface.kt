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
package io.infinitic.clients

import io.infinitic.workflows.Consumer0
import io.infinitic.workflows.Consumer1
import io.infinitic.workflows.Consumer2
import io.infinitic.workflows.Consumer3
import io.infinitic.workflows.Consumer4
import io.infinitic.workflows.Consumer5
import io.infinitic.workflows.Consumer6
import io.infinitic.workflows.Consumer7
import io.infinitic.workflows.Consumer8
import io.infinitic.workflows.Consumer9
import io.infinitic.workflows.DeferredStatus
import java.io.Closeable
import java.util.concurrent.CompletableFuture

interface InfiniticClientInterface : Closeable {
  /** Client's name - this name must be unique through all connected clients */
  val name: String

  /** Get last Deferred created by the call of a stub */
  val lastDeferred: Deferred<*>?

  /**
   * Create a stub for a new workflow
   *
   * @property klass the interface of a workflow
   * @property tags the workflow's tags
   * @property meta the workflow's meta
   */
  fun <T : Any> newWorkflow(
    klass: Class<out T>,
    tags: Set<String>? = null,
    meta: Map<String, ByteArray>?
  ): T

  /**
   * Create a stub for a new workflow
   *
   * @property klass the interface of a workflow
   * @property tags the workflow's tags
   */
  fun <T : Any> newWorkflow(klass: Class<out T>, tags: Set<String>? = null): T =
      newWorkflow(klass, tags, null)

  /**
   * Create a stub for a new workflow
   *
   * @property klass the interface of a workflow
   */
  fun <T : Any> newWorkflow(klass: Class<out T>): T = newWorkflow(klass, null, null)

  /**
   * Create a stub for an existing workflow (targeted by id)
   *
   * @property klass should the interface of a workflow
   * @property id should the workflow's id
   */
  fun <T : Any> getWorkflowById(klass: Class<out T>, id: String): T

  /**
   * Create a stub for existing workflow(s) (targeted by tag)
   *
   * @property klass should the interface of a workflow
   * @property tag should the workflow's tag
   */
  fun <T : Any> getWorkflowByTag(klass: Class<out T>, tag: String): T

  /** Dispatch without parameter a task or workflow returning an object */
  fun <R : Any?> dispatchAsync(method: () -> R): CompletableFuture<Deferred<R>> = startAsync {
    method.invoke()
  }

  /** Dispatch with 1 parameter a task or workflow returning an object */
  fun <P1, R : Any?> dispatchAsync(method: (p1: P1) -> R, p1: P1): CompletableFuture<Deferred<R>> =
      startAsync {
        method.invoke(p1)
      }

  /** Dispatch with 2 parameters a task or workflow returning an object */
  fun <P1, P2, R : Any?> dispatchAsync(
    method: (p1: P1, p2: P2) -> R,
    p1: P1,
    p2: P2
  ): CompletableFuture<Deferred<R>> = startAsync { method.invoke(p1, p2) }

  /** Dispatch with 3 parameters a task or workflow returning an object */
  fun <P1, P2, P3, R : Any?> dispatchAsync(
    method: (p1: P1, p2: P2, p3: P3) -> R,
    p1: P1,
    p2: P2,
    p3: P3
  ): CompletableFuture<Deferred<R>> = startAsync { method.invoke(p1, p2, p3) }

  /** Dispatch with 4 parameters a task or workflow returning an object */
  fun <P1, P2, P3, P4, R : Any?> dispatchAsync(
    method: (p1: P1, p2: P2, p3: P3, p4: P4) -> R,
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4
  ): CompletableFuture<Deferred<R>> = startAsync { method.invoke(p1, p2, p3, p4) }

  /** Dispatch with 5 parameters a task or workflow returning an object */
  fun <P1, P2, P3, P4, P5, R : Any?> dispatchAsync(
    method: (p1: P1, p2: P2, p3: P3, p4: P4, p5: P5) -> R,
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4,
    p5: P5
  ): CompletableFuture<Deferred<R>> = startAsync { method.invoke(p1, p2, p3, p4, p5) }

  /** Dispatch with 6 parameters a task or workflow returning an object */
  fun <P1, P2, P3, P4, P5, P6, R : Any?> dispatchAsync(
    method: (p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6) -> R,
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4,
    p5: P5,
    p6: P6
  ): CompletableFuture<Deferred<R>> = startAsync { method.invoke(p1, p2, p3, p4, p5, p6) }

  /** Dispatch with 7 parameters a task or workflow returning an object */
  fun <P1, P2, P3, P4, P5, P6, P7, R : Any?> dispatchAsync(
    method: (p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7) -> R,
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4,
    p5: P5,
    p6: P6,
    p7: P7
  ): CompletableFuture<Deferred<R>> = startAsync { method.invoke(p1, p2, p3, p4, p5, p6, p7) }

  /** Dispatch with 8 parameters a task or workflow returning an object */
  fun <P1, P2, P3, P4, P5, P6, P7, P8, R : Any?> dispatchAsync(
    method: (p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8) -> R,
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4,
    p5: P5,
    p6: P6,
    p7: P7,
    p8: P8
  ): CompletableFuture<Deferred<R>> = startAsync { method.invoke(p1, p2, p3, p4, p5, p6, p7, p8) }

  /** Dispatch with 9 parameters a task or workflow returning an object */
  fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, R : Any?> dispatchAsync(
    method: (p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9) -> R,
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4,
    p5: P5,
    p6: P6,
    p7: P7,
    p8: P8,
    p9: P9
  ): CompletableFuture<Deferred<R>> = startAsync {
    method.invoke(p1, p2, p3, p4, p5, p6, p7, p8, p9)
  }

  /** Dispatch without parameter a task or workflow returning an object */
  fun <R : Any?> dispatch(method: () -> R): Deferred<R> = dispatchAsync(method).join()

  /** Dispatch with 1 parameter a task or workflow returning an object */
  fun <P1, R : Any?> dispatch(method: (p1: P1) -> R, p1: P1): Deferred<R> =
      dispatchAsync(method, p1).join()

  /** Dispatch with 2 parameters a task or workflow returning an object */
  fun <P1, P2, R : Any?> dispatch(method: (p1: P1, p2: P2) -> R, p1: P1, p2: P2): Deferred<R> =
      dispatchAsync(method, p1, p2).join()

  /** Dispatch with 3 parameters a task or workflow returning an object */
  fun <P1, P2, P3, R : Any?> dispatch(
    method: (p1: P1, p2: P2, p3: P3) -> R,
    p1: P1,
    p2: P2,
    p3: P3
  ): Deferred<R> = dispatchAsync(method, p1, p2, p3).join()

  /** Dispatch with 4 parameters a task or workflow returning an object */
  fun <P1, P2, P3, P4, R : Any?> dispatch(
    method: (p1: P1, p2: P2, p3: P3, p4: P4) -> R,
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4
  ): Deferred<R> = dispatchAsync(method, p1, p2, p3, p4).join()

  /** Dispatch with 5 parameters a task or workflow returning an object */
  fun <P1, P2, P3, P4, P5, R : Any?> dispatch(
    method: (p1: P1, p2: P2, p3: P3, p4: P4, p5: P5) -> R,
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4,
    p5: P5
  ): Deferred<R> = dispatchAsync(method, p1, p2, p3, p4, p5).join()

  /** Dispatch with 6 parameters a task or workflow returning an object */
  fun <P1, P2, P3, P4, P5, P6, R : Any?> dispatch(
    method: (p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6) -> R,
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4,
    p5: P5,
    p6: P6
  ): Deferred<R> = dispatchAsync(method, p1, p2, p3, p4, p5, p6).join()

  /** Dispatch with 7 parameters a task or workflow returning an object */
  fun <P1, P2, P3, P4, P5, P6, P7, R : Any?> dispatch(
    method: (p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7) -> R,
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4,
    p5: P5,
    p6: P6,
    p7: P7
  ): Deferred<R> = dispatchAsync(method, p1, p2, p3, p4, p5, p6, p7).join()

  /** Dispatch with 8 parameters a task or workflow returning an object */
  fun <P1, P2, P3, P4, P5, P6, P7, P8, R : Any?> dispatch(
    method: (p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8) -> R,
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4,
    p5: P5,
    p6: P6,
    p7: P7,
    p8: P8
  ): Deferred<R> = dispatchAsync(method, p1, p2, p3, p4, p5, p6, p7, p8).join()

  /** Dispatch with 9 parameters a task or workflow returning an object */
  fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, R : Any?> dispatch(
    method: (p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9) -> R,
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4,
    p5: P5,
    p6: P6,
    p7: P7,
    p8: P8,
    p9: P9
  ): Deferred<R> = dispatchAsync(method, p1, p2, p3, p4, p5, p6, p7, p8, p9).join()

  /** Dispatch without parameter a task or workflow returning void */
  fun dispatchVoidAsync(method: Consumer0): CompletableFuture<Deferred<Void>> = startVoidAsync {
    method.apply()
  }

  /** Dispatch with 1 parameter a task or workflow returning void */
  fun <P1> dispatchVoidAsync(method: Consumer1<P1>, p1: P1): CompletableFuture<Deferred<Void>> =
      startVoidAsync {
        method.apply(p1)
      }

  /** Dispatch with 2 parameters a task or workflow returning void */
  fun <P1, P2> dispatchVoidAsync(
    method: Consumer2<P1, P2>,
    p1: P1,
    p2: P2
  ): CompletableFuture<Deferred<Void>> = startVoidAsync { method.apply(p1, p2) }

  /** Dispatch with 3 parameters a task or workflow returning void */
  fun <P1, P2, P3> dispatchVoidAsync(
    method: Consumer3<P1, P2, P3>,
    p1: P1,
    p2: P2,
    p3: P3
  ): CompletableFuture<Deferred<Void>> = startVoidAsync { method.apply(p1, p2, p3) }

  /** Dispatch with 4 parameters a task or workflow returning void */
  fun <P1, P2, P3, P4> dispatchVoidAsync(
    method: Consumer4<P1, P2, P3, P4>,
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4
  ): CompletableFuture<Deferred<Void>> = startVoidAsync { method.apply(p1, p2, p3, p4) }

  /** Dispatch with 5 parameters a task or workflow returning void */
  fun <P1, P2, P3, P4, P5> dispatchVoidAsync(
    method: Consumer5<P1, P2, P3, P4, P5>,
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4,
    p5: P5
  ): CompletableFuture<Deferred<Void>> = startVoidAsync { method.apply(p1, p2, p3, p4, p5) }

  /** Dispatch with 6 parameters a task or workflow returning void */
  fun <P1, P2, P3, P4, P5, P6> dispatchVoidAsync(
    method: Consumer6<P1, P2, P3, P4, P5, P6>,
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4,
    p5: P5,
    p6: P6
  ): CompletableFuture<Deferred<Void>> = startVoidAsync { method.apply(p1, p2, p3, p4, p5, p6) }

  /** Dispatch with 7 parameters a task or workflow returning void */
  fun <P1, P2, P3, P4, P5, P6, P7> dispatchVoidAsync(
    method: Consumer7<P1, P2, P3, P4, P5, P6, P7>,
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4,
    p5: P5,
    p6: P6,
    p7: P7
  ): CompletableFuture<Deferred<Void>> = startVoidAsync { method.apply(p1, p2, p3, p4, p5, p6, p7) }

  /** Dispatch with 8 parameters a task or workflow returning void */
  fun <P1, P2, P3, P4, P5, P6, P7, P8> dispatchVoidAsync(
    method: Consumer8<P1, P2, P3, P4, P5, P6, P7, P8>,
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4,
    p5: P5,
    p6: P6,
    p7: P7,
    p8: P8
  ): CompletableFuture<Deferred<Void>> = startVoidAsync {
    method.apply(p1, p2, p3, p4, p5, p6, p7, p8)
  }

  /** Dispatch with 9 parameters a task or workflow returning void */
  fun <P1, P2, P3, P4, P5, P6, P7, P8, P9> dispatchVoidAsync(
    method: Consumer9<P1, P2, P3, P4, P5, P6, P7, P8, P9>,
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4,
    p5: P5,
    p6: P6,
    p7: P7,
    p8: P8,
    p9: P9
  ): CompletableFuture<Deferred<Void>> = startVoidAsync {
    method.apply(p1, p2, p3, p4, p5, p6, p7, p8, p9)
  }

  /** Dispatch without parameter a task or workflow returning void */
  fun dispatchVoid(method: Consumer0): Deferred<Void> = dispatchVoidAsync(method).join()

  /** Dispatch with 1 parameter a task or workflow returning void */
  fun <P1> dispatchVoid(method: Consumer1<P1>, p1: P1): Deferred<Void> =
      dispatchVoidAsync(method, p1).join()

  /** Dispatch with 2 parameters a task or workflow returning void */
  fun <P1, P2> dispatchVoid(method: Consumer2<P1, P2>, p1: P1, p2: P2): Deferred<Void> =
      dispatchVoidAsync(method, p1, p2).join()

  /** Dispatch with 3 parameters a task or workflow returning void */
  fun <P1, P2, P3> dispatchVoid(
    method: Consumer3<P1, P2, P3>,
    p1: P1,
    p2: P2,
    p3: P3
  ): Deferred<Void> = dispatchVoidAsync(method, p1, p2, p3).join()

  /** Dispatch with 4 parameters a task or workflow returning void */
  fun <P1, P2, P3, P4> dispatchVoid(
    method: Consumer4<P1, P2, P3, P4>,
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4
  ): Deferred<Void> = dispatchVoidAsync(method, p1, p2, p3, p4).join()

  /** Dispatch with 5 parameters a task or workflow returning void */
  fun <P1, P2, P3, P4, P5> dispatchVoid(
    method: Consumer5<P1, P2, P3, P4, P5>,
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4,
    p5: P5
  ): Deferred<Void> = dispatchVoidAsync(method, p1, p2, p3, p4, p5).join()

  /** Dispatch with 6 parameters a task or workflow returning void */
  fun <P1, P2, P3, P4, P5, P6> dispatchVoid(
    method: Consumer6<P1, P2, P3, P4, P5, P6>,
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4,
    p5: P5,
    p6: P6
  ): Deferred<Void> = dispatchVoidAsync(method, p1, p2, p3, p4, p5, p6).join()

  /** Dispatch with 7 parameters a task or workflow returning void */
  fun <P1, P2, P3, P4, P5, P6, P7> dispatchVoid(
    method: Consumer7<P1, P2, P3, P4, P5, P6, P7>,
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4,
    p5: P5,
    p6: P6,
    p7: P7
  ): Deferred<Void> = dispatchVoidAsync(method, p1, p2, p3, p4, p5, p6, p7).join()

  /** Dispatch with 8 parameters a task or workflow returning void */
  fun <P1, P2, P3, P4, P5, P6, P7, P8> dispatchVoid(
    method: Consumer8<P1, P2, P3, P4, P5, P6, P7, P8>,
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4,
    p5: P5,
    p6: P6,
    p7: P7,
    p8: P8
  ): Deferred<Void> = dispatchVoidAsync(method, p1, p2, p3, p4, p5, p6, p7, p8).join()

  /** Dispatch with 9 parameters a task or workflow returning void */
  fun <P1, P2, P3, P4, P5, P6, P7, P8, P9> dispatchVoid(
    method: Consumer9<P1, P2, P3, P4, P5, P6, P7, P8, P9>,
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4,
    p5: P5,
    p6: P6,
    p7: P7,
    p8: P8,
    p9: P9
  ): Deferred<Void> = dispatchVoidAsync(method, p1, p2, p3, p4, p5, p6, p7, p8, p9).join()

  /**
   * Cancel workflow(s) (without waiting for the message to be sent)
   *
   * @property stub should be a workflow stub obtained by [getWorkflowById] or [getWorkflowByTag]
   */
  fun cancelAsync(stub: Any): CompletableFuture<Unit>

  /**
   * Cancel workflow(s)
   *
   * @property stub should be a workflow stub obtained by [getWorkflowById] or [getWorkflowByTag]
   */
  fun cancel(stub: Any): Unit = cancelAsync(stub).join()

  /**
   * Complete ongoing timers (without waiting for the message to be sent)
   *
   * @property stub should be a workflow stub obtained by [getWorkflowById] or [getWorkflowByTag]
   * @id of the targeted method run - main one if null
   */
  fun completeTimersAsync(stub: Any, id: String? = null): CompletableFuture<Unit>

  /**
   * Complete ongoing timers
   *
   * @property stub should be a workflow stub obtained by [getWorkflowById] or [getWorkflowByTag]
   * @id of the method run - main one if null
   */
  fun completeTimers(stub: Any, id: String? = null): Unit = completeTimersAsync(stub, id).join()

  /**
   * Retry the workflow task (without waiting for the message to be sent)
   *
   * @property stub should be a workflow stub obtained by [getWorkflowById] or [getWorkflowByTag]
   */
  fun retryWorkflowTaskAsync(stub: Any): CompletableFuture<Unit>

  /**
   * Retry the workflow task
   *
   * @property stub should be a workflow stub obtained by [getWorkflowById] or [getWorkflowByTag]
   */
  fun retryWorkflowTask(stub: Any): Unit = retryWorkflowTaskAsync(stub).join()

  /**
   * Retry task in workflow(s) (without waiting for the message to be sent)
   *
   * @property stub should be a workflow stub obtained by [getWorkflowById] or [getWorkflowByTag]
   * @property taskId should be the if of the task to retry (null if all)
   */
  fun retryTasksAsync(stub: Any, taskId: String): CompletableFuture<Unit>

  /**
   * Retry task in workflow(s)
   *
   * @property stub should be a workflow stub obtained by [getWorkflowById] or [getWorkflowByTag]
   * @property taskId should be the if of the task to retry (null if all)
   */
  fun retryTasks(stub: Any, taskId: String): Unit = retryTasksAsync(stub, taskId).join()

  /**
   * Retry task(s) in workflow(s) (without waiting for the message to be sent)
   *
   * @property stub should be a workflow stub obtained by [getWorkflowById] or [getWorkflowByTag]
   * @property taskStatus should be the status of the task(s) to retry (null if all)
   * @property taskClass should be the interface of the task(s) to retry (null if all)
   *
   * [taskStatus] and [taskClass] can be combined, eg. to target failed tasks of a specific class.
   */
  fun retryTasksAsync(
    stub: Any,
    taskStatus: DeferredStatus? = null,
    taskClass: Class<*>? = null
  ): CompletableFuture<Unit>

  /**
   * Retry task(s) in workflow(s)
   *
   * @property stub should be a workflow stub obtained by [getWorkflowById] or [getWorkflowByTag]
   * @property taskStatus should be the status of the task(s) to retry (null if all)
   * @property taskClass should be the interface of the task(s) to retry (null if all)
   *
   * [taskStatus] and [taskClass] can be combined, e.g. to target failed tasks of a specific class.
   */
  fun retryTasks(stub: Any, taskStatus: DeferredStatus? = null, taskClass: Class<*>? = null): Unit =
      retryTasksAsync(stub, taskStatus, taskClass).join()

  /**
   * Get ids of running workflows
   *
   * @property stub should be a workflow stub obtained by [getWorkflowById] or [getWorkflowByTag]
   */
  fun <T : Any> getIds(stub: T): Set<String>

  fun <R> startAsync(invoke: () -> R): CompletableFuture<Deferred<R>>

  fun startVoidAsync(invoke: () -> Unit): CompletableFuture<Deferred<Void>>
}
