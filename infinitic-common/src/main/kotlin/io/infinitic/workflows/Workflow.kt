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
package io.infinitic.workflows

import io.infinitic.annotations.Ignore
import io.infinitic.common.proxies.ExistingWorkflowProxyHandler
import io.infinitic.common.proxies.NewServiceProxyHandler
import io.infinitic.common.proxies.NewWorkflowProxyHandler
import io.infinitic.common.proxies.ProxyHandler
import io.infinitic.common.proxies.RequestByWorkflowId
import io.infinitic.common.proxies.RequestByWorkflowTag
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskTag
import io.infinitic.common.workflows.Consumer0
import io.infinitic.common.workflows.Consumer1
import io.infinitic.common.workflows.Consumer2
import io.infinitic.common.workflows.Consumer3
import io.infinitic.common.workflows.Consumer4
import io.infinitic.common.workflows.Consumer5
import io.infinitic.common.workflows.Consumer6
import io.infinitic.common.workflows.Consumer7
import io.infinitic.common.workflows.Consumer8
import io.infinitic.common.workflows.Consumer9
import io.infinitic.common.workflows.WorkflowContext
import io.infinitic.common.workflows.WorkflowDispatcher
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.exceptions.clients.InvalidStubException
import io.infinitic.exceptions.workflows.MultipleGettersForSameChannelException
import io.infinitic.exceptions.workflows.NonIdempotentChannelGetterException
import java.lang.reflect.ParameterizedType
import java.time.Duration
import java.time.Instant
import org.jetbrains.annotations.TestOnly

@Suppress("unused")
abstract class Workflow {
  @Ignore
  lateinit var dispatcher: WorkflowDispatcher

  companion object {
    private val context: WorkflowContext
      get() = threadLocalWorkflowContext.get()

    @TestOnly
    @JvmStatic
    fun setContext(c: WorkflowContext) {
      threadLocalWorkflowContext.set(c)
    }

    @JvmStatic
    val workflowName get() = context.workflowName

    @JvmStatic
    val workflowId get() = context.workflowId

    @JvmStatic
    val methodName get() = context.methodName

    @JvmStatic
    val methodId get() = context.methodId

    @JvmStatic
    val tags get() = context.tags

    @JvmStatic
    val meta get() = context.meta
  }

  /** Create a stub for a task */
  @JvmOverloads
  protected fun <T : Any> newService(
    klass: Class<out T>,
    tags: Set<String>? = null,
    meta: Map<String, ByteArray>? = null
  ): T = NewServiceProxyHandler(
      klass = klass,
      taskTags = tags?.map { TaskTag(it) }?.toSet() ?: setOf(),
      taskMeta = TaskMeta(meta ?: mapOf()),
  ) { dispatcher }.stub()

  /** Create a stub for a workflow */
  @JvmOverloads
  protected fun <T> newWorkflow(
    klass: Class<out T>,
    tags: Set<String>? = null,
    meta: Map<String, ByteArray>? = null
  ): T = NewWorkflowProxyHandler(
      klass = klass,
      workflowTags = tags?.map { WorkflowTag(it) }?.toSet() ?: setOf(),
      workflowMeta = WorkflowMeta(meta ?: mapOf()),
  ) { dispatcher }.stub()

  /** Create a stub for an existing workflow targeted by id */
  protected fun <T : Any> getWorkflowById(klass: Class<out T>, id: String): T =
      ExistingWorkflowProxyHandler(klass, RequestByWorkflowId.by(id)) { dispatcher }.stub()

  /** Create a stub for existing workflow targeted by tag */
  protected fun <T : Any> getWorkflowByTag(klass: Class<out T>, tag: String): T =
      ExistingWorkflowProxyHandler(klass, RequestByWorkflowTag.by(tag)) { dispatcher }.stub()

  /** Dispatch without parameter a task or workflow returning an object */
  protected fun <R : Any?> dispatch(method: () -> R): Deferred<R> = start { method.invoke() }

  /** Dispatch with 1 parameter a task or workflow returning an object */
  protected fun <P1, R : Any?> dispatch(method: (p1: P1) -> R, p1: P1): Deferred<R> = start {
    method.invoke(p1)
  }

