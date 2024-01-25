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

import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.clients.Deferred
import io.infinitic.clients.deferred.DeferredChannel
import io.infinitic.clients.deferred.DeferredSend
import io.infinitic.clients.deferred.ExistingDeferredWorkflow
import io.infinitic.clients.deferred.NewDeferredWorkflow
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
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.executors.errors.MethodFailedError
import io.infinitic.common.transport.ClientTopic
import io.infinitic.common.transport.InfiniticConsumerAsync
import io.infinitic.common.transport.InfiniticProducerAsync
import io.infinitic.common.transport.LoggedInfiniticProducer
import io.infinitic.common.transport.MainSubscription
import io.infinitic.common.transport.Topic
import io.infinitic.common.transport.WorkflowCmdTopic
import io.infinitic.common.transport.WorkflowTagTopic
import io.infinitic.common.workflows.data.channels.SignalId
import io.infinitic.common.workflows.data.methodRuns.WorkflowMethodId
import io.infinitic.common.workflows.data.workflows.WorkflowCancellationReason
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.CompleteTimers
import io.infinitic.common.workflows.engine.messages.DispatchMethodWorkflow
import io.infinitic.common.workflows.engine.messages.DispatchNewWorkflow
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.future.future
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.io.Closeable
import java.util.concurrent.CompletableFuture
import io.infinitic.common.workflows.engine.messages.RetryTasks as RetryTaskInWorkflow
import io.infinitic.common.workflows.tags.messages.RetryTasksByTag as RetryTaskInWorkflowByTag

