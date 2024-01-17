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
package io.infinitic.workers

import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.autoclose.addAutoCloseResource
import io.infinitic.autoclose.autoClose
import io.infinitic.clients.InfiniticClient
import io.infinitic.clients.InfiniticClientInterface
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.messages.Message
import io.infinitic.common.transport.InfiniticConsumerAsync
import io.infinitic.common.transport.InfiniticProducerAsync
import io.infinitic.common.transport.LoggedInfiniticProducer
import io.infinitic.events.toCloudEvent
import io.infinitic.pulsar.PulsarInfiniticConsumerAsync
import io.infinitic.tasks.Task
import io.infinitic.tasks.executor.TaskEventHandler
import io.infinitic.tasks.executor.TaskExecutor
import io.infinitic.tasks.tag.TaskTagEngine
import io.infinitic.transport.config.TransportConfig
import io.infinitic.workers.config.WorkerConfig
import io.infinitic.workers.config.WorkerConfigInterface
import io.infinitic.workers.register.InfiniticRegister
import io.infinitic.workers.register.InfiniticRegisterInterface
import io.infinitic.workflows.engine.WorkflowCmdHandler
import io.infinitic.workflows.engine.WorkflowEngine
import io.infinitic.workflows.engine.WorkflowEventHandler
import io.infinitic.workflows.tag.WorkflowTagEngine
import java.util.concurrent.CompletableFuture
import kotlin.system.exitProcess

