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
import io.infinitic.clients.InfiniticClientInterface
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.tasks.tags.messages.TaskTagMessage
import io.infinitic.common.transport.InfiniticConsumer
import io.infinitic.common.transport.InfiniticProducer
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagMessage
import io.infinitic.tasks.executor.TaskExecutor
import io.infinitic.tasks.tag.TaskTagEngine
import io.infinitic.workers.config.WorkerConfig
import io.infinitic.workers.register.InfiniticRegister
import io.infinitic.workflows.engine.WorkflowEngine
import io.infinitic.workflows.tag.WorkflowTagEngine
import java.io.Closeable
import java.util.concurrent.CompletableFuture

@Suppress("unused")
class InfiniticWorker(
  private val register: InfiniticRegister,
  private val consumer: InfiniticConsumer,
  private val producer: InfiniticProducer,
  private val client: InfiniticClientInterface
) : Closeable, InfiniticRegister by register {

  private val logger = KotlinLogging.logger {}

  private val workerRegistry = register.registry

  override fun close() {
    consumer.close()
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
      val tagEngine = WorkflowTagEngine(it.value.storage, producer)

      val handler: (suspend (WorkflowTagMessage) -> Unit) =
          { message: WorkflowTagMessage -> tagEngine.handle(message) }

      futures.add(
          consumer.startWorkflowTagConsumerAsync(
              handler = handler,
              workflowName = it.key,
              concurrency = it.value.concurrency,
          ),
      )
    }

    workerRegistry.workflowEngines.forEach {
      // start workflow engines
      val workflowEngine = WorkflowEngine(it.value.storage, producer)

      val handler: (suspend (WorkflowEngineMessage) -> Unit) =
          { message: WorkflowEngineMessage -> workflowEngine.handle(message) }

      futures.add(
          consumer.startWorkflowEngineConsumerAsync(
              handler = handler,
              workflowName = it.key,
              concurrency = it.value.concurrency,
          ),
      )

      // start consumer for delayed WorkflowEngineMessage
      futures.add(
          consumer.startDelayedWorkflowEngineConsumerAsync(
              handler = { message: WorkflowEngineMessage -> producer.send(message) },
              workflowName = it.key,
              concurrency = it.value.concurrency,
          ),
      )
    }

    // start workflow task executors
    workerRegistry.workflows.forEach {
      val taskExecutor = TaskExecutor(workerRegistry, producer, client)

      val handler: (suspend (TaskExecutorMessage) -> Unit) =
          { message: TaskExecutorMessage -> taskExecutor.handle(message) }

      futures.add(
          consumer.startWorkflowTaskConsumerAsync(
              handler = handler,
              workflowName = it.key,
              concurrency = it.value.concurrency,
          ),
      )

      // start consumer for delayed (Workflow)TaskExecutorMessage
      futures.add(
          consumer.startDelayedWorkflowTaskConsumerAsync(
              handler = { message: TaskExecutorMessage -> producer.send(message) },
              workflowName = it.key,
              concurrency = it.value.concurrency,
          ),
      )
    }

    // start task executors
    workerRegistry.services.forEach {
      val taskExecutor = TaskExecutor(workerRegistry, producer, client)

      val handler: (suspend (TaskExecutorMessage) -> Unit) =
          { message: TaskExecutorMessage -> taskExecutor.handle(message) }

      futures.add(
          consumer.startTaskExecutorConsumerAsync(
              handler = handler,
              serviceName = it.key,
              concurrency = it.value.concurrency,
          ),
      )

      // start consumer for delayed TaskExecutorMessage
      futures.add(
          consumer.startDelayedTaskExecutorConsumerAsync(
              handler = { message: TaskExecutorMessage -> producer.send(message) },
              serviceName = it.key,
              concurrency = it.value.concurrency,
          ),
      )
    }

    // start task tags
    workerRegistry.serviceTags.forEach {
      val tagEngine = TaskTagEngine(it.value.storage, producer)

      val handler: (suspend (TaskTagMessage) -> Unit) =
          { message: TaskTagMessage -> tagEngine.handle(message) }

      futures.add(
          consumer.startTaskTagConsumerAsync(
              handler = handler,
              serviceName = it.key,
              concurrency = it.value.concurrency,
          ),
      )
    }

    logger.info { "Worker \"${producer.name}\" ready" }

    return CompletableFuture.allOf(*futures.toTypedArray())
  }

  companion object {
    /** Create [InfiniticWorker] from config in resources */
    @JvmStatic
    fun fromConfigResource(vararg resources: String) =
        WorkerConfig.fromResource(*resources).worker

    /** Create [InfiniticWorker] from config in system file */
    @JvmStatic
    fun fromConfigFile(vararg files: String) = WorkerConfig.fromFile(*files).worker

  }
}
