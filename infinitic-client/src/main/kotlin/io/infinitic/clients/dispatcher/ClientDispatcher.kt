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
import io.infinitic.common.clients.messages.MethodRunUnknown
import io.infinitic.common.clients.messages.WorkflowIdsByTag
import io.infinitic.common.clients.messages.interfaces.MethodMessage
import io.infinitic.common.data.ClientName
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.exceptions.thisShouldNotHappen
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
import io.infinitic.common.transport.InfiniticConsumer
import io.infinitic.common.transport.InfiniticProducer
import io.infinitic.common.workflows.data.channels.SignalId
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.workflows.WorkflowCancellationReason
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.CompleteTimers
import io.infinitic.common.workflows.engine.messages.DispatchMethodOnRunningWorkflow
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.future.future
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.io.Closeable
import java.util.concurrent.CompletableFuture
import io.infinitic.common.workflows.engine.messages.RetryTasks as RetryTaskInWorkflow
import io.infinitic.common.workflows.tags.messages.RetryTasksByTag as RetryTaskInWorkflowByTag

class ClientDispatcher(
  private val consumer: InfiniticConsumer,
  private val producer: InfiniticProducer
) : ProxyDispatcher, Closeable {
  private val logger = KotlinLogging.logger {}

  // Name of the client
  private val clientName by lazy { ClientName(producer.name) }

  // Scope used to send messages
  private val sendingScope = CoroutineScope(Dispatchers.IO)

  // flag telling if the client consumer loop is initialized
  private var isClientConsumerInitialized = false

  // Scope used to consuming messages
  private val waitingScope = CoroutineScope(Dispatchers.IO)

  // Flow used to receive messages
  private val responseFlow = MutableSharedFlow<ClientMessage>(replay = 0)

  override fun close() {
    // Do not wait anymore for messages
    waitingScope.cancel()
    // wait for all messages to be sent
    runBlocking {
      sendingScope.coroutineContext.job.children.forEach { it.join() }
    }
    sendingScope.cancel()
  }

  // a message received by the client is sent to responseFlow
  internal suspend fun handle(message: ClientMessage) {
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
        is ExistingWorkflowProxyHandler -> dispatchExistingWorkflowAndWait(handler)
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
        deferred.methodRunId,
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
    methodName: MethodName,
    methodRunId: MethodRunId?,
    methodTimeout: MillisDuration?,
    dispatchTime: Long,
    clientWaiting: Boolean
  ): T {

    // lazily starts client consumer if not already started
    if (!isClientConsumerInitialized) {
      consumer.startClientConsumerAsync(::handle, null, clientName)
      isClientConsumerInitialized = true
    }

    val runId = methodRunId ?: MethodRunId.from(workflowId)

    // if task was not initially sync, then send WaitTask message
    if (clientWaiting) {
      val waitWorkflow = WaitWorkflow(
          workflowName = workflowName,
          workflowId = workflowId,
          methodRunId = runId,
          emitterName = clientName,
      )
      producer.sendAsync(waitWorkflow).join()
    }
    // calculate timeout from now
    val timeout = methodTimeout
        ?.let { it.long - (System.currentTimeMillis() - dispatchTime) }
        ?.let { if (it < 0) 0 else it }
      ?: Long.MAX_VALUE


    // wait for the message of method completion
    val workflowResult = waitingScope.future {
      withTimeoutOrNull(timeout) {
        responseFlow.first {
          it is MethodMessage && it.workflowId == workflowId && it.methodRunId == runId
        }
      }
    }.join()

    @Suppress("UNCHECKED_CAST")
    return when (workflowResult) {
      null -> throw WorkflowTimedOutException(
          workflowName = workflowName.toString(),
          workflowId = workflowId.toString(),
          methodName = methodName.toString(),
          methodRunId = methodRunId?.toString(),
      )

      is MethodCompleted -> workflowResult.methodReturnValue.value() as T

      is MethodCanceled -> throw WorkflowCanceledException(
          workflowName = workflowName.toString(),
          workflowId = workflowId.toString(),
          methodRunId = methodRunId?.toString(),
      )

      is MethodFailed -> throw WorkflowFailedException.from(
          MethodFailedError(
              workflowName = workflowName,
              methodName = methodName,
              workflowId = workflowId,
              methodRunId = methodRunId,
              deferredError = workflowResult.cause,
          ),
      )

      is MethodRunUnknown -> throw WorkflowUnknownException(
          workflowName = workflowName.toString(),
          workflowId = workflowId.toString(),
          methodRunId = methodRunId?.toString(),
      )

      else -> thisShouldNotHappen("Unexpected ${workflowResult::class}")
    }
  }

  fun cancelWorkflowAsync(
    workflowName: WorkflowName,
    requestBy: RequestBy,
    methodRunId: MethodRunId?,
  ): CompletableFuture<Unit> = sendingScope.future {
    when (requestBy) {
      is RequestByWorkflowId -> {
        val msg = CancelWorkflow(
            workflowName = workflowName,
            workflowId = requestBy.workflowId,
            methodRunId = methodRunId,
            reason = WorkflowCancellationReason.CANCELED_BY_CLIENT,
            emitterName = clientName,
        )
        launch { producer.send(msg) }
      }

      is RequestByWorkflowTag -> {
        val msg = CancelWorkflowByTag(
            workflowName = workflowName,
            workflowTag = requestBy.workflowTag,
            reason = WorkflowCancellationReason.CANCELED_BY_CLIENT,
            emitterWorkflowId = null,
            emitterName = clientName,
        )
        launch { producer.send(msg) }
      }

      else -> thisShouldNotHappen()
    }
  }

  fun retryWorkflowTaskAsync(
    workflowName: WorkflowName,
    requestBy: RequestBy
  ): CompletableFuture<Unit> =
      sendingScope.future {
        when (requestBy) {
          is RequestByWorkflowId -> {
            val msg = RetryWorkflowTask(
                workflowName = workflowName,
                workflowId = requestBy.workflowId,
                emitterName = clientName,
            )
            launch { producer.send(msg) }
          }

          is RequestByWorkflowTag -> {
            val msg = RetryWorkflowTaskByTag(
                workflowName = workflowName,
                workflowTag = requestBy.workflowTag,
                emitterName = clientName,
            )
            launch { producer.send(msg) }
          }

          else -> thisShouldNotHappen()
        }
      }

  fun completeTimersAsync(
    workflowName: WorkflowName,
    requestBy: RequestBy,
    methodRunId: MethodRunId?
  ): CompletableFuture<Unit> =
      sendingScope.future {
        when (requestBy) {
          is RequestByWorkflowId -> {
            val msg = CompleteTimers(
                workflowName = workflowName,
                workflowId = requestBy.workflowId,
                emitterName = clientName,
                methodRunId = methodRunId,
            )
            launch { producer.send(msg) }
          }

          is RequestByWorkflowTag -> {
            val msg = CompleteTimersByTag(
                workflowName = workflowName,
                workflowTag = requestBy.workflowTag,
                emitterName = clientName,
                methodRunId = methodRunId,
            )
            launch { producer.send(msg) }
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
  ): CompletableFuture<Unit> =
      sendingScope.future {
        when (requestBy) {
          is RequestByWorkflowId -> {
            val msg = RetryTaskInWorkflow(
                workflowName = workflowName,
                workflowId = requestBy.workflowId,
                emitterName = clientName,
                taskId = taskId,
                taskStatus = taskStatus,
                serviceName = serviceName,
            )
            launch { producer.send(msg) }
          }

          is RequestByWorkflowTag -> {
            val msg = RetryTaskInWorkflowByTag(
                workflowName = workflowName,
                workflowTag = requestBy.workflowTag,
                emitterName = clientName,
                taskId = taskId,
                taskStatus = taskStatus,
                serviceName = serviceName,
            )
            launch { producer.send(msg) }
          }

          else -> thisShouldNotHappen()
        }
      }

  fun getWorkflowIdsByTag(workflowName: WorkflowName, requestBy: RequestBy): Set<String> =
      when (requestBy) {
        is RequestByWorkflowId -> setOf(requestBy.workflowId.toString())
        is RequestByWorkflowTag -> {
          val workflowIdsByTag = sendingScope.future {
            val msg = GetWorkflowIdsByTag(
                workflowName = workflowName,
                workflowTag = requestBy.workflowTag,
                emitterName = clientName,
            )
            launch { producer.send(msg) }

            responseFlow.first {
              (it is WorkflowIdsByTag) &&
                  (it.workflowName == workflowName) &&
                  (it.workflowTag == requestBy.workflowTag)
            } as WorkflowIdsByTag
          }.join()

          workflowIdsByTag.workflowIds.map { it.toString() }.toSet()
        }

        else -> thisShouldNotHappen()
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
              emitterName = clientName,
          )
        }
        // dispatch workflow message
        val dispatchWorkflow = DispatchNewWorkflow(
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
            emitterName = clientName,
        )

        sendingScope.future {

          // first, we send all tags in parallel
          coroutineScope { workflowTags.forEach { launch { producer.send(it) } } }
          // workflow message is dispatched after tags
          // to avoid a potential race condition if the engine remove tags
          producer.send(dispatchWorkflow)

          deferred
        }
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
            parentWorkflowName = null,
            parentWorkflowId = null,
            parentMethodRunId = null,
            clientWaiting = clientWaiting,
            emitterName = clientName,
        )

        sendingScope.future {
          producer.send(dispatchWorkflowByCustomId)

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

          sendingScope.future {
            dispatchExistingWorkflow(deferred, false, handler)
            deferred
          }
        }
      }

  // synchronous call: stub.method(*args)
  @Suppress("UNCHECKED_CAST")
  private fun <R : Any?> dispatchExistingWorkflowAndWait(handler: ExistingWorkflowProxyHandler<*>): R =
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

          sendingScope.launch { dispatchExistingWorkflow(deferred, true, handler) }

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
  )
      // store in ThreadLocal to be used in ::getDeferred
      .also { localLastDeferred.set(it) }

  private fun <R : Any?> dispatchExistingWorkflow(
    deferred: ExistingDeferredWorkflow<R>,
    clientWaiting: Boolean,
    handler: ExistingWorkflowProxyHandler<*>
  ) =
      when (deferred.requestBy) {
        is RequestByWorkflowId -> {
          val dispatchMethod = DispatchMethodOnRunningWorkflow(
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
              emitterName = clientName,
          )
          producer.sendAsync(dispatchMethod).join()
        }

        is RequestByWorkflowTag -> {
          val dispatchMethodByTag = DispatchMethodByTag(
              workflowName = deferred.workflowName,
              workflowTag = deferred.requestBy.workflowTag,
              parentWorkflowId = null,
              parentWorkflowName = null,
              parentMethodRunId = null,
              methodRunId = deferred.methodRunId,
              methodName = handler.methodName,
              methodParameterTypes = handler.methodParameterTypes,
              methodParameters = handler.methodParameters,
              methodTimeout = deferred.methodTimeout,
              clientWaiting = clientWaiting,
              emitterName = clientName,
          )
          producer.sendAsync(dispatchMethodByTag).join()
        }
      }

  // asynchronous call: dispatch(stub.channel::send, signal)
  private fun <S : Any?> dispatchSignalAsync(
    handler: ChannelProxyHandler<*>
  ): CompletableFuture<Deferred<S>> {
    val deferredSend = deferredSend<S>()

    return sendingScope.future {
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
        val sendSignal = SendSignal(
            workflowName = handler.workflowName,
            workflowId = (handler.requestBy as RequestByWorkflowId).workflowId,
            channelName = handler.channelName,
            signalId = deferredSend.signalId,
            signalData = handler.signalData,
            channelTypes = handler.channelTypes,
            emitterName = clientName,
        )
        producer.sendAsync(sendSignal).join()
      }

      handler.requestBy is RequestByWorkflowTag -> {
        val sendSignalByTag = SendSignalByTag(
            workflowName = handler.workflowName,
            workflowTag = (handler.requestBy as RequestByWorkflowTag).workflowTag,
            channelName = handler.channelName,
            signalId = deferredSend.signalId,
            signalData = handler.signalData,
            channelTypes = handler.channelTypes,
            emitterWorkflowId = null,
            emitterName = clientName,
        )
        producer.sendAsync(sendSignalByTag).join()
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


  private fun getTimeout(handler: ProxyHandler<*>): MillisDuration? = handler.timeout.getOrElse {
    throw IllegalStateException("Unable to retrieve Timeout info when dispatching ${handler.method}")
  }

  companion object {
    @JvmStatic
    private val localLastDeferred: ThreadLocal<Deferred<*>?> = ThreadLocal()
  }
}
