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
import io.infinitic.common.data.MillisInstant
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
import io.infinitic.common.transport.InfiniticConsumer
import io.infinitic.common.transport.InfiniticProducer
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.future.future
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.annotations.TestOnly
import java.lang.reflect.Method
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import io.infinitic.common.workflows.engine.messages.RetryTasks as RetryTaskInWorkflow
import io.infinitic.common.workflows.tags.messages.RetryTasksByTag as RetryTaskInWorkflowByTag

internal class ClientDispatcher(
  private val clientScope: CoroutineScope,
  private val consumer: InfiniticConsumer,
  private val producer: InfiniticProducer
) : ProxyDispatcher {

  // Name of the client
  private val emitterName by lazy { EmitterName(producer.name) }

  // This as requester
  private val clientRequester by lazy { ClientRequester(clientName = ClientName.from(emitterName)) }

  // flag telling if the client consumer loop is initialized
  private val isClientConsumerInitialized: AtomicBoolean = AtomicBoolean(false)

  // Flow used to receive messages
  private val responseFlow = MutableSharedFlow<ClientMessage>(replay = 0)

  private suspend fun <T : Message> T.sendTo(topic: Topic<T>) = with(producer) { sendTo(topic) }

  private fun <T : Message> T.sendToAsync(topic: Topic<T>) = clientScope.future { sendTo(topic) }

  // a message received by the client is sent to responseFlow
  @Suppress("UNUSED_PARAMETER")
  internal suspend fun handle(message: ClientMessage, publishTime: MillisInstant) {
    responseFlow.emit(message)
  }

  // Utility to get access to last deferred
  internal fun getLastDeferred(): Deferred<*>? = localLastDeferred.get()

  // asynchronous call: dispatch(stub::method)(*args)
  fun <R : Any?> dispatchAsync(handler: ProxyHandler<*>): CompletableFuture<Deferred<R>> =
      when (handler) {
        is NewWorkflowProxyHandler -> handler.dispatchMethodAsync()
        is ExistingWorkflowProxyHandler -> handler.dispatchMethodAsync()
        is ChannelProxyHandler -> handler.dispatchSignalAsync()
        is NewServiceProxyHandler -> thisShouldNotHappen()
        is ExistingServiceProxyHandler -> thisShouldNotHappen()
      }

  // synchronous call: stub.method(*args)
  override fun <R : Any?> dispatchAndWait(handler: ProxyHandler<*>): R =
      when (handler) {
        is NewWorkflowProxyHandler -> handler.dispatchMethodAndWait()
        is ExistingWorkflowProxyHandler -> handler.dispatchMethodAndWait()
        is ChannelProxyHandler -> handler.dispatchSignalAndWait()
        is ExistingServiceProxyHandler -> thisShouldNotHappen()
        is NewServiceProxyHandler -> thisShouldNotHappen()
      }

  internal fun <T> awaitNewWorkflow(
    deferred: NewDeferredWorkflow<T>,
    clientWaiting: Boolean
  ): T = awaitWorkflow(
      deferred.workflowName,
      deferred.workflowId,
      deferred.method,
      null,
      deferred.methodTimeout,
      deferred.dispatchTime,
      clientWaiting,
  )

  internal fun <T> awaitExistingWorkflow(
    deferred: ExistingDeferredWorkflow<T>,
    clientWaiting: Boolean
  ): T = when (deferred.requestBy) {
    is RequestByWorkflowId -> awaitWorkflow(
        deferred.workflowName,
        deferred.requestBy.workflowId,
        deferred.method,
        deferred.workflowMethodId,
        deferred.methodTimeout,
        deferred.dispatchTime,
        clientWaiting,
    )

    is RequestByWorkflowTag -> TODO()
  }

  // wait for the completion of a method
  private fun <T> awaitWorkflow(
    workflowName: WorkflowName,
    workflowId: WorkflowId,
    workflowMethod: Method,
    workflowMethodId: WorkflowMethodId?,
    methodTimeout: MillisDuration?,
    dispatchTime: Long,
    clientWaiting: Boolean
  ): T {
    val methodId = workflowMethodId ?: WorkflowMethodId.from(workflowId)

    // calculate timeout from now
    val timeout = methodTimeout
        ?.let { it.millis - (System.currentTimeMillis() - dispatchTime) }
        ?.let { if (it < 0) 0 else it }
      ?: Long.MAX_VALUE

    // lazily starts client consumer if not already started and waits
    val waiting = waitForAsync(timeout) {
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
      waitWorkflow.sendToAsync(WorkflowStateCmdTopic)
    }

    // Get result
    val workflowResult = waiting.join()

    @Suppress("UNCHECKED_CAST")
    return when (workflowResult) {
      is MethodTimedOut, null -> {
        throw WorkflowTimedOutException(
            workflowName = workflowName.toString(),
            workflowId = workflowId.toString(),
            workflowMethodName = workflowMethod.name,
            workflowMethodId = workflowMethodId?.toString(),
        )
      }

      is MethodCompleted -> workflowMethod.decodeReturnValue(workflowResult.methodReturnValue) as T

      is MethodCanceled -> throw WorkflowCanceledException(
          workflowName = workflowName.toString(),
          workflowId = workflowId.toString(),
          workflowMethodName = workflowMethod.name,
          workflowMethodId = workflowResult.workflowMethodId.toString(),
      )

      is MethodFailed -> throw WorkflowFailedException.from(
          MethodFailedError(
              workflowName = workflowName,
              workflowMethodName = MethodName(workflowMethod.name),
              workflowId = workflowId,
              workflowMethodId = workflowResult.workflowMethodId,
              deferredError = workflowResult.cause,
          ),
      )

      is MethodUnknown -> throw WorkflowUnknownException(
          workflowName = workflowName.toString(),
          workflowId = workflowId.toString(),
          workflowMethodName = workflowMethod.name,
          workflowMethodId = workflowMethodId?.toString(),
      )

      else -> thisShouldNotHappen("Unexpected ${workflowResult::class}")
    }
  }

  fun cancelWorkflowAsync(
    workflowName: WorkflowName,
    requestBy: RequestBy,
    workflowMethodId: WorkflowMethodId?,
  ): CompletableFuture<Unit> = when (requestBy) {
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
      msg.sendToAsync(WorkflowStateCmdTopic)
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
      msg.sendToAsync(WorkflowTagEngineTopic)
    }

    else -> thisShouldNotHappen()
  }

  fun retryWorkflowTaskAsync(
    workflowName: WorkflowName,
    requestBy: RequestBy
  ): CompletableFuture<Unit> = when (requestBy) {
    is RequestByWorkflowId -> {
      val msg = RetryWorkflowTask(
          workflowName = workflowName,
          workflowId = requestBy.workflowId,
          requester = clientRequester,
          emitterName = emitterName,
          emittedAt = null,
      )
      msg.sendToAsync(WorkflowStateCmdTopic)
    }

    is RequestByWorkflowTag -> {
      val msg = RetryWorkflowTaskByTag(
          workflowName = workflowName,
          workflowTag = requestBy.workflowTag,
          requester = clientRequester,
          emitterName = emitterName,
          emittedAt = null,
      )
      msg.sendToAsync(WorkflowTagEngineTopic)
    }

    else -> thisShouldNotHappen()
  }

  fun completeTaskAsync(
    serviceName: ServiceName,
    taskId: TaskId,
    returnValue: MethodReturnValue
  ): CompletableFuture<Unit> {
    val msg = CompleteDelegatedTask(
        serviceName = serviceName,
        taskId = taskId,
        returnValue = returnValue,
        emitterName = emitterName,
    )
    return msg.sendToAsync(ServiceTagEngineTopic)
  }

  fun completeTimersAsync(
    workflowName: WorkflowName,
    requestBy: RequestBy,
    workflowMethodId: WorkflowMethodId?
  ): CompletableFuture<Unit> = when (requestBy) {
    is RequestByWorkflowId -> {
      val msg = CompleteTimers(
          workflowMethodId = workflowMethodId,
          workflowName = workflowName,
          workflowId = requestBy.workflowId,
          requester = clientRequester,
          emitterName = emitterName,
          emittedAt = null,
      )
      msg.sendToAsync(WorkflowStateCmdTopic)
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
      msg.sendToAsync(WorkflowTagEngineTopic)
    }

    else -> thisShouldNotHappen()
  }


  fun retryTaskAsync(
    workflowName: WorkflowName,
    requestBy: RequestBy,
    taskId: TaskId?,
    taskStatus: DeferredStatus?,
    serviceName: ServiceName?
  ): CompletableFuture<Unit> = when (requestBy) {
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
      msg.sendToAsync(WorkflowStateCmdTopic)
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
      msg.sendToAsync(WorkflowTagEngineTopic)
    }

    else -> thisShouldNotHappen()
  }


  fun getWorkflowIdsByTag(
    workflowName: WorkflowName,
    workflowTag: WorkflowTag
  ): Set<String> {
    // lazily starts client consumer if not already started and waits
    val waiting = waitForAsync {
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
    msg.sendToAsync(WorkflowTagEngineTopic).join()

    val workflowIdsByTag = waiting.join() as WorkflowIdsByTag

    return workflowIdsByTag.workflowIds.map { it.toString() }.toSet()
  }

  // asynchronous call: dispatch(stub::method)(*args)
  @Suppress("UNCHECKED_CAST")
  private fun <R : Any?> NewWorkflowProxyHandler<*>.dispatchMethodAsync(): CompletableFuture<Deferred<R>> =
      when (isChannelGetter()) {
        true -> throw InvalidChannelUsageException()
        false -> {
          val deferredWorkflow = newDeferredWorkflow(
              workflowName,
              method,
              method.returnType as Class<R>,
              getTimeout(),
          )
          dispatchMethodAsync(deferredWorkflow, false)
        }
      }

  // synchronous call: stub.method(*args)
  @Suppress("UNCHECKED_CAST")
  private fun <R : Any?> NewWorkflowProxyHandler<*>.dispatchMethodAndWait(): R =
      when (isChannelGetter()) {
        true -> throw InvalidChannelUsageException()
        false -> {
          val deferredWorkflow = newDeferredWorkflow(
              workflowName,
              method,
              method.returnType as Class<R>,
              getTimeout(),
          )
          dispatchMethodAsync(deferredWorkflow, true)

          awaitNewWorkflow(deferredWorkflow, false)
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

  private fun <R : Any?> NewWorkflowProxyHandler<*>.dispatchMethodAsync(
    deferred: NewDeferredWorkflow<R>,
    clientWaiting: Boolean,
  ): CompletableFuture<Deferred<R>> {
    // it's important to build those objects out of the coroutine scope
    // otherwise the handler's value could be changed if reused
    val customIds = workflowTags.filter { it.isCustomId() }

    return when (customIds.size) {
      // no customId tag provided
      0 -> {
        // first, we send all tags in parallel
        val futures = workflowTags.map {
          AddTagToWorkflow(
              workflowName = deferred.workflowName,
              workflowTag = it,
              workflowId = deferred.workflowId,
              emitterName = emitterName,
              emittedAt = null,
          ).sendToAsync(WorkflowTagEngineTopic)
        }
        CompletableFuture.allOf(*futures.toTypedArray()).join()

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
        dispatchWorkflow.sendToAsync(WorkflowStateCmdTopic).thenApply { deferred }
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
        dispatchWorkflowByCustomId.sendToAsync(WorkflowTagEngineTopic).thenApply { deferred }
      }
      // more than 1 customId tag were provided
      else -> throw MultipleCustomIdException
    }
  }

  // asynchronous call: dispatch(stub::method)(*args)
  @Suppress("UNCHECKED_CAST")
  private fun <R : Any?> ExistingWorkflowProxyHandler<*>.dispatchMethodAsync(): CompletableFuture<Deferred<R>> =
      when (isChannelGetter()) {
        true -> {
          // special case of getting a channel from a workflow
          val channel = ChannelProxyHandler<SendChannel<*>>(this).stub()
          CompletableFuture.completedFuture(DeferredChannel(channel) as Deferred<R>)
        }

        false -> {
          val deferred = existingDeferredWorkflow(
              workflowName,
              requestBy,
              method,
              method.returnType as Class<R>,
              getTimeout(),
          )
          dispatchMethodAsync(deferred, false).thenApply { deferred }
        }
      }

  // synchronous call: stub.method(*args)
  @Suppress("UNCHECKED_CAST")
  private fun <R : Any?> ExistingWorkflowProxyHandler<*>.dispatchMethodAndWait(): R =
      when (isChannelGetter()) {
        true -> {
          // special case of getting a channel from a workflow
          @Suppress("UNCHECKED_CAST")
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
          // synchronously sent the message to get errors
          dispatchMethodAsync(deferred, true).join()

          awaitExistingWorkflow(deferred, false)
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

  private fun <R : Any?> ExistingWorkflowProxyHandler<*>.dispatchMethodAsync(
    deferred: ExistingDeferredWorkflow<R>,
    clientWaiting: Boolean,
  ): CompletableFuture<Unit> = when (deferred.requestBy) {
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
      dispatchMethod.sendToAsync(WorkflowStateCmdTopic)
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
      dispatchMethodByTag.sendToAsync(WorkflowTagEngineTopic)
    }
  }

  // asynchronous call: dispatch(stub.channel::send, signal)
  private fun <S : Any?> ChannelProxyHandler<*>.dispatchSignalAsync(): CompletableFuture<Deferred<S>> {
    val deferredSend = deferredSend<S>()

    return dispatchSignalAsync(deferredSend).thenApply { deferredSend }
  }

  // synchronous call: stub.channel.send(signal)
  private fun <S : Any?> ChannelProxyHandler<*>.dispatchSignalAndWait(): S {
    val deferredSend = deferredSend<S>()

    // dispatch signal synchronously
    dispatchSignalAsync(deferredSend).join()

    return deferredSend.await()
  }

  private fun ChannelProxyHandler<*>.dispatchSignalAsync(
    deferredSend: DeferredSend<*>,
  ): CompletableFuture<Unit> {
    if (annotatedMethodName.toString() != SendChannel<*>::send.name) thisShouldNotHappen()

    return when (requestBy) {
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
        sendSignal.sendToAsync(WorkflowStateCmdTopic)
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
        sendSignalByTag.sendToAsync(WorkflowTagEngineTopic)
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


  private fun ProxyHandler<*>.getTimeout(): MillisDuration? =
      timeoutInMillisDuration.getOrElse {
        throw IllegalStateException("Unable to retrieve Timeout info when dispatching $method", it)
      }

  private fun waitForAsync(
    timeout: Long = Long.MAX_VALUE,
    predicate: suspend (ClientMessage) -> Boolean
  ): CompletableFuture<ClientMessage?> {
    // lazily starts client consumer if not already started
    if (isClientConsumerInitialized.compareAndSet(false, true)) {
      clientScope.future {
        consumer.start(
            subscription = MainSubscription(ClientTopic),
            entity = emitterName.toString(),
            handler = ::handle,
            beforeDlq = null,
            concurrency = 1,
        )
      }.join()
    }

    // wait for the first message that matches the predicate
    return clientScope.future {
      withTimeoutOrNull(timeout) {
        responseFlow.first { predicate(it) }
      }
    }
  }


  companion object {
    @TestOnly
    @JvmStatic
    private val localLastDeferred: ThreadLocal<Deferred<*>?> = ThreadLocal()
  }
}
