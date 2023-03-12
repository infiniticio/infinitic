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
package io.infinitic.clients.dispatcher

import io.infinitic.clients.Deferred
import io.infinitic.clients.deferred.DeferredChannel
import io.infinitic.clients.deferred.DeferredMethod
import io.infinitic.clients.deferred.DeferredSend
import io.infinitic.clients.deferred.DeferredWorkflow
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.clients.messages.MethodCanceled
import io.infinitic.common.clients.messages.MethodCompleted
import io.infinitic.common.clients.messages.MethodFailed
import io.infinitic.common.clients.messages.MethodRunUnknown
import io.infinitic.common.clients.messages.WorkflowIdsByTag
import io.infinitic.common.clients.messages.interfaces.MethodMessage
import io.infinitic.common.data.ClientName
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.proxies.ChannelProxyHandler
import io.infinitic.common.proxies.ExistingServiceProxyHandler
import io.infinitic.common.proxies.ExistingWorkflowProxyHandler
import io.infinitic.common.proxies.NewServiceProxyHandler
import io.infinitic.common.proxies.NewWorkflowProxyHandler
import io.infinitic.common.proxies.ProxyHandler
import io.infinitic.common.proxies.RequestBy
import io.infinitic.common.proxies.RequestByWorkflowId
import io.infinitic.common.proxies.RequestByWorkflowTag
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.executors.errors.FailedWorkflowError
import io.infinitic.common.workflows.data.channels.SignalId
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.workflows.WorkflowCancellationReason
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.CompleteTimers
import io.infinitic.common.workflows.engine.messages.DispatchMethod
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.engine.messages.RetryTasks as RetryTaskInWorkflow
import io.infinitic.common.workflows.engine.messages.RetryWorkflowTask
import io.infinitic.common.workflows.engine.messages.SendSignal
import io.infinitic.common.workflows.engine.messages.WaitWorkflow
import io.infinitic.common.workflows.tags.SendToWorkflowTag
import io.infinitic.common.workflows.tags.messages.AddTagToWorkflow
import io.infinitic.common.workflows.tags.messages.CancelWorkflowByTag
import io.infinitic.common.workflows.tags.messages.CompleteTimersByTag
import io.infinitic.common.workflows.tags.messages.DispatchMethodByTag
import io.infinitic.common.workflows.tags.messages.DispatchWorkflowByCustomId
import io.infinitic.common.workflows.tags.messages.GetWorkflowIdsByTag
import io.infinitic.common.workflows.tags.messages.RetryTasksByTag as RetryTaskInWorkflowByTag
import io.infinitic.common.workflows.tags.messages.RetryWorkflowTaskByTag
import io.infinitic.common.workflows.tags.messages.SendSignalByTag
import io.infinitic.exceptions.CanceledWorkflowException
import io.infinitic.exceptions.FailedWorkflowException
import io.infinitic.exceptions.UnknownWorkflowException
import io.infinitic.exceptions.clients.InvalidChannelUsageException
import io.infinitic.exceptions.clients.MultipleCustomIdException
import io.infinitic.workflows.DeferredStatus
import io.infinitic.workflows.SendChannel
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.future.future
import kotlinx.coroutines.launch
import mu.KotlinLogging