@Suppress("unused")
class InfiniticWorker(
  private val register: InfiniticRegisterInterface,
  private val consumerAsync: InfiniticConsumerAsync,
  private val producerAsync: InfiniticProducerAsync,
  val client: InfiniticClientInterface
) : AutoCloseable, InfiniticRegisterInterface by register {

  private val logger = KotlinLogging.logger {}

  init {
    // Aggregate logs from consumerAsync with InfiniticWorker's
    consumerAsync.logName = this::class.java.name

    Runtime.getRuntime().addShutdownHook(
        Thread {
          logger.info { "Closing worker!" }
          close()
          logger.info { "Worker closed" }
        },
    )
  }

  private val delayedTaskProducer =
      LoggedInfiniticProducer(TaskExecutor::class.java.name, producerAsync)

  private val delayedWorkflowProducer =
      LoggedInfiniticProducer(WorkflowEngine::class.java.name, producerAsync)

  private val workerRegistry = register.registry

  private val sendingDlqMessage = { "Unable to process message, sending to Dead Letter Queue" }

  override fun close() {
    autoClose()
  }


  /**
   * Start worker synchronously
   * (blocks the current thread)
   */
  fun start(): Unit = try {
    startAsync().get()
  } catch (e: Throwable) {
    // exiting if any of startAsyncs is failing
    // this will trigger the shutdown hook
    exitProcess(1)
  }

  /**
   * Start worker asynchronously
   */
  fun startAsync(): CompletableFuture<Unit> {
    val futures = mutableListOf<CompletableFuture<Unit>>()

    val logMessageSentToDLQ = { message: Message?, e: Exception ->
      logger.error(e) { "Sending message to DLQ ${message ?: "(Not Deserialized)"}" }
    }

    workerRegistry.workflowTags.forEach {
      // WORKFLOW-TAG
      val workflowTagEngine = WorkflowTagEngine(it.value.storage, producerAsync)

      futures.addIfNotDone(
          consumerAsync.startWorkflowTagConsumerAsync(
              handler = workflowTagEngine::handle,
              beforeDlq = logMessageSentToDLQ,
              workflowName = it.key,
              concurrency = it.value.concurrency,
          ),
      )
    }

    workerRegistry.workflowEngines.forEach {
      // WORKFLOW-CMD
      val workflowCmdHandler = WorkflowCmdHandler(producerAsync)

      futures.addIfNotDone(
          consumerAsync.startWorkflowCmdConsumerAsync(
              handler = workflowCmdHandler::handle,
              beforeDlq = logMessageSentToDLQ,
              workflowName = it.key,
              concurrency = it.value.concurrency,
          ),
      )

      // WORKFLOW-ENGINE
      val workflowEngine = WorkflowEngine(it.value.storage, producerAsync)

      futures.addIfNotDone(
          consumerAsync.startWorkflowEngineConsumerAsync(
              handler = workflowEngine::handle,
              beforeDlq = logMessageSentToDLQ,
              workflowName = it.key,
              concurrency = it.value.concurrency,
          ),
      )

      // WORKFLOW-DELAY
      futures.addIfNotDone(
          consumerAsync.startDelayedWorkflowEngineConsumerAsync(
              handler = { msg, _ -> delayedWorkflowProducer.sendToWorkflowEngine(msg) },
              beforeDlq = logMessageSentToDLQ,
              workflowName = it.key,
              concurrency = it.value.concurrency,
          ),
      )

      // WORKFLOW-EVENTS
      val workflowEventHandler = WorkflowEventHandler(producerAsync)

      futures.addIfNotDone(
          consumerAsync.startWorkflowEventsConsumerAsync(
              handler = workflowEventHandler::handle,
              beforeDlq = logMessageSentToDLQ,
              workflowName = it.key,
              concurrency = it.value.concurrency,
          ),
      )
    }

    workerRegistry.workflowExecutors.forEach {
      // WORKFLOW-TASK_EXECUTOR
      val workflowTaskExecutor = TaskExecutor(workerRegistry, producerAsync, client)

      futures.addIfNotDone(
          consumerAsync.startWorkflowTaskConsumerAsync(
              handler = workflowTaskExecutor::handle,
              beforeDlq = { message, cause ->
                logMessageSentToDLQ(message, cause)
                message?.let {
                  workflowTaskExecutor.sendTaskFailed(it, cause, Task.meta, sendingDlqMessage)
                }
              },
              workflowName = it.key,
              concurrency = it.value.concurrency,
          ),
      )

      // WORKFLOW-TASK_EXECUTOR-DELAY
      futures.addIfNotDone(
          consumerAsync.startDelayedWorkflowTaskConsumerAsync(
              handler = { msg, _ -> delayedTaskProducer.sendToServiceExecutor(msg) },
              beforeDlq = logMessageSentToDLQ,
              workflowName = it.key,
              concurrency = it.value.concurrency,
          ),
      )

      // WORKFLOW-TASK-EVENT
      val workflowTaskEventHandler = TaskEventHandler(producerAsync)

      futures.addIfNotDone(
          consumerAsync.startWorkflowTaskEventsConsumerAsync(
              handler = workflowTaskEventHandler::handle,
              beforeDlq = logMessageSentToDLQ,
              workflowName = it.key,
              concurrency = it.value.concurrency,
          ),
      )
    }

    workerRegistry.serviceTags.forEach {
      // TASK-TAG
      val tagEngine = TaskTagEngine(it.value.storage, producerAsync)

      futures.addIfNotDone(
          consumerAsync.startTaskTagConsumerAsync(
              handler = tagEngine::handle,
              beforeDlq = logMessageSentToDLQ,
              serviceName = it.key,
              concurrency = it.value.concurrency,
          ),
      )
    }

    workerRegistry.serviceExecutors.forEach {

      // TASK-EXECUTOR
      val taskExecutor = TaskExecutor(workerRegistry, producerAsync, client)

      futures.addIfNotDone(
          consumerAsync.startTaskExecutorConsumerAsync(
              handler = taskExecutor::handle,
              beforeDlq = { message, cause ->
                logMessageSentToDLQ(message, cause)
                message?.let {
                  taskExecutor.sendTaskFailed(it, cause, Task.meta, sendingDlqMessage)
                }
              },
              serviceName = it.key,
              concurrency = it.value.concurrency,
          ),
      )


      // TASK-EXECUTOR-DELAY
      futures.addIfNotDone(
          consumerAsync.startDelayedTaskExecutorConsumerAsync(
              handler = { msg, _ -> delayedTaskProducer.sendToServiceExecutor(msg) },
              beforeDlq = logMessageSentToDLQ,
              serviceName = it.key,
              concurrency = it.value.concurrency,
          ),
      )

      // TASK-EVENTS
      val taskEventHandler = TaskEventHandler(producerAsync)

      futures.addIfNotDone(
          consumerAsync.startTaskEventsConsumerAsync(
              handler = taskEventHandler::handle,
              beforeDlq = logMessageSentToDLQ,
              serviceName = it.key,
              concurrency = it.value.concurrency,
          ),
      )
    }

    workerRegistry.serviceListeners.forEach {
      val eventHandler = { message: Message, publishedAt: MillisInstant ->
        it.value.eventListener.onCloudEvent(message.toCloudEvent(publishedAt))
      }

      // TASK-EXECUTOR topic
      futures.addIfNotDone(
          consumerAsync.startTaskExecutorConsumerAsync(
              handler = eventHandler,
              beforeDlq = logMessageSentToDLQ,
              serviceName = it.key,
              concurrency = it.value.concurrency,
          ),
      )

      // TASK-EXECUTOR-DELAY topic
      futures.addIfNotDone(
          consumerAsync.startDelayedTaskExecutorConsumerAsync(
              handler = eventHandler,
              beforeDlq = logMessageSentToDLQ,
              serviceName = it.key,
              concurrency = it.value.concurrency,
          ),
      )

      // TASK-EVENTS topic
      futures.addIfNotDone(
          consumerAsync.startTaskEventsConsumerAsync(
              handler = eventHandler,
              beforeDlq = logMessageSentToDLQ,
              serviceName = it.key,
              concurrency = it.value.concurrency,
          ),
      )
    }

    logger.info {
      "Worker \"${producerAsync.name}\" ready" + when (consumerAsync is PulsarInfiniticConsumerAsync) {
        true -> " (shutdownGracePeriodInSeconds=${consumerAsync.shutdownGracePeriodInSeconds}s)"
        false -> ""
      }
    }

    return CompletableFuture.anyOf(*futures.toTypedArray()).thenApply {}
  }

  private fun MutableList<CompletableFuture<Unit>>.addIfNotDone(future: CompletableFuture<Unit>) {
    if (!future.isDone) add(future)
  }

  companion object {
    /** Create [InfiniticWorker] from config */
    @JvmStatic
    fun fromConfig(workerConfig: WorkerConfigInterface): InfiniticWorker = with(workerConfig) {
      val transportConfig = TransportConfig(transport, pulsar, shutdownGracePeriodInSeconds)

      /** Infinitic Consumer */
      val consumerAsync = transportConfig.consumerAsync

      /** Infinitic  Producer */
      val producerAsync = transportConfig.producerAsync

      // apply name if it exists
      name?.let { producerAsync.name = it }

      /** Infinitic Client */
      val client = InfiniticClient(consumerAsync, producerAsync)

      /** Infinitic Register */
      val register = InfiniticRegister(InfiniticWorker::class.java.name, this)

      /** Infinitic Worker */
      InfiniticWorker(register, consumerAsync, producerAsync, client).also {
        // close client with the worker
        it.addAutoCloseResource(client)
        // close consumer with the worker
        it.addAutoCloseResource(consumerAsync)
        // close storages with the worker
        it.addAutoCloseResource(register)
      }
    }

    /** Create [InfiniticWorker] from config in resources */
    @JvmStatic
    fun fromConfigResource(vararg resources: String): InfiniticWorker =
        fromConfig(WorkerConfig.fromResource(*resources))

    /** Create [InfiniticWorker] from config in system file */
    @JvmStatic
    fun fromConfigFile(vararg files: String): InfiniticWorker =
        fromConfig(WorkerConfig.fromFile(*files))
  }
}
