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
import io.infinitic.common.transport.InfiniticConsumerAsync
import io.infinitic.common.transport.InfiniticProducerAsync
import io.infinitic.common.transport.LoggedInfiniticProducer
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
import java.util.concurrent.CompletionException

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
          logger.info { "Interruption! Closing worker ${producerAsync.name}!" }
          close()
          logger.info { "Worker ${producerAsync.name} closed" }
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
    startAsync().join()
  } catch (e: CompletionException) {
    // Do nothing
  }

  /**
   * Start worker asynchronously
   */
  fun startAsync(): CompletableFuture<Unit> {
    val futures = mutableListOf<CompletableFuture<Unit>>()

    workerRegistry.workflowTags.forEach {
      // WORKFLOW-TAG
      val workflowTagEngine = WorkflowTagEngine(it.value.storage, producerAsync)

      futures.add(
          consumerAsync.startWorkflowTagConsumerAsync(
              handler = workflowTagEngine::handle,
              beforeDlq = null,
              workflowName = it.key,
              concurrency = it.value.concurrency,
          ),
      )
    }

    workerRegistry.workflowEngines.forEach {
      // WORKFLOW-CMD
      val workflowCmdHandler = WorkflowCmdHandler(producerAsync)

      futures.add(
          consumerAsync.startWorkflowCmdConsumerAsync(
              handler = workflowCmdHandler::handle,
              beforeDlq = null,
              workflowName = it.key,
              concurrency = it.value.concurrency,
          ),
      )

      // WORKFLOW-ENGINE
      val workflowEngine = WorkflowEngine(it.value.storage, producerAsync)

      futures.add(
          consumerAsync.startWorkflowEngineConsumerAsync(
              handler = workflowEngine::handle,
              beforeDlq = null,
              workflowName = it.key,
              concurrency = it.value.concurrency,
          ),
      )

      // WORKFLOW-DELAY
      futures.add(
          consumerAsync.startDelayedWorkflowEngineConsumerAsync(
              handler = { msg, _ -> delayedWorkflowProducer.run { sendToWorkflowEngine(msg) } },
              beforeDlq = null,
              workflowName = it.key,
              concurrency = it.value.concurrency,
          ),
      )

      // WORKFLOW-EVENTS
      val workflowEvent = WorkflowEventHandler(producerAsync)

      futures.add(
          consumerAsync.startWorkflowEventsConsumerAsync(
              handler = workflowEvent::handle,
              beforeDlq = null,
              workflowName = it.key,
              concurrency = it.value.concurrency,
          ),
      )
    }

    workerRegistry.workflows.forEach {
      // WORKFLOW-TASK_EXECUTOR
      val workflowTaskExecutor = TaskExecutor(workerRegistry, producerAsync, client)

      futures.add(
          consumerAsync.startWorkflowTaskConsumerAsync(
              handler = workflowTaskExecutor::handle,
              beforeDlq = { message, cause ->
                workflowTaskExecutor.run {
                  sendTaskFailed(
                      message,
                      cause,
                      Task.meta,
                      sendingDlqMessage,
                  )
                }
              },
              workflowName = it.key,
              concurrency = it.value.concurrency,
          ),
      )

      // WORKFLOW-TASK_EXECUTOR-DELAY
      futures.add(
          consumerAsync.startDelayedWorkflowTaskConsumerAsync(
              handler = { msg, _ -> delayedTaskProducer.run { sendToTaskExecutor(msg) } },
              beforeDlq = null,
              workflowName = it.key,
              concurrency = it.value.concurrency,
          ),
      )

      // WORKFLOW-TASK-EVENT
      val workflowTaskEventHandler = TaskEventHandler(producerAsync)

      futures.add(
          consumerAsync.startWorkflowTaskEventsConsumerAsync(
              handler = workflowTaskEventHandler::handle,
              beforeDlq = null,
              workflowName = it.key,
              concurrency = it.value.concurrency,
          ),
      )
    }

    workerRegistry.serviceTags.forEach {
      // TASK-TAG
      val tagEngine = TaskTagEngine(it.value.storage, producerAsync)

      futures.add(
          consumerAsync.startTaskTagConsumerAsync(
              handler = tagEngine::handle,
              beforeDlq = null,
              serviceName = it.key,
              concurrency = it.value.concurrency,
          ),
      )
    }

    workerRegistry.services.forEach {
      // TASK-EXECUTOR
      val taskExecutor = TaskExecutor(workerRegistry, producerAsync, client)

      futures.add(
          consumerAsync.startTaskExecutorConsumerAsync(
              handler = taskExecutor::handle,
              beforeDlq = { message, cause ->
                taskExecutor.run { sendTaskFailed(message, cause, Task.meta, sendingDlqMessage) }
              },
              serviceName = it.key,
              concurrency = it.value.concurrency,
          ),
      )

      // TASK-EXECUTOR-DELAY
      futures.add(
          consumerAsync.startDelayedTaskExecutorConsumerAsync(
              handler = { msg, _ -> delayedTaskProducer.run { sendToTaskExecutor(msg) } },
              beforeDlq = null,
              serviceName = it.key,
              concurrency = it.value.concurrency,
          ),
      )

      // TASK-EVENTS
      val taskEventHandler = TaskEventHandler(producerAsync)

      futures.add(
          consumerAsync.startTaskEventsConsumerAsync(
              handler = taskEventHandler::handle,
              beforeDlq = null,
              serviceName = it.key,
              concurrency = it.value.concurrency,
          ),
      )
    }

    logger.info {
      "Worker \"${producerAsync.name}\" ready" + when (consumerAsync is PulsarInfiniticConsumerAsync) {
        true -> " (shutdownGracePeriodInSeconds= ${consumerAsync.shutdownGracePeriodInSeconds})"
        false -> ""
      }
    }

    return CompletableFuture.allOf(*futures.toTypedArray()).thenApply { }
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