internal class ClientDispatcher(
  logName: String,
  private val consumerAsync: InfiniticConsumerAsync,
  private val producerAsync: InfiniticProducerAsync
) : ProxyDispatcher, Closeable {
  private val logger = KotlinLogging.logger(logName)

  private val producer = LoggedInfiniticProducer(logName, producerAsync)

  // Name of the client
  private val emitterName by lazy { EmitterName(producer.name) }

  // flag telling if the client consumer loop is initialized
  private var isClientConsumerInitialized = false

  // Scope used to consuming messages
  private val clientScope = CoroutineScope(Dispatchers.IO)

  // Flow used to receive messages
  private val responseFlow = MutableSharedFlow<ClientMessage>(replay = 0)

  override fun close() {
    // Do not wait anymore for messages
    clientScope.cancel()
  }

  private fun <T : Message> T.sendToAsync(topic: Topic<T>) =
      runBlocking(clientScope.coroutineContext) {
        with(producerAsync) { sendToAsync(topic) }
      }

  // a message received by the client is sent to responseFlow
  @Suppress("UNUSED_PARAMETER")
  internal suspend fun handle(message: ClientMessage, publishTime: MillisInstant) {
    logger.debug { "Client ${producer.name}: Receiving $message" }
    responseFlow.emit(message)
  }

  // Utility to get access to last deferred
  internal fun getLastDeferred(): Deferred<*>? = localLastDeferred.get()

  // asynchronous call: dispatch(stub::method)(*args)
  fun <R : Any?> dispatchAsync(handler: ProxyHandler<*>): CompletableFuture<Deferred<R>> =
      when (handler) {
        is NewWorkflowProxyHandler -> dispatchNewWorkflowAsync(handler)
        is ExistingWorkflowProxyHandler -> dispatchExistingWorkflowAsync(handler)
        is ChannelProxyHandler -> dispatchSignalAsync(handler)
        is NewServiceProxyHandler -> thisShouldNotHappen()
        is ExistingServiceProxyHandler -> thisShouldNotHappen()
      }

  // synchronous call: stub.method(*args)
  override fun <R : Any?> dispatchAndWait(handler: ProxyHandler<*>): R =
      when (handler) {
        is NewWorkflowProxyHandler -> dispatchNewWorkflowAndWait(handler)
        is ExistingWorkflowProxyHandler -> dispatchMethodOnExistingWorkflowAndWait(handler)
        is ChannelProxyHandler -> dispatchSignalAndWait(handler)
        is ExistingServiceProxyHandler -> thisShouldNotHappen()
        is NewServiceProxyHandler -> thisShouldNotHappen()
      }

  internal fun <T> awaitNewWorkflow(
    deferred: NewDeferredWorkflow<T>,
    clientWaiting: Boolean
  ): T = awaitWorkflow(
      deferred.workflowName,
      deferred.workflowId,
      deferred.methodName,
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
        deferred.methodName,
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
    workflowMethodName: MethodName,
    workflowMethodId: WorkflowMethodId?,
    methodTimeout: MillisDuration?,
    dispatchTime: Long,
    clientWaiting: Boolean
  ): T {

    val runId = workflowMethodId ?: WorkflowMethodId.from(workflowId)

    // calculate timeout from now
    val timeout = methodTimeout
        ?.let { it.long - (System.currentTimeMillis() - dispatchTime) }
        ?.let { if (it < 0) 0 else it }
      ?: Long.MAX_VALUE

    // lazily starts client consumer if not already started and waits
    val waiting = waitForAsync(timeout) {
      it is MethodMessage && it.workflowId == workflowId && it.workflowMethodId == runId
    }

    // if task was not initially sync, then send WaitTask message
    if (clientWaiting) {
      val waitWorkflow = WaitWorkflow(
          workflowMethodId = runId,
          workflowName = workflowName,
          workflowId = workflowId,
          emitterName = emitterName,
          emittedAt = null,
          requesterWorkflowId = null,
          requesterWorkflowName = null,
          requesterWorkflowMethodId = null,
      )
      // synchronously sent the message to get errors
      waitWorkflow.sendToAsync(WorkflowCmdTopic)
    }

    // Get result
    val workflowResult = waiting.join()

    @Suppress("UNCHECKED_CAST")
    return when (workflowResult) {
      is MethodTimedOut, null -> {
        throw WorkflowTimedOutException(
            workflowName = workflowName.toString(),
            workflowId = workflowId.toString(),
            methodName = workflowMethodName.toString(),
            workflowMethodId = workflowMethodId?.toString(),
        )
      }

      is MethodCompleted -> workflowResult.methodReturnValue.value() as T

      is MethodCanceled -> throw WorkflowCanceledException(
          workflowName = workflowName.toString(),
          workflowId = workflowId.toString(),
          workflowMethodId = workflowMethodId?.toString(),
      )

      is MethodFailed -> throw WorkflowFailedException.from(
          MethodFailedError(
              workflowName = workflowName,
              workflowMethodName = workflowMethodName,
              workflowId = workflowId,
              workflowMethodId = workflowMethodId,
              deferredError = workflowResult.cause,
          ),
      )

      is MethodUnknown -> throw WorkflowUnknownException(
          workflowName = workflowName.toString(),
          workflowId = workflowId.toString(),
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
          emitterName = emitterName,
          emittedAt = null,
          requesterWorkflowId = null,
          requesterWorkflowName = null,
          requesterWorkflowMethodId = null,
      )
      msg.sendToAsync(WorkflowCmdTopic)
    }

    is RequestByWorkflowTag -> {
      val msg = CancelWorkflowByTag(
          workflowName = workflowName,
          workflowTag = requestBy.workflowTag,
          reason = WorkflowCancellationReason.CANCELED_BY_CLIENT,
          emitterWorkflowId = null,
          emitterName = emitterName,
          emittedAt = null,
          requesterWorkflowId = null,
          requesterWorkflowName = null,
          requesterWorkflowMethodId = null,
      )
      msg.sendToAsync(WorkflowTagTopic)
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
          emitterName = emitterName,
          emittedAt = null,
          requesterWorkflowId = null,
          requesterWorkflowName = null,
          requesterWorkflowMethodId = null,
      )
      msg.sendToAsync(WorkflowCmdTopic)
    }

    is RequestByWorkflowTag -> {
      val msg = RetryWorkflowTaskByTag(
          workflowName = workflowName,
          workflowTag = requestBy.workflowTag,
          emitterName = emitterName,
          emittedAt = null,
          requesterWorkflowId = null,
          requesterWorkflowName = null,
          requesterWorkflowMethodId = null,
      )
      msg.sendToAsync(WorkflowTagTopic)
    }

    else -> thisShouldNotHappen()
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
          emitterName = emitterName,
          emittedAt = null,
          requesterWorkflowId = null,
          requesterWorkflowName = null,
          requesterWorkflowMethodId = null,
      )
      msg.sendToAsync(WorkflowCmdTopic)
    }

    is RequestByWorkflowTag -> {
      val msg = CompleteTimersByTag(
          workflowName = workflowName,
          workflowTag = requestBy.workflowTag,
          workflowMethodId = workflowMethodId,
          emitterName = emitterName,
          emittedAt = null,
          requesterWorkflowId = null,
          requesterWorkflowName = null,
          requesterWorkflowMethodId = null,
      )
      msg.sendToAsync(WorkflowTagTopic)
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
          emittedAt = null,
          requesterWorkflowId = null,
          requesterWorkflowName = null,
          requesterWorkflowMethodId = null,
      )
      msg.sendToAsync(WorkflowCmdTopic)
    }

    is RequestByWorkflowTag -> {
      val msg = RetryTaskInWorkflowByTag(
          workflowName = workflowName,
          workflowTag = requestBy.workflowTag,
          taskId = taskId,
          taskStatus = taskStatus,
          serviceName = serviceName,
          emitterName = emitterName,
          emittedAt = null,
          requesterWorkflowId = null,
          requesterWorkflowName = null,
          requesterWorkflowMethodId = null,
      )
      msg.sendToAsync(WorkflowTagTopic)
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
    msg.sendToAsync(WorkflowTagTopic).join()

    val workflowIdsByTag = waiting.join() as WorkflowIdsByTag

    return workflowIdsByTag.workflowIds.map { it.toString() }.toSet()
  }

  // asynchronous call: dispatch(stub::method)(*args)
  @Suppress("UNCHECKED_CAST")
  private fun <R : Any?> dispatchNewWorkflowAsync(
    handler: NewWorkflowProxyHandler<*>
  ): CompletableFuture<Deferred<R>> =
      when (handler.isChannelGetter()) {
        true -> throw InvalidChannelUsageException()
        false -> {
          val deferredWorkflow = newDeferredWorkflow(
              handler.workflowName,
              handler.methodName,
              handler.method.returnType as Class<R>,
              getTimeout(handler),
          )

          dispatchNewWorkflowAsync(deferredWorkflow, false, handler)
        }
      }

  // synchronous call: stub.method(*args)
  @Suppress("UNCHECKED_CAST")
  private fun <R : Any?> dispatchNewWorkflowAndWait(handler: NewWorkflowProxyHandler<*>): R =
      when (handler.isChannelGetter()) {
        true -> throw InvalidChannelUsageException()
        false -> {
          val deferred = newDeferredWorkflow(
              handler.workflowName,
              handler.methodName,
              handler.method.returnType as Class<R>,
              getTimeout(handler),
          )
          dispatchNewWorkflowAsync(deferred, true, handler)

          awaitNewWorkflow(deferred, false)
        }
      }

  private fun <R : Any?> newDeferredWorkflow(
    workflowName: WorkflowName,
    methodName: MethodName,
    methodReturnClass: Class<R>,
    methodTimeout: MillisDuration?
  ) = NewDeferredWorkflow(workflowName, methodName, methodReturnClass, methodTimeout, this)
      .also {
        // store in ThreadLocal to be used in ::getDeferred
        localLastDeferred.set(it)
      }

  private fun <R : Any?> dispatchNewWorkflowAsync(
    deferred: NewDeferredWorkflow<R>,
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
        val workflowTags = handler.workflowTags.map {
          AddTagToWorkflow(
              workflowName = deferred.workflowName,
              workflowTag = it,
              workflowId = deferred.workflowId,
              emitterName = emitterName,
              emittedAt = null,
          )
        }

        // first, we send all tags in parallel
        val futures = workflowTags.map {
          it.sendToAsync(WorkflowTagTopic)
        }.toTypedArray()
        CompletableFuture.allOf(*futures).join()

        // dispatch workflow message
        val dispatchWorkflow = DispatchNewWorkflow(
            workflowName = deferred.workflowName,
            workflowId = deferred.workflowId,
            methodName = handler.methodName,
            methodParameters = handler.methodParameters,
            methodParameterTypes = handler.methodParameterTypes,
            workflowTags = handler.workflowTags,
            workflowMeta = handler.workflowMeta,
            requesterWorkflowName = null,
            requesterWorkflowId = null,
            requesterWorkflowMethodId = null,
            clientWaiting = clientWaiting,
            emitterName = emitterName,
            emittedAt = null,
        )

        // workflow message is dispatched after tags
        // to avoid a potential race condition if the engine remove tags
        dispatchWorkflow.sendToAsync(WorkflowCmdTopic).thenApply { deferred }
      }
      // a customId tag was provided
      1 -> {
        // dispatch workflow message with customId tag
        val dispatchWorkflowByCustomId = DispatchWorkflowByCustomId(
            workflowName = deferred.workflowName,
            workflowTag = customIds.first(),
            workflowId = deferred.workflowId,
            methodName = deferred.methodName,
            methodParameters = handler.methodParameters,
            methodParameterTypes = handler.methodParameterTypes,
            methodTimeout = deferred.methodTimeout,
            workflowTags = handler.workflowTags,
            workflowMeta = handler.workflowMeta,
            requesterWorkflowName = null,
            requesterWorkflowId = null,
            requesterWorkflowMethodId = null,
            clientWaiting = clientWaiting,
            emitterName = emitterName,
            emittedAt = null,
        )
        dispatchWorkflowByCustomId.sendToAsync(WorkflowTagTopic).thenApply { deferred }
      }
      // more than 1 custom tag were provided
      else -> {
        throw MultipleCustomIdException
      }
    }
  }

  // asynchronous call: dispatch(stub::method)(*args)
  @Suppress("UNCHECKED_CAST")
  private fun <R : Any?> dispatchExistingWorkflowAsync(
    handler: ExistingWorkflowProxyHandler<*>
  ): CompletableFuture<Deferred<R>> =
      when (handler.isChannelGetter()) {
        true -> {
          // special case of getting a channel from a workflow
          val channel = ChannelProxyHandler<SendChannel<*>>(handler).stub()
          CompletableFuture.completedFuture(DeferredChannel(channel) as Deferred<R>)
        }

        false -> {
          val deferred = existingDeferredWorkflow(
              handler.workflowName,
              handler.requestBy,
              handler.methodName,
              handler.method.returnType as Class<R>,
              getTimeout(handler),
          )

          dispatchMethodOnExistingWorkflowAsync(deferred, false, handler)
              .thenApply { deferred }
        }
      }

  // synchronous call: stub.method(*args)
  @Suppress("UNCHECKED_CAST")
  private fun <R : Any?> dispatchMethodOnExistingWorkflowAndWait(handler: ExistingWorkflowProxyHandler<*>): R =
      when (handler.isChannelGetter()) {
        true -> {
          // special case of getting a channel from a workflow
          @Suppress("UNCHECKED_CAST")
          ChannelProxyHandler<SendChannel<*>>(handler).stub() as R
        }

        false -> {
          val deferred = existingDeferredWorkflow(
              handler.workflowName,
              handler.requestBy,
              handler.methodName,
              handler.method.returnType as Class<R>,
              getTimeout(handler),
          )

          // synchronously sent the message to get errors
          dispatchMethodOnExistingWorkflowAsync(deferred, true, handler).join()

          awaitExistingWorkflow(deferred, false)
        }
      }

  private fun <R : Any?> existingDeferredWorkflow(
    workflowName: WorkflowName,
    requestBy: RequestBy,
    methodName: MethodName,
    methodReturnClass: Class<R>,
    methodTimeout: MillisDuration?
  ) = ExistingDeferredWorkflow(
      workflowName,
      requestBy,
      methodName,
      methodReturnClass,
      methodTimeout,
      this,
  ) // store in ThreadLocal to be used in ::getDeferred
      .also { localLastDeferred.set(it) }

  private fun <R : Any?> dispatchMethodOnExistingWorkflowAsync(
    deferred: ExistingDeferredWorkflow<R>,
    clientWaiting: Boolean,
    handler: ExistingWorkflowProxyHandler<*>
  ): CompletableFuture<Unit> = when (deferred.requestBy) {
    is RequestByWorkflowId -> {
      val dispatchMethod = DispatchMethodWorkflow(
          workflowName = deferred.workflowName,
          workflowId = deferred.requestBy.workflowId,
          workflowMethodId = deferred.workflowMethodId,
          methodName = handler.methodName,
          methodParameters = handler.methodParameters,
          methodParameterTypes = handler.methodParameterTypes,
          requesterWorkflowId = null,
          requesterWorkflowName = null,
          requesterWorkflowMethodId = null,
          clientWaiting = clientWaiting,
          emitterName = emitterName,
          emittedAt = null,
      )
      dispatchMethod.sendToAsync(WorkflowCmdTopic)
    }

    is RequestByWorkflowTag -> {
      val dispatchMethodByTag = DispatchMethodByTag(
          workflowName = deferred.workflowName,
          workflowTag = deferred.requestBy.workflowTag,
          workflowMethodId = deferred.workflowMethodId,
          methodName = handler.methodName,
          methodParameterTypes = handler.methodParameterTypes,
          methodParameters = handler.methodParameters,
          methodTimeout = deferred.methodTimeout,
          requesterWorkflowId = null,
          requesterWorkflowName = null,
          requesterWorkflowMethodId = null,
          clientWaiting = clientWaiting,
          emitterName = emitterName,
          emittedAt = null,
      )
      dispatchMethodByTag.sendToAsync(WorkflowTagTopic)
    }
  }

  // asynchronous call: dispatch(stub.channel::send, signal)
  private fun <S : Any?> dispatchSignalAsync(
    handler: ChannelProxyHandler<*>
  ): CompletableFuture<Deferred<S>> {
    val deferredSend = deferredSend<S>()

    return dispatchSignalAsync(deferredSend, handler).thenApply { deferredSend }
  }

  // synchronous call: stub.channel.send(signal)
  private fun <S : Any?> dispatchSignalAndWait(handler: ChannelProxyHandler<*>): S {
    val deferredSend = deferredSend<S>()

    // dispatch signal synchronously
    dispatchSignalAsync(deferredSend, handler).join()

    return deferredSend.await()
  }

  private fun dispatchSignalAsync(
    deferredSend: DeferredSend<*>,
    handler: ChannelProxyHandler<*>
  ): CompletableFuture<Unit> {
    if (handler.methodName.toString() != SendChannel<*>::send.name) thisShouldNotHappen()

    return when (handler.requestBy) {
      is RequestByWorkflowId -> {
        val sendSignal = SendSignal(
            channelName = handler.channelName,
            signalId = deferredSend.signalId,
            signalData = handler.signalData,
            channelTypes = handler.channelTypes,
            workflowName = handler.workflowName,
            workflowId = (handler.requestBy as RequestByWorkflowId).workflowId,
            emitterName = emitterName,
            emittedAt = null,
            requesterWorkflowId = null,
            requesterWorkflowName = null,
            requesterWorkflowMethodId = null,
        )
        sendSignal.sendToAsync(WorkflowCmdTopic)
      }

      is RequestByWorkflowTag -> {
        val sendSignalByTag = SendSignalByTag(
            workflowName = handler.workflowName,
            workflowTag = (handler.requestBy as RequestByWorkflowTag).workflowTag,
            channelName = handler.channelName,
            signalId = deferredSend.signalId,
            signalData = handler.signalData,
            channelTypes = handler.channelTypes,
            parentWorkflowId = null,
            emitterName = emitterName,
            emittedAt = null,
            requesterWorkflowId = null,
            requesterWorkflowName = null,
            requesterWorkflowMethodId = null,
        )
        sendSignalByTag.sendToAsync(WorkflowTagTopic)
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


  private fun getTimeout(handler: ProxyHandler<*>): MillisDuration? =
      handler.timeoutInMillisDuration.getOrElse {
        throw IllegalStateException(
            "Unable to retrieve Timeout info when dispatching ${handler.method}",
            it,
        )
      }

  private val logMessageSentToDLQ = { message: Message?, e: Exception ->
    logger.error(e) { "Unable to process message ${message ?: "(Not Deserialized)"}" }
  }

  private fun waitForAsync(
    timeout: Long = Long.MAX_VALUE,
    predicate: suspend (ClientMessage) -> Boolean
  ): CompletableFuture<ClientMessage?> {
    // lazily starts client consumer if not already started
    synchronized(this) {
      if (!isClientConsumerInitialized) {
        runBlocking(clientScope.coroutineContext) {
          consumerAsync.start(
              subscription = MainSubscription(ClientTopic),
              entity = emitterName.toString(),
              handler = ::handle,
              beforeDlq = logMessageSentToDLQ,
              concurrency = 1,
          )
          isClientConsumerInitialized = true
        }
      }
    }

    // wait for the first message that matches the predicate
    return clientScope.future {
      withTimeoutOrNull(timeout) {
        responseFlow.first { predicate(it) }
      }
    }
  }


  companion object {
    @JvmStatic
    private val localLastDeferred: ThreadLocal<Deferred<*>?> = ThreadLocal()
  }
}
