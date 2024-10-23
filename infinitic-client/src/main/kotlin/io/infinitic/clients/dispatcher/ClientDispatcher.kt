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

import io.github.oshai.kotlinlogging.KLogger
import io.infinitic.clients.Deferred
import io.infinitic.clients.InfiniticClient
import io.infinitic.clients.deferred.DeferredChannel
import io.infinitic.clients.deferred.DeferredSend
import io.infinitic.clients.deferred.ExistingDeferredWorkflow
import io.infinitic.clients.deferred.NewDeferredWorkflow
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.clients.messages.MethodCanceled
import io.infinitic.common.clients.messages.MethodCompleted
import io.infinitic.common.clients.messages.MethodFailed
import io.infinitic.common.clients.messages.MethodTimedOut
import io.infinitic.common.clients.messages.MethodUnknown
import io.infinitic.common.clients.messages.WorkflowIdsByTag
import io.infinitic.common.clients.messages.interfaces.MethodMessage
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodReturnValue
import io.infinitic.common.data.methods.decodeReturnValue
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.messages.Message
import io.infinitic.common.proxies.ChannelProxyHandler
import io.infinitic.common.proxies.ExistingServiceProxyHandler
import io.infinitic.common.proxies.ExistingWorkflowProxyHandler
import io.infinitic.common.proxies.NewServiceProxyHandler
import io.infinitic.common.proxies.NewWorkflowProxyHandler
import io.infinitic.common.proxies.ProxyDispatcher
import io.infinitic.common.proxies.ProxyHandler
import io.infinitic.common.proxies.RequestBy
import io.infinitic.common.proxies.RequestByWorkflowId
import io.infinitic.common.proxies.RequestByWorkflowTag
import io.infinitic.common.requester.ClientRequester
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.executors.errors.MethodFailedError
import io.infinitic.common.tasks.tags.messages.CompleteDelegatedTask
import io.infinitic.common.transport.ClientTopic
import io.infinitic.common.transport.interfaces.InfiniticConsumer
import io.infinitic.common.transport.interfaces.InfiniticProducer
import io.infinitic.common.transport.MainSubscription
import io.infinitic.common.transport.ServiceTagEngineTopic
import io.infinitic.common.transport.Topic
import io.infinitic.common.transport.WorkflowStateCmdTopic
import io.infinitic.common.transport.WorkflowTagEngineTopic
import io.infinitic.common.workflows.data.channels.SignalId
import io.infinitic.common.workflows.data.workflowMethods.WorkflowMethodId
import io.infinitic.common.workflows.data.workflows.WorkflowCancellationReason
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.CompleteTimers
import io.infinitic.common.workflows.engine.messages.DispatchMethod
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.engine.messages.RetryWorkflowTask
import io.infinitic.common.workflows.engine.messages.SendSignal
import io.infinitic.common.workflows.engine.messages.WaitWorkflow
import io.infinitic.common.workflows.tags.messages.AddTagToWorkflow
import io.infinitic.common.workflows.tags.messages.CancelWorkflowByTag
import io.infinitic.common.workflows.tags.messages.CompleteTimersByTag
import io.infinitic.common.workflows.tags.messages.DispatchMethodByTag
import io.infinitic.common.workflows.tags.messages.DispatchWorkflowByCustomId
import io.infinitic.common.workflows.tags.messages.GetWorkflowIdsByTag
import io.infinitic.common.workflows.tags.messages.RetryWorkflowTaskByTag
import io.infinitic.common.workflows.tags.messages.SendSignalByTag
import io.infinitic.exceptions.WorkflowCanceledException
import io.infinitic.exceptions.WorkflowFailedException
import io.infinitic.exceptions.WorkflowTimedOutException
import io.infinitic.exceptions.WorkflowUnknownException
import io.infinitic.exceptions.clients.InvalidChannelUsageException
import io.infinitic.exceptions.clients.MultipleCustomIdException
import io.infinitic.workflows.DeferredStatus
import io.infinitic.workflows.SendChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.annotations.TestOnly
import java.lang.reflect.Method
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import io.infinitic.common.workflows.engine.messages.RetryTasks as RetryTaskInWorkflow
import io.infinitic.common.workflows.tags.messages.RetryTasksByTag as RetryTaskInWorkflowByTag
import kotlinx.coroutines.Deferred as CoroutineDeferred