  /** Dispatch with 2 parameters a task or workflow returning an object */
  protected fun <P1, P2, R : Any?> dispatch(
    method: (p1: P1, p2: P2) -> R,
    p1: P1,
    p2: P2
  ): Deferred<R> = start { method.invoke(p1, p2) }

  /** Dispatch with 3 parameters a task or workflow returning an object */
  protected fun <P1, P2, P3, R : Any?> dispatch(
    method: (p1: P1, p2: P2, p3: P3) -> R,
    p1: P1,
    p2: P2,
    p3: P3
  ): Deferred<R> = start { method.invoke(p1, p2, p3) }

  /** Dispatch with 4 parameters a task or workflow returning an object */
  protected fun <P1, P2, P3, P4, R : Any?> dispatch(
    method: (p1: P1, p2: P2, p3: P3, p4: P4) -> R,
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4
  ): Deferred<R> = start { method.invoke(p1, p2, p3, p4) }

  /** Dispatch with 5 parameters a task or workflow returning an object */
  protected fun <P1, P2, P3, P4, P5, R : Any?> dispatch(
    method: (p1: P1, p2: P2, p3: P3, p4: P4, p5: P5) -> R,
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4,
    p5: P5
  ): Deferred<R> = start { method.invoke(p1, p2, p3, p4, p5) }

  /** Dispatch with 6 parameters a task or workflow returning an object */
  protected fun <P1, P2, P3, P4, P5, P6, R : Any?> dispatch(
    method: (p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6) -> R,
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4,
    p5: P5,
    p6: P6
  ): Deferred<R> = start { method.invoke(p1, p2, p3, p4, p5, p6) }

  /** Dispatch with 7 parameters a task or workflow returning an object */
  protected fun <P1, P2, P3, P4, P5, P6, P7, R : Any?> dispatch(
    method: (p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7) -> R,
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4,
    p5: P5,
    p6: P6,
    p7: P7
  ): Deferred<R> = start { method.invoke(p1, p2, p3, p4, p5, p6, p7) }

  /** Dispatch with 8 parameters a task or workflow returning an object */
  protected fun <P1, P2, P3, P4, P5, P6, P7, P8, R : Any?> dispatch(
    method: (p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8) -> R,
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4,
    p5: P5,
    p6: P6,
    p7: P7,
    p8: P8
  ): Deferred<R> = start { method.invoke(p1, p2, p3, p4, p5, p6, p7, p8) }

  /** Dispatch with 9 parameters a task or workflow returning an object */
  protected fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, R : Any?> dispatch(
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
  ): Deferred<R> = start { method.invoke(p1, p2, p3, p4, p5, p6, p7, p8, p9) }

  /** Dispatch without parameter a task or workflow returning void */
  protected fun dispatchVoid(method: Consumer0): Deferred<Void> = startVoid { method.apply() }

  /** Dispatch with 1 parameter a task or workflow returning void */
  protected fun <P1> dispatchVoid(method: Consumer1<P1>, p1: P1): Deferred<Void> = startVoid {
    method.apply(p1)
  }

  /** Dispatch with 2 parameters a task or workflow returning void */
  protected fun <P1, P2> dispatchVoid(method: Consumer2<P1, P2>, p1: P1, p2: P2): Deferred<Void> =
      startVoid {
        method.apply(p1, p2)
      }

  /** Dispatch with 3 parameters a task or workflow returning void */
  protected fun <P1, P2, P3> dispatchVoid(
    method: Consumer3<P1, P2, P3>,
    p1: P1,
    p2: P2,
    p3: P3
  ): Deferred<Void> = startVoid { method.apply(p1, p2, p3) }

  /** Dispatch with 4 parameters a task or workflow returning void */
  protected fun <P1, P2, P3, P4> dispatchVoid(
    method: Consumer4<P1, P2, P3, P4>,
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4
  ): Deferred<Void> = startVoid { method.apply(p1, p2, p3, p4) }

