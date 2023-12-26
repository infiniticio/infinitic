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
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.tasks.tags.messages.TaskTagMessage
import io.infinitic.common.transport.InfiniticConsumerAsync
import io.infinitic.common.transport.InfiniticProducerAsync
import io.infinitic.common.transport.LoggedInfiniticProducer
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagMessage
import io.infinitic.tasks.executor.TaskExecutor
import io.infinitic.tasks.tag.TaskTagEngine
import io.infinitic.transport.config.TransportConfig
import io.infinitic.workers.config.WorkerConfig
import io.infinitic.workers.config.WorkerConfigInterface
import io.infinitic.workers.register.InfiniticRegister
import io.infinitic.workers.register.InfiniticRegisterInterface
import io.infinitic.workflows.engine.WorkflowEngine
import io.infinitic.workflows.tag.WorkflowTagEngine
import java.util.concurrent.CompletableFuture

@Suppress("unused")
class InfiniticWorker(
  private val register: InfiniticRegisterInterface,
  private val consumerAsync: InfiniticConsumerAsync,
  private val producerAsync: InfiniticProducerAsync,
  val client: InfiniticClientInterface
) : AutoCloseable, InfiniticRegisterInterface by register {

  private val logger = KotlinLogging.logger {}

  private val taskExecutorDelayedProducer =
      LoggedInfiniticProducer(TaskExecutor::javaClass.name + "Delay", producerAsync)
  private val workflowEngineDelayedProducer =
      LoggedInfiniticProducer(WorkflowEngine::javaClass.name + "Delay", producerAsync)

  private val workerRegistry = register.registry

  private val sendingDlqMessage = { "Unable to process message, sending to Dead Letter Queue" }

  override fun close() {
    autoClose()
  }

  /**
   * Start worker synchronously
   * (blocks the current thread)
   */
  fun start(): Void = startAsync().join()

  /**
   * Start worker asynchronously
   */
  fun startAsync(): CompletableFuture<Void> {
    val futures = mutableListOf<CompletableFuture<Unit>>()

    // start workflow tags
    workerRegistry.workflowTags.forEach {
      val tagEngine = WorkflowTagEngine(it.value.storage, producerAsync)

      val handler: (suspend (WorkflowTagMessage) -> Unit) =
          { message: WorkflowTagMessage -> tagEngine.handle(message) }

      // do nothing before sending message on dead letter queue
      val beforeDlq = null

      futures.add(
          consumerAsync.startWorkflowTagConsumerAsync(
              handler = handler,
              beforeDlq = beforeDlq,
              workflowName = it.key,
              concurrency = it.value.concurrency,
          ),
      )
    }

    workerRegistry.workflowEngines.forEach {
      // start workflow engines
      val workflowEngine = WorkflowEngine(it.value.storage, producerAsync)

      val handler: (suspend (WorkflowEngineMessage) -> Unit) =
          { message: WorkflowEngineMessage -> workflowEngine.handle(message) }

      // do nothing before sending message on dead letter queue
      val beforeDlq = null

      futures.add(
          consumerAsync.startWorkflowEngineConsumerAsync(
              handler = handler,
              beforeDlq = beforeDlq,
              workflowName = it.key,
              concurrency = it.value.concurrency,
          ),
      )

      // start consumer for delayed WorkflowEngineMessage
      futures.add(
          consumerAsync.startDelayedWorkflowEngineConsumerAsync(
              handler = { message: WorkflowEngineMessage ->
                workflowEngineDelayedProducer.send(
                    message,
                )
              },
              beforeDlq = beforeDlq,
              workflowName = it.key,
              concurrency = it.value.concurrency,
          ),
      )
    }

    // start workflow task executors
    workerRegistry.workflows.forEach {
      val taskExecutor = TaskExecutor(workerRegistry, producerAsync, client)

      val handler: (suspend (TaskExecutorMessage) -> Unit) =
          { message: TaskExecutorMessage -> taskExecutor.handle(message) }

      // tell clients and/or workflow engine about that failure
      val beforeDlq: (suspend (TaskExecutorMessage, Exception) -> Unit) =
          { message: TaskExecutorMessage, cause: Exception ->
            taskExecutor.sendTaskFailed(message, cause, sendingDlqMessage)
          }

      futures.add(
          consumerAsync.startWorkflowTaskConsumerAsync(
              handler = handler,
              beforeDlq = beforeDlq,
              workflowName = it.key,
              concurrency = it.value.concurrency,
          ),
      )

      // start consumer for delayed (Workflow)TaskExecutorMessage
      futures.add(
          consumerAsync.startDelayedWorkflowTaskConsumerAsync(
              handler = { message: TaskExecutorMessage -> taskExecutorDelayedProducer.send(message) },
              beforeDlq = beforeDlq,
              workflowName = it.key,
              concurrency = it.value.concurrency,
          ),
      )
    }

    // start task executors
    workerRegistry.services.forEach {
      val taskExecutor = TaskExecutor(workerRegistry, producerAsync, client)

      val handler: (suspend (TaskExecutorMessage) -> Unit) =
          { message: TaskExecutorMessage -> taskExecutor.handle(message) }

      // tell clients and/or workflow engine about that failure
      val beforeDlq: (suspend (TaskExecutorMessage, Exception) -> Unit) =
          { message: TaskExecutorMessage, cause: Exception ->
            taskExecutor.sendTaskFailed(message, cause, sendingDlqMessage)
          }

      futures.add(
          consumerAsync.startTaskExecutorConsumerAsync(
              handler = handler,
              beforeDlq = beforeDlq,
              serviceName = it.key,
              concurrency = it.value.concurrency,
          ),
      )

      // start consumer for delayed TaskExecutorMessage
      futures.add(
          consumerAsync.startDelayedTaskExecutorConsumerAsync(
              handler = { message: TaskExecutorMessage -> taskExecutorDelayedProducer.send(message) },
              beforeDlq = beforeDlq,
              serviceName = it.key,
              concurrency = it.value.concurrency,
          ),
      )
    }

    // start task tags
    workerRegistry.serviceTags.forEach {
      val tagEngine = TaskTagEngine(it.value.storage, producerAsync)

      val handler: (suspend (TaskTagMessage) -> Unit) =
          { message: TaskTagMessage -> tagEngine.handle(message) }

      // do nothing before sending message on dead letter queue
      val beforeDlq = null

      futures.add(
          consumerAsync.startTaskTagConsumerAsync(
              handler = handler,
              beforeDlq = beforeDlq,
              serviceName = it.key,
              concurrency = it.value.concurrency,
          ),
      )
    }

    logger.info { "Worker \"${producerAsync.name}\" ready" }

    return CompletableFuture.allOf(*futures.toTypedArray())
  }

  companion object {
    /** Create [InfiniticWorker] from config */
    @JvmStatic
    fun fromConfig(workerConfig: WorkerConfigInterface): InfiniticWorker = with(workerConfig) {
      val transportConfig = TransportConfig(transport, pulsar)

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