internal class ClientDispatcherImpl(
    private val scope: CoroutineScope,
    val clientName: ClientName,
    val sendToWorkflowEngine: SendToWorkflowEngine,
    val sendToWorkflowTag: SendToWorkflowTag
) : ClientDispatcher {
  val logger = KotlinLogging.logger {}

  private val responseFlow = MutableSharedFlow<ClientMessage>(replay = 0)

  companion object {
    @JvmStatic private val localLastDeferred: ThreadLocal<Deferred<*>?> = ThreadLocal()
  }

  override suspend fun handle(message: ClientMessage) {
    responseFlow.emit(message)
  }

  override fun getLastDeferred(): Deferred<*>? = localLastDeferred.get()

  // asynchronous call: dispatch(stub::method)(*args)
  override fun <R : Any?> dispatchAsync(handler: ProxyHandler<*>): CompletableFuture<Deferred<R>> =
      when (handler) {
        is NewWorkflowProxyHandler -> dispatchWorkflowAsync(handler)
        is ExistingWorkflowProxyHandler -> dispatchMethodAsync(handler)
        is ChannelProxyHandler -> dispatchSignalAsync(handler)
        is NewServiceProxyHandler -> thisShouldNotHappen()
        is ExistingServiceProxyHandler -> thisShouldNotHappen()
      }

  // synchronous call: stub.method(*args)
  override fun <R : Any?> dispatchAndWait(handler: ProxyHandler<*>): R =
      when (handler) {
        is NewWorkflowProxyHandler -> dispatchWorkflowAndWait(handler)
        is ExistingWorkflowProxyHandler -> dispatchMethodAndWait(handler)
        is ChannelProxyHandler -> dispatchSignalAndWait(handler)
        is ExistingServiceProxyHandler -> thisShouldNotHappen()
        is NewServiceProxyHandler -> thisShouldNotHappen()
      }

  override fun <T> awaitWorkflow(
      returnClass: Class<T>,
      workflowName: WorkflowName,
      methodName: MethodName,
      workflowId: WorkflowId,
      methodRunId: MethodRunId?,
      clientWaiting: Boolean
  ): T {
    // wait for result
    val workflowResult =
        scope
            .future {
              val runId = methodRunId ?: MethodRunId.from(workflowId)
              // if task was not initially sync, then send WaitTask message
              if (clientWaiting) {
                val waitWorkflow =
                    WaitWorkflow(
                        workflowName = workflowName,
                        workflowId = workflowId,
                        methodRunId = runId,
                        emitterName = clientName)
                launch { sendToWorkflowEngine(waitWorkflow) }
              }

              responseFlow.first {
                logger.debug { "ResponseFlow: $it" }
                it is MethodMessage && it.workflowId == workflowId && it.methodRunId == runId
              }
            }
            .join()

    @Suppress("UNCHECKED_CAST")
    return when (workflowResult) {
      is MethodCompleted -> {
        workflowResult.methodReturnValue.value() as T
      }
      is MethodCanceled -> {
        throw CanceledWorkflowException(
            workflowName = workflowName.toString(),
            workflowId = workflowId.toString(),
            methodRunId = methodRunId?.toString())
      }
      is MethodFailed -> {
        throw FailedWorkflowException.from(
            FailedWorkflowError(
                workflowName = workflowName,
                methodName = methodName,
                workflowId = workflowId,
                methodRunId = methodRunId,
                deferredError = workflowResult.cause))
      }
      is MethodRunUnknown -> {
        throw UnknownWorkflowException(
            workflowName = workflowName.toString(),
            workflowId = workflowId.toString(),
            methodRunId = methodRunId?.toString())
      }
      else -> {
        thisShouldNotHappen("Unexpected ${workflowResult::class}")
      }
    }
  }

  override fun cancelWorkflowAsync(
      workflowName: WorkflowName,
      requestBy: RequestBy,
      methodRunId: MethodRunId?,
  ): CompletableFuture<Unit> =
      scope.future {
        when (requestBy) {
          is RequestByWorkflowId -> {
            val msg =
                CancelWorkflow(
                    workflowName = workflowName,
                    workflowId = requestBy.workflowId,
                    methodRunId = methodRunId,
                    reason = WorkflowCancellationReason.CANCELED_BY_CLIENT,
                    emitterName = clientName)
            sendToWorkflowEngine(msg)
          }
          is RequestByWorkflowTag -> {
            val msg =
                CancelWorkflowByTag(
                    workflowName = workflowName,
                    workflowTag = requestBy.workflowTag,
                    reason = WorkflowCancellationReason.CANCELED_BY_CLIENT,
                    emitterWorkflowId = null,
                    emitterName = clientName)
            sendToWorkflowTag(msg)
          }
          else -> thisShouldNotHappen()
        }
      }

  override fun retryWorkflowTaskAsync(
      workflowName: WorkflowName,
      requestBy: RequestBy
  ): CompletableFuture<Unit> =
      scope.future {
        when (requestBy) {
          is RequestByWorkflowId -> {
            val msg =
                RetryWorkflowTask(
                    workflowName = workflowName,
                    workflowId = requestBy.workflowId,
                    emitterName = clientName)
            sendToWorkflowEngine(msg)
          }
          is RequestByWorkflowTag -> {
            val msg =
                RetryWorkflowTaskByTag(
                    workflowName = workflowName,
                    workflowTag = requestBy.workflowTag,
                    emitterName = clientName)
            sendToWorkflowTag(msg)
          }
          else -> thisShouldNotHappen()
        }
      }

  override fun completeTimersAsync(
      workflowName: WorkflowName,
      requestBy: RequestBy,
      methodRunId: MethodRunId?
  ): CompletableFuture<Unit> =
      scope.future {
        when (requestBy) {
          is RequestByWorkflowId -> {
            val msg =
                CompleteTimers(
                    workflowName = workflowName,
                    workflowId = requestBy.workflowId,
                    emitterName = clientName,
                    methodRunId = methodRunId)
            sendToWorkflowEngine(msg)
          }
          is RequestByWorkflowTag -> {
            val msg =
                CompleteTimersByTag(
                    workflowName = workflowName,
                    workflowTag = requestBy.workflowTag,
                    emitterName = clientName,
                    methodRunId = methodRunId)
            sendToWorkflowTag(msg)
          }
          else -> thisShouldNotHappen()
        }
      }

  override fun retryTaskAsync(
      workflowName: WorkflowName,
      requestBy: RequestBy,
      taskId: TaskId?,
      taskStatus: DeferredStatus?,
      serviceName: ServiceName?
  ): CompletableFuture<Unit> =
      scope.future {
        when (requestBy) {
          is RequestByWorkflowId -> {
            val msg =
                RetryTaskInWorkflow(
                    workflowName = workflowName,
                    workflowId = requestBy.workflowId,
                    emitterName = clientName,
                    taskId = taskId,
                    taskStatus = taskStatus,
                    serviceName = serviceName)
            sendToWorkflowEngine(msg)
          }
          is RequestByWorkflowTag -> {
            val msg =
                RetryTaskInWorkflowByTag(
                    workflowName = workflowName,
                    workflowTag = requestBy.workflowTag,
                    emitterName = clientName,
                    taskId = taskId,
                    taskStatus = taskStatus,
                    serviceName = serviceName)
            sendToWorkflowTag(msg)
          }
          else -> thisShouldNotHappen()
        }
      }

  override fun getWorkflowIdsByTag(workflowName: WorkflowName, requestBy: RequestBy): Set<String> =
      when (requestBy) {
        is RequestByWorkflowId -> setOf(requestBy.workflowId.toString())
        is RequestByWorkflowTag -> {
          val workflowIdsByTag =
              scope
                  .future {
                    val msg =
                        GetWorkflowIdsByTag(
                            workflowName = workflowName,
                            workflowTag = requestBy.workflowTag,
                            emitterName = clientName)
                    launch { sendToWorkflowTag(msg) }

                    responseFlow.first {
                      (it is WorkflowIdsByTag) &&
                          (it.workflowName == workflowName) &&
                          (it.workflowTag == requestBy.workflowTag)
                    } as WorkflowIdsByTag
                  }
                  .join()

          workflowIdsByTag.workflowIds.map { it.toString() }.toSet()
        }
        else -> thisShouldNotHappen()
      }

  // asynchronous call: dispatch(stub::method)(*args)
  @Suppress("UNCHECKED_CAST")
  private fun <R : Any?> dispatchWorkflowAsync(
      handler: NewWorkflowProxyHandler<*>
  ): CompletableFuture<Deferred<R>> =
      when (handler.isChannelGetter()) {
        true -> throw InvalidChannelUsageException()
        false -> {
          val deferredWorkflow =
              deferredWorkflow(
                  handler.method.returnType as Class<R>, handler.workflowName, handler.methodName)

          dispatchWorkflowAsync(deferredWorkflow, false, handler)
        }
      }

  // synchronous call: stub.method(*args)
  @Suppress("UNCHECKED_CAST")
  private fun <R : Any?> dispatchWorkflowAndWait(handler: NewWorkflowProxyHandler<*>): R =
      when (handler.isChannelGetter()) {
        true -> throw InvalidChannelUsageException()
        false -> {
          val deferredWorkflow =
              deferredWorkflow(
                  handler.method.returnType as Class<R>, handler.workflowName, handler.methodName)

          dispatchWorkflowAsync(deferredWorkflow, true, handler)

          with(deferredWorkflow) {
            awaitWorkflow(returnClass, workflowName, methodName, workflowId, null, false)
          }
        }
      }

  private fun <R : Any?> deferredWorkflow(
      returnClass: Class<R>,
      workflowName: WorkflowName,
      methodName: MethodName
  ): DeferredWorkflow<R> {
    val workflowId = WorkflowId()

    val deferredWorkflow = DeferredWorkflow(returnClass, workflowName, methodName, workflowId, this)

    // store in ThreadLocal to be used in ::getDeferred
    localLastDeferred.set(deferredWorkflow)

    return deferredWorkflow
  }

  private fun <R : Any?> dispatchWorkflowAsync(
      deferred: DeferredWorkflow<R>,
      clientWaiting: Boolean,
      handler: NewWorkflowProxyHandler<*>
  ): CompletableFuture<Deferred<R>> {
    // it's important to build those objects out of the coroutine scope
    // otherwise the handler's value could be changed if reused

    val customIds = handler.workflowTags.filter { it.isCustomId() }

    return when (customIds.size) {
      // no customId tag provided
      0 -> {
        // provided tags
        val workflowTags =
            handler.workflowTags.map {
              AddTagToWorkflow(
                  workflowName = deferred.workflowName,
                  workflowTag = it,
                  workflowId = deferred.workflowId,
                  emitterName = clientName)
            }
        // dispatch workflow message
        val dispatchWorkflow =
            DispatchWorkflow(
                workflowName = deferred.workflowName,
                workflowId = deferred.workflowId,
                methodName = handler.methodName,
                methodParameters = handler.methodParameters,
                methodParameterTypes = handler.methodParameterTypes,
                workflowTags = handler.workflowTags,
                workflowMeta = handler.workflowMeta,
                parentWorkflowName = null,
                parentWorkflowId = null,
                parentMethodRunId = null,
                clientWaiting = clientWaiting,
                emitterName = clientName)

        scope.future {
          // first, we send all tags in parallel
          coroutineScope { workflowTags.forEach { launch { sendToWorkflowTag(it) } } }
          // only then dispatch workflow message
          // this to avoid a potential race condition
          // if the engine remove tags
          sendToWorkflowEngine(dispatchWorkflow)

          deferred
        }
      }
      // a customId tag was provided
      1 -> {
        // dispatch workflow message with customId tag
        val dispatchWorkflowByCustomId =
            DispatchWorkflowByCustomId(
                workflowName = deferred.workflowName,
                workflowTag = customIds.first(),
                workflowId = deferred.workflowId,
                methodName = deferred.methodName,
                methodParameters = handler.methodParameters,
                methodParameterTypes = handler.methodParameterTypes,
                workflowTags = handler.workflowTags,
                workflowMeta = handler.workflowMeta,
                parentWorkflowName = null,
                parentWorkflowId = null,
                parentMethodRunId = null,
                clientWaiting = clientWaiting,
                emitterName = clientName)

        scope.future {
          sendToWorkflowTag(dispatchWorkflowByCustomId)

          deferred
        }
      }
      // more than 1 custom tag were provided
      else -> {
        throw MultipleCustomIdException
      }
    }
  }

  // asynchronous call: dispatch(stub::method)(*args)
  @Suppress("UNCHECKED_CAST")
  private fun <R : Any?> dispatchMethodAsync(
      handler: ExistingWorkflowProxyHandler<*>
  ): CompletableFuture<Deferred<R>> =
      when (handler.isChannelGetter()) {
        true -> {
          // special case of getting a channel from a workflow
          val channel = ChannelProxyHandler<SendChannel<*>>(handler).stub()
          CompletableFuture.completedFuture(DeferredChannel(channel) as Deferred<R>)
        }
        false -> {
          val deferredMethod =
              deferredMethod(
                  handler.method.returnType as Class<R>,
                  handler.workflowName,
                  handler.methodName,
                  handler.requestBy)

          scope.future {
            dispatchMethod(deferredMethod, false, handler)
            deferredMethod
          }
        }
      }

  // synchronous call: stub.method(*args)
  @Suppress("UNCHECKED_CAST")
  private fun <R : Any?> dispatchMethodAndWait(handler: ExistingWorkflowProxyHandler<*>): R =
      when (handler.isChannelGetter()) {
        true -> {
          // special case of getting a channel from a workflow
          @Suppress("UNCHECKED_CAST")
          ChannelProxyHandler<SendChannel<*>>(handler).stub() as R
        }
        false -> {
          val deferredMethod =
              deferredMethod(
                  handler.method.returnType as Class<R>,
                  handler.workflowName,
                  handler.methodName,
                  handler.requestBy)

          scope.launch { dispatchMethod(deferredMethod, true, handler) }

          with(deferredMethod) {
            when (handler.requestBy) {
              is RequestByWorkflowId ->
                  awaitWorkflow(
                      returnClass,
                      workflowName,
                      methodName,
                      (handler.requestBy as RequestByWorkflowId).workflowId,
                      methodRunId,
                      false)
              is RequestByWorkflowTag ->
                  TODO("Not implemented as tag can target multiple workflows")
              else -> thisShouldNotHappen()
            }
          }
        }
      }

  private fun <R : Any?> deferredMethod(
      returnClass: Class<R>,
      workflowName: WorkflowName,
      methodName: MethodName,
      requestBy: RequestBy
  ): DeferredMethod<R> =
      DeferredMethod(returnClass, workflowName, methodName, requestBy, MethodRunId(), this)
          // store in ThreadLocal to be used in ::getDeferred
          .also { localLastDeferred.set(it) }

  private fun <R : Any?> dispatchMethod(
      deferred: DeferredMethod<R>,
      clientWaiting: Boolean,
      handler: ExistingWorkflowProxyHandler<*>
  ) =
      when {
        deferred.requestBy is RequestByWorkflowId -> {
          val dispatchMethod =
              DispatchMethod(
                  workflowName = deferred.workflowName,
                  workflowId = deferred.requestBy.workflowId,
                  methodRunId = deferred.methodRunId,
                  methodName = handler.methodName,
                  methodParameters = handler.methodParameters,
                  methodParameterTypes = handler.methodParameterTypes,
                  parentWorkflowId = null,
                  parentWorkflowName = null,
                  parentMethodRunId = null,
                  clientWaiting = clientWaiting,
                  emitterName = clientName)
          sendToWorkflowEngine(dispatchMethod)
        }
        deferred.requestBy is RequestByWorkflowTag -> {
          val dispatchMethodByTag =
              DispatchMethodByTag(
                  workflowName = deferred.workflowName,
                  workflowTag = deferred.requestBy.workflowTag,
                  parentWorkflowId = null,
                  parentWorkflowName = null,
                  parentMethodRunId = null,
                  methodRunId = deferred.methodRunId,
                  methodName = handler.methodName,
                  methodParameterTypes = handler.methodParameterTypes,
                  methodParameters = handler.methodParameters,
                  clientWaiting = clientWaiting,
                  emitterName = clientName)
          sendToWorkflowTag(dispatchMethodByTag)
        }
        else -> thisShouldNotHappen()
      }

  // asynchronous call: dispatch(stub.channel::send, signal)
  private fun <S : Any?> dispatchSignalAsync(
      handler: ChannelProxyHandler<*>
  ): CompletableFuture<Deferred<S>> {
    val deferredSend = deferredSend<S>()

    return scope.future {
      dispatchSignal(deferredSend, handler)
      deferredSend
    }
  }

  // synchronous call: stub.channel.send(signal)
  private fun <S : Any?> dispatchSignalAndWait(handler: ChannelProxyHandler<*>): S {
    val deferredSend = deferredSend<S>()

    // synchronous call
    dispatchSignal(deferredSend, handler)

    return deferredSend.await()
  }

  // asynchronous call: dispatch(stub.channel::send, signal)
  private fun dispatchSignal(deferredSend: DeferredSend<*>, handler: ChannelProxyHandler<*>) {
    if (handler.methodName.toString() != SendChannel<*>::send.name) thisShouldNotHappen()

    when {
      handler.requestBy is RequestByWorkflowId -> {
        val sendSignal =
            SendSignal(
                workflowName = handler.workflowName,
                workflowId = (handler.requestBy as RequestByWorkflowId).workflowId,
                channelName = handler.channelName,
                signalId = deferredSend.signalId,
                signalData = handler.signalData,
                channelTypes = handler.channelTypes,
                emitterName = clientName)
        sendToWorkflowEngine(sendSignal)
      }
      handler.requestBy is RequestByWorkflowTag -> {
        val sendSignalByTag =
            SendSignalByTag(
                workflowName = handler.workflowName,
                workflowTag = (handler.requestBy as RequestByWorkflowTag).workflowTag,
                channelName = handler.channelName,
                signalId = deferredSend.signalId,
                signalData = handler.signalData,
                channelTypes = handler.channelTypes,
                emitterWorkflowId = null,
                emitterName = clientName)
        sendToWorkflowTag(sendSignalByTag)
      }
      else -> thisShouldNotHappen()
    }
  }

  private fun <R : Any?> deferredSend(): DeferredSend<R> {
    val deferredSend = DeferredSend<R>(SignalId())

    // store in ThreadLocal to be used in ::getLastDeferred
    localLastDeferred.set(deferredSend)

    return deferredSend
  }
}