internal class ClientDispatcher(
  private val clientScope: CoroutineScope,
  private val consumer: InfiniticConsumer,
  private val producer: InfiniticProducer,
) : ProxyDispatcher {

  // Name of the client
  private val emitterName by lazy { runBlocking { EmitterName(producer.getName()) } }

  // This as requester
  private val clientRequester by lazy { ClientRequester(clientName = ClientName.from(emitterName)) }

  // flag telling if the client consumer loop is initialized
  private val hasClientConsumerStarted: AtomicBoolean = AtomicBoolean(false)

  /**
   * A mutable shared flow that buffers the latest `Result` of `ClientMessage`.
   * This flow is used to publish and observe the outcome of client messages received by the dispatcher.
   *
   * The flow maintains a replay cache of 1 element, ensuring that the most recent value
   * is always available to new subscribers immediately upon subscription,
   * in particular the last exception if the consuming process failed
   */
  internal val responseFlow = ResponseFlow<ClientMessage>()

  private suspend fun <T : Message> T.sendTo(topic: Topic<T>) = with(producer) {
    sendTo(topic)
  }

  // Utility to get access to last deferred
  internal fun getLastDeferred(): Deferred<*>? = localLastDeferred.get()

  // asynchronous call: dispatch(stub::method)(*args)
  fun <R : Any?> dispatchAsync(handler: ProxyHandler<*>): CompletableFuture<Deferred<R>> =
      runAsync {
        when (handler) {
          is NewWorkflowProxyHandler -> handler.dispatchMethod()
          is ExistingWorkflowProxyHandler -> handler.dispatchMethod()
          is ChannelProxyHandler -> handler.dispatchSignal()
          is NewServiceProxyHandler -> thisShouldNotHappen()
          is ExistingServiceProxyHandler -> thisShouldNotHappen()
        }
      }

  // synchronous call: stub.method(*args)
  override fun <R : Any?> dispatchAndWait(handler: ProxyHandler<*>): R = runBlocking {
    when (handler) {
      is NewWorkflowProxyHandler -> handler.dispatchMethodAndWait()
      is ExistingWorkflowProxyHandler -> handler.dispatchMethodAndWait()
      is ChannelProxyHandler -> handler.dispatchSignal<R>().await()
      is ExistingServiceProxyHandler -> thisShouldNotHappen()
      is NewServiceProxyHandler -> thisShouldNotHappen()
    }
  }

  private suspend fun <T> awaitNewWorkflowAsync(
    deferred: NewDeferredWorkflow<T>,
    clientWaiting: Boolean
  ): CoroutineDeferred<ClientMessage?> = awaitWorkflowAsync(
      deferred.workflowName,
      deferred.workflowId,
      null,
      deferred.methodTimeout,
      deferred.dispatchTime,
      clientWaiting,
  )

  internal suspend fun <T> awaitNewWorkflow(
    deferred: NewDeferredWorkflow<T>,
    clientWaiting: Boolean
  ): T = awaitNewWorkflowAsync(deferred, clientWaiting).await().getValue(
      deferred.workflowName,
      deferred.workflowId,
      deferred.method,
      WorkflowMethodId.from(deferred.workflowId),
  )

  private suspend fun <T> awaitExistingWorkflowAsync(
    deferred: ExistingDeferredWorkflow<T>,
    clientWaiting: Boolean
  ): CoroutineDeferred<ClientMessage?> = when (deferred.requestBy) {
    is RequestByWorkflowId -> awaitWorkflowAsync(
        deferred.workflowName,
        deferred.requestBy.workflowId,
        deferred.workflowMethodId,
        deferred.methodTimeout,
        deferred.dispatchTime,
        clientWaiting,
    )

    is RequestByWorkflowTag -> TODO()
  }

  internal suspend fun <T> awaitExistingWorkflow(
    deferred: ExistingDeferredWorkflow<T>,
    clientWaiting: Boolean
  ): T = when (deferred.requestBy) {
    is RequestByWorkflowId -> awaitWorkflowAsync(
        deferred.workflowName,
        deferred.requestBy.workflowId,
        deferred.workflowMethodId,
        deferred.methodTimeout,
        deferred.dispatchTime,
        clientWaiting,
    ).await().getValue(
        deferred.workflowName,
        deferred.requestBy.workflowId,
        deferred.method,
        deferred.workflowMethodId,
    ) as T

    is RequestByWorkflowTag -> TODO()
  }

  // wait for the completion of a method
  private suspend fun awaitWorkflowAsync(
    workflowName: WorkflowName,
    workflowId: WorkflowId,
    workflowMethodId: WorkflowMethodId?,
    methodTimeout: MillisDuration?,
    dispatchTime: Long,
    clientWaiting: Boolean
  ): CoroutineDeferred<ClientMessage?> {
    val methodId = workflowMethodId ?: WorkflowMethodId.from(workflowId)

    // calculate timeout from now
    val timeout = methodTimeout
        ?.let { it.millis - (System.currentTimeMillis() - dispatchTime) }
        ?.let { if (it < 0) 0 else it }
      ?: Long.MAX_VALUE

    // lazily starts listener
    val waiting = awaitAsync(timeout) {
      it is MethodMessage && it.workflowId == workflowId && it.workflowMethodId == methodId
    }

    // if task was not initially sync, then send WaitTask message
    if (clientWaiting) {
      val waitWorkflow = WaitWorkflow(
          workflowMethodId = methodId,
          workflowName = workflowName,
          workflowId = workflowId,
          requester = clientRequester,
          emitterName = emitterName,
          emittedAt = null,
      )
      // synchronously sent the message to get errors
      waitWorkflow.sendTo(WorkflowStateCmdTopic)
    }

    // Get result
    return waiting
  }

  fun cancelWorkflowAsync(
    workflowName: WorkflowName,
    requestBy: RequestBy,
    workflowMethodId: WorkflowMethodId?,
  ): CompletableFuture<Unit> = runAsync {
    when (requestBy) {
      is RequestByWorkflowId -> {
        val msg = CancelWorkflow(
            cancellationReason = WorkflowCancellationReason.CANCELED_BY_CLIENT,
            workflowMethodId = workflowMethodId,
            workflowName = workflowName,
            workflowId = requestBy.workflowId,
            requester = clientRequester,
            emitterName = emitterName,
            emittedAt = null,
        )
        msg.sendTo(WorkflowStateCmdTopic)
      }

      is RequestByWorkflowTag -> {
        val msg = CancelWorkflowByTag(
            workflowName = workflowName,
            workflowTag = requestBy.workflowTag,
            reason = WorkflowCancellationReason.CANCELED_BY_CLIENT,
            requester = clientRequester,
            emitterName = emitterName,
            emittedAt = null,
        )
        msg.sendTo(WorkflowTagEngineTopic)
      }

      else -> thisShouldNotHappen()
    }
  }

  fun retryWorkflowTaskAsync(
    workflowName: WorkflowName,
    requestBy: RequestBy
  ): CompletableFuture<Unit> = runAsync {
    when (requestBy) {
      is RequestByWorkflowId -> {
        val msg = RetryWorkflowTask(
            workflowName = workflowName,
            workflowId = requestBy.workflowId,
            requester = clientRequester,
            emitterName = emitterName,
            emittedAt = null,
        )
        msg.sendTo(WorkflowStateCmdTopic)
      }

      is RequestByWorkflowTag -> {
        val msg = RetryWorkflowTaskByTag(
            workflowName = workflowName,
            workflowTag = requestBy.workflowTag,
            requester = clientRequester,
            emitterName = emitterName,
            emittedAt = null,
        )
        msg.sendTo(WorkflowTagEngineTopic)
      }

      else -> thisShouldNotHappen()
    }
  }

  fun completeTaskAsync(
    serviceName: ServiceName,
    taskId: TaskId,
    returnValue: MethodReturnValue
  ): CompletableFuture<Unit> = runAsync {
    val msg = CompleteDelegatedTask(
        serviceName = serviceName,
        taskId = taskId,
        returnValue = returnValue,
        emitterName = emitterName,
    )
    msg.sendTo(ServiceTagEngineTopic)
  }

  fun completeTimersAsync(
    workflowName: WorkflowName,
    requestBy: RequestBy,
    workflowMethodId: WorkflowMethodId?
  ): CompletableFuture<Unit> = runAsync {
    when (requestBy) {
      is RequestByWorkflowId -> {
        val msg = CompleteTimers(
            workflowMethodId = workflowMethodId,
            workflowName = workflowName,
            workflowId = requestBy.workflowId,
            requester = clientRequester,
            emitterName = emitterName,
            emittedAt = null,
        )
        msg.sendTo(WorkflowStateCmdTopic)
      }

      is RequestByWorkflowTag -> {
        val msg = CompleteTimersByTag(
            workflowName = workflowName,
            workflowTag = requestBy.workflowTag,
            workflowMethodId = workflowMethodId,
            requester = clientRequester,
            emitterName = emitterName,
            emittedAt = null,
        )
        msg.sendTo(WorkflowTagEngineTopic)
      }

      else -> thisShouldNotHappen()
    }
  }

  fun retryTaskAsync(
    workflowName: WorkflowName,
    requestBy: RequestBy,
    taskId: TaskId?,
    taskStatus: DeferredStatus?,
    serviceName: ServiceName?
  ): CompletableFuture<Unit> = runAsync {
    when (requestBy) {
      is RequestByWorkflowId -> {
        val msg = RetryTaskInWorkflow(
            workflowName = workflowName,
            workflowId = requestBy.workflowId,
            emitterName = emitterName,
            taskId = taskId,
            taskStatus = taskStatus,
            serviceName = serviceName,
            requester = clientRequester,
            emittedAt = null,
        )
        msg.sendTo(WorkflowStateCmdTopic)
      }

      is RequestByWorkflowTag -> {
        val msg = RetryTaskInWorkflowByTag(
            workflowName = workflowName,
            workflowTag = requestBy.workflowTag,
            taskId = taskId,
            taskStatus = taskStatus,
            serviceName = serviceName,
            requester = clientRequester,
            emitterName = emitterName,
            emittedAt = null,
        )
        msg.sendTo(WorkflowTagEngineTopic)
      }

      else -> thisShouldNotHappen()
    }
  }

  suspend fun getWorkflowIdsByTag(
    workflowName: WorkflowName,
    workflowTag: WorkflowTag
  ): Set<String> {
    // lazily starts client consumer if not already started and waits
    val waiting = awaitAsync {
      (it is WorkflowIdsByTag) &&
          (it.workflowName == workflowName) &&
          (it.workflowTag == workflowTag)
    }

    val msg = GetWorkflowIdsByTag(
        workflowName = workflowName,
        workflowTag = workflowTag,
        emitterName = emitterName,
        emittedAt = null,
    )

    // synchronously sent the message to get errors
    msg.sendTo(WorkflowTagEngineTopic)

    val workflowIdsByTag = waiting.await() as WorkflowIdsByTag

    return workflowIdsByTag.workflowIds.map { it.toString() }.toSet()
  }

  private suspend fun <R : Any?> NewWorkflowProxyHandler<*>.dispatchMethod(): Deferred<R> =
      when (isChannelGetter()) {
        true -> throw InvalidChannelUsageException()
        false -> {
          @Suppress("UNCHECKED_CAST")
          newDeferredWorkflow(
              workflowName,
              method,
              method.returnType as Class<R>,
              getTimeout(),
          ).also {
            dispatchMethod(it, false)
          }
        }
      }

  // synchronous call: stub.method(*args)
  private suspend fun <R : Any?> NewWorkflowProxyHandler<*>.dispatchMethodAndWait(): R =
      when (isChannelGetter()) {
        true -> throw InvalidChannelUsageException()
        false -> {
          @Suppress("UNCHECKED_CAST")
          val deferredWorkflow = newDeferredWorkflow(
              workflowName,
              method,
              method.returnType as Class<R>,
              getTimeout(),
          )

          val future = awaitNewWorkflowAsync(deferredWorkflow, false)

          // synchronously send the message to get errors
          dispatchMethod(deferredWorkflow, true)

          future.await().getValue(
              workflowName,
              deferredWorkflow.workflowId,
              deferredWorkflow.method,
              WorkflowMethodId.from(deferredWorkflow.workflowId),
          ) as R
        }
      }

  private fun <R : Any?> newDeferredWorkflow(
    workflowName: WorkflowName,
    method: Method,
    methodReturnClass: Class<R>,
    methodTimeout: MillisDuration?
  ) = NewDeferredWorkflow(workflowName, method, methodReturnClass, methodTimeout, this)
      // store in ThreadLocal to be used in ::getDeferred
      .also { localLastDeferred.set(it) }

  private suspend fun <R : Any?> NewWorkflowProxyHandler<*>.dispatchMethod(
    deferred: NewDeferredWorkflow<R>,
    clientWaiting: Boolean,
  ) {
    // it's important to build those objects out of the coroutine scope
    // otherwise the handler's value could be changed if reused
    val customIds = workflowTags.filter { it.isCustomId() }

    return when (customIds.size) {
      // no customId tag provided
      0 -> {
        // first, we send all tags in parallel
        coroutineScope {
          workflowTags.map {
            launch {
              AddTagToWorkflow(
                  workflowName = deferred.workflowName,
                  workflowTag = it,
                  workflowId = deferred.workflowId,
                  emitterName = emitterName,
                  emittedAt = null,
              ).sendTo(WorkflowTagEngineTopic)
            }
          }
        }
        // dispatch workflow message
        val dispatchWorkflow = DispatchWorkflow(
            workflowName = deferred.workflowName,
            workflowId = deferred.workflowId,
            methodName = annotatedMethodName,
            methodParameters = methodParameters,
            methodParameterTypes = methodParameterTypes,
            workflowTags = workflowTags,
            workflowMeta = workflowMeta,
            requester = clientRequester,
            clientWaiting = clientWaiting,
            emitterName = emitterName,
            emittedAt = null,
        )
        // workflow message is dispatched after tags
        // to avoid a potential race condition if the engine remove tags
        dispatchWorkflow.sendTo(WorkflowStateCmdTopic)
      }
      // a customId tag was provided
      1 -> {
        // dispatch workflow message with customId tag
        val dispatchWorkflowByCustomId = DispatchWorkflowByCustomId(
            workflowName = deferred.workflowName,
            workflowTag = customIds.first(),
            workflowId = deferred.workflowId,
            methodName = MethodName(deferred.method.name),
            methodParameters = methodParameters,
            methodParameterTypes = methodParameterTypes,
            methodTimeout = deferred.methodTimeout,
            workflowTags = workflowTags,
            workflowMeta = workflowMeta,
            requester = clientRequester,
            clientWaiting = clientWaiting,
            emitterName = emitterName,
            emittedAt = null,
        )
        dispatchWorkflowByCustomId.sendTo(WorkflowTagEngineTopic)
      }
      // more than 1 customId tag were provided
      else -> throw MultipleCustomIdException
    }
  }


  // asynchronous call: dispatch(stub::method)(*args)
  @Suppress("UNCHECKED_CAST")
  private suspend fun <R : Any?> ExistingWorkflowProxyHandler<*>.dispatchMethod(): Deferred<R> =
      when (isChannelGetter()) {
        true -> {
          // special case of getting a channel from a workflow
          val channel = ChannelProxyHandler<SendChannel<*>>(this).stub()
          DeferredChannel(channel) as Deferred<R>
        }

        false -> {
          existingDeferredWorkflow(
              workflowName,
              requestBy,
              method,
              method.returnType as Class<R>,
              getTimeout(),
          ).also {
            dispatchMethod(it, false)
          }
        }
      }

  // synchronous call: stub.method(*args)
  @Suppress("UNCHECKED_CAST")
  private suspend fun <R : Any?> ExistingWorkflowProxyHandler<*>.dispatchMethodAndWait(): R =
      when (isChannelGetter()) {
        true -> {
          // special case of getting a channel from a workflow
          ChannelProxyHandler<SendChannel<*>>(this).stub() as R
        }

        false -> {
          val deferred = existingDeferredWorkflow(
              workflowName,
              requestBy,
              method,
              method.returnType as Class<R>,
              getTimeout(),
          )

          val future = awaitExistingWorkflowAsync(deferred, false)

          // send the message synchronously to get errors
          dispatchMethod(deferred, true)

          future.await().getValue(
              deferred.workflowName,
              deferred.requestBy.workflowId!!,
              deferred.method,
              deferred.workflowMethodId,
          ) as R
        }
      }

  private fun <R : Any?> existingDeferredWorkflow(
    workflowName: WorkflowName,
    requestBy: RequestBy,
    method: Method,
    methodReturnClass: Class<R>,
    methodTimeout: MillisDuration?
  ) = ExistingDeferredWorkflow(
      workflowName,
      requestBy,
      method,
      methodReturnClass,
      methodTimeout,
      this,
  ) // store in ThreadLocal to be used in ::getDeferred
      .also { localLastDeferred.set(it) }

  private suspend fun <R : Any?> ExistingWorkflowProxyHandler<*>.dispatchMethod(
    deferred: ExistingDeferredWorkflow<R>,
    clientWaiting: Boolean,
  ) = when (deferred.requestBy) {
    is RequestByWorkflowId -> {
      val dispatchMethod = DispatchMethod(
          workflowName = deferred.workflowName,
          workflowId = deferred.requestBy.workflowId,
          workflowMethodId = deferred.workflowMethodId,
          workflowMethodName = annotatedMethodName,
          methodParameters = methodParameters,
          methodParameterTypes = methodParameterTypes,
          requester = clientRequester,
          clientWaiting = clientWaiting,
          emitterName = emitterName,
          emittedAt = null,
      )
      dispatchMethod.sendTo(WorkflowStateCmdTopic)
    }

    is RequestByWorkflowTag -> {
      val dispatchMethodByTag = DispatchMethodByTag(
          workflowName = deferred.workflowName,
          workflowTag = deferred.requestBy.workflowTag,
          workflowMethodId = deferred.workflowMethodId,
          methodName = annotatedMethodName,
          methodParameterTypes = methodParameterTypes,
          methodParameters = methodParameters,
          methodTimeout = deferred.methodTimeout,
          requester = clientRequester,
          clientWaiting = clientWaiting,
          emitterName = emitterName,
          emittedAt = null,
      )
      dispatchMethodByTag.sendTo(WorkflowTagEngineTopic)
    }
  }

  // asynchronous call: dispatch(stub.channel::send, signal)
  private suspend fun <S : Any?> ChannelProxyHandler<*>.dispatchSignal() =
      deferredSend<S>().also { dispatchSignal(it) }

  private suspend fun ChannelProxyHandler<*>.dispatchSignal(
    deferredSend: DeferredSend<*>,
  ) {
    if (annotatedMethodName.toString() != SendChannel<*>::send.name) thisShouldNotHappen()

    when (requestBy) {
      is RequestByWorkflowId -> {
        val sendSignal = SendSignal(
            channelName = channelName,
            signalId = deferredSend.signalId,
            signalData = signalData,
            channelTypes = channelTypes,
            workflowName = workflowName,
            workflowId = (requestBy as RequestByWorkflowId).workflowId,
            emitterName = emitterName,
            emittedAt = null,
            requester = clientRequester,
        )
        sendSignal.sendTo(WorkflowStateCmdTopic)
      }

      is RequestByWorkflowTag -> {
        val sendSignalByTag = SendSignalByTag(
            workflowName = workflowName,
            workflowTag = (requestBy as RequestByWorkflowTag).workflowTag,
            channelName = channelName,
            signalId = deferredSend.signalId,
            signalData = signalData,
            channelTypes = channelTypes,
            parentWorkflowId = null,
            emitterName = emitterName,
            emittedAt = null,
            requester = clientRequester,
        )
        sendSignalByTag.sendTo(WorkflowTagEngineTopic)
      }

      else -> thisShouldNotHappen()
    }
  }

  private fun <R : Any?> deferredSend(): DeferredSend<R> =
      DeferredSend<R>(SignalId()).also {
        // store in ThreadLocal to be used in ::getLastDeferred
        localLastDeferred.set(it)
      }

  private fun ProxyHandler<*>.getTimeout(): MillisDuration? =
      timeoutInMillisDuration.getOrElse {
        throw IllegalStateException("Unable to retrieve Timeout info when dispatching $method", it)
      }

  @Suppress("UNCHECKED_CAST")
  private fun <T> ClientMessage?.getValue(
    workflowName: WorkflowName,
    workflowId: WorkflowId,
    workflowMethod: Method,
    workflowMethodId: WorkflowMethodId?,
  ) = when (this) {
    is MethodCompleted -> workflowMethod.decodeReturnValue(methodReturnValue) as T

    is MethodTimedOut, null -> {
      throw WorkflowTimedOutException(
          workflowName = workflowName.toString(),
          workflowId = workflowId.toString(),
          workflowMethodName = workflowMethod.name,
          workflowMethodId = workflowMethodId?.toString(),
      )
    }

    is MethodCanceled -> throw WorkflowCanceledException(
        workflowName = workflowName.toString(),
        workflowId = workflowId.toString(),
        workflowMethodName = workflowMethod.name,
        workflowMethodId = workflowMethodId.toString(),
    )

    is MethodFailed -> throw WorkflowFailedException.from(
        MethodFailedError(
            workflowName = workflowName,
            workflowMethodName = MethodName(workflowMethod.name),
            workflowId = workflowId,
            workflowMethodId = workflowMethodId,
            deferredError = cause,
        ),
    )

    is MethodUnknown -> throw WorkflowUnknownException(
        workflowName = workflowName.toString(),
        workflowId = workflowId.toString(),
        workflowMethodName = workflowMethod.name,
        workflowMethodId = workflowMethodId?.toString(),
    )

    else -> thisShouldNotHappen("Unexpected $this")
  }


  /**
   * Starts listening asynchronously for incoming messages for a specific client.
   *
   * This method ensures that a consumer client, identified by `emitterName`, is created and started
   * only once. It launches a coroutine to handle the listening process and ensures that messages
   * are emitted to the response flow or any errors are propagated appropriately.
   *
   * This function must be called with an active CoroutineScope and KLogger context.
   *
   * The function performs the following tasks:
   * - It checks if the consumer client has already started using `hasClientConsumerStarted`.
   * - Logs the starting message for the client using `KLogger`.
   * - Invokes `consumer.startAsync` to start listening to the specified subscription synchronously.
   * - Launches a coroutine to handle the asynchronous listening process, which waits for messages
   *   and emits them to `responseFlow`.
   * - Catches any exceptions during message processing and emits them to the `responseFlow` before rethrowing.
   *
   * This function suspends until the consumer client is successfully started and the listening coroutine is launched.
   */
  context(CoroutineScope, KLogger)
  private suspend fun startListeningAsync() {
    if (hasClientConsumerStarted.compareAndSet(false, true)) {
      info { "Starting consumer client for client $emitterName" }
      // synchronously make sure that the consumer is created and started
      val listenerJob =
          consumer.startAsync(
              subscription = MainSubscription(ClientTopic),
              entity = emitterName.toString(),
              concurrency = 1,
              process = { message, _ -> responseFlow.emit(message) },
          )
      // asynchronously listen
      launch {
        try {
          listenerJob.join()
        } catch (e: Exception) {
          // all subsequent calls to await will fail and trigger this exception
          responseFlow.emitThrowable(e)
          throw e
        }
      }
    }
  }

  /**
   * Awaits until a `ClientMessage` that matches the given predicate is received
   * or the timeout is reached, in this case response will be null
   * Ensures that the client is listening for messages before awaiting.
   *
   * @param timeout The maximum time to wait for the message. Defaults to `Long.MAX_VALUE`.
   * @param predicate A suspend function that determines if a received `ClientMessage` matches the criteria.
   * @return A `CoroutineDeferred` that will provide the matching `ClientMessage` or `null` if the timeout is reached.
   */
  private suspend fun awaitAsync(
    timeout: Long = Long.MAX_VALUE,
    predicate: suspend (ClientMessage) -> Boolean
  ): CoroutineDeferred<ClientMessage?> = with(clientScope) {
    // make sure the client is listening
    with(logger) { startListeningAsync() }

    return async { responseFlow.first(timeout) { predicate(it) } }
  }

  private fun <S> runAsync(block: suspend () -> S): CompletableFuture<S> =
      CompletableFuture<S>().also {
        clientScope.launch {
          try {
            it.complete(block())
          } catch (e: CancellationException) {
            it.cancel(false)
          } catch (e: Throwable) {
            it.completeExceptionally(e)
          }
        }
      }

  companion object {
    @TestOnly
    @JvmStatic
    private val localLastDeferred: ThreadLocal<Deferred<*>?> = ThreadLocal()

    internal val logger = InfiniticClient.logger
  }
}