  /** Dispatch with 5 parameters a task or workflow returning void */
  protected fun <P1, P2, P3, P4, P5> dispatchVoid(
    method: Consumer5<P1, P2, P3, P4, P5>,
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4,
    p5: P5
  ): Deferred<Void> = startVoid { method.apply(p1, p2, p3, p4, p5) }

  /** Dispatch with 6 parameters a task or workflow returning void */
  protected fun <P1, P2, P3, P4, P5, P6> dispatchVoid(
    method: Consumer6<P1, P2, P3, P4, P5, P6>,
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4,
    p5: P5,
    p6: P6
  ): Deferred<Void> = startVoid { method.apply(p1, p2, p3, p4, p5, p6) }

  /** Dispatch with 7 parameters a task or workflow returning void */
  protected fun <P1, P2, P3, P4, P5, P6, P7> dispatchVoid(
    method: Consumer7<P1, P2, P3, P4, P5, P6, P7>,
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4,
    p5: P5,
    p6: P6,
    p7: P7
  ): Deferred<Void> = startVoid { method.apply(p1, p2, p3, p4, p5, p6, p7) }

  /** Dispatch with 8 parameters a task or workflow returning void */
  protected fun <P1, P2, P3, P4, P5, P6, P7, P8> dispatchVoid(
    method: Consumer8<P1, P2, P3, P4, P5, P6, P7, P8>,
    p1: P1,
    p2: P2,
    p3: P3,
    p4: P4,
    p5: P5,
    p6: P6,
    p7: P7,
    p8: P8
  ): Deferred<Void> = startVoid { method.apply(p1, p2, p3, p4, p5, p6, p7, p8) }

  /** Dispatch with 9 parameters a task or workflow returning void */
  protected fun <P1, P2, P3, P4, P5, P6, P7, P8, P9> dispatchVoid(
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
  ): Deferred<Void> = startVoid { method.apply(p1, p2, p3, p4, p5, p6, p7, p8, p9) }

  /** Create a channel */
  protected fun <T : Any> channel(): Channel<T> = Channel { dispatcher }

  /** Run inline task */
  protected fun <S> inline(task: () -> S): S = dispatcher.inline(task)

  /** Run inline task returning void */
  protected fun inlineVoid(task: Consumer0): Unit = dispatcher.inline { task.apply() }

  /** Create a timer from a duration */
  protected fun timer(duration: Duration): Deferred<Instant> = dispatcher.timer(duration)

  /** Create a timer from an instant */
  protected fun timer(instant: Instant): Deferred<Instant> = dispatcher.timer(instant)

  private fun <R> start(invoke: () -> R): Deferred<R> {
    val handler = ProxyHandler.async(invoke) ?: throw InvalidStubException()

    return dispatcher.dispatch(handler, false)
  }

  private fun startVoid(invoke: () -> Unit): Deferred<Void> {
    val handler = ProxyHandler.async(invoke) ?: throw InvalidStubException()

    return dispatcher.dispatch(handler, false)
  }
}

val threadLocalWorkflowContext: ThreadLocal<WorkflowContext> = ThreadLocal.withInitial { null }

/** Set the names of all channels in this workflow */
fun Workflow.setChannelNames() {
  this::class.java.declaredMethods
      .filter { it.returnType.name == Channel::class.java.name && it.parameterCount == 0 }
      .map {
        // channel must be created only once per method
        it.isAccessible = true
        val channel = it.invoke(this)
        // checking getter idempotency
        if (channel !== it.invoke(this)) {
          throw NonIdempotentChannelGetterException(this::class.java.name, it.name)
        }
        // this channel must not have a name already
        channel as Channel<*>
        if (channel.hasName()) {
          throw MultipleGettersForSameChannelException(this::class.java.name, it.name, channel.name)
        }
        // set channel name
        channel.setName(it.name)
      }
}

/** Set types of all channels in this workflow */
fun Workflow.setChannelTypes() {
  this::class.java.declaredFields
      .filter { it.type.name == Channel::class.java.name }
      .map {
        // get Channel object
        it.isAccessible = true
        val channel = it.get(this) as Channel<*>
        // set channel type
        channel.type = (it.genericType as ParameterizedType).actualTypeArguments[0]
      }
}
