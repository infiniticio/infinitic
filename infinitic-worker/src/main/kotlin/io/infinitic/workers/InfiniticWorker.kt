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
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.messages.Message
import io.infinitic.common.transport.DelayedServiceExecutorTopic
import io.infinitic.common.transport.DelayedWorkflowEngineTopic
import io.infinitic.common.transport.DelayedWorkflowTaskExecutorTopic
import io.infinitic.common.transport.InfiniticConsumerAsync
import io.infinitic.common.transport.InfiniticProducerAsync
import io.infinitic.common.transport.ListenerSubscription
import io.infinitic.common.transport.LoggedInfiniticProducer
import io.infinitic.common.transport.MainSubscription
import io.infinitic.common.transport.ServiceEventsTopic
import io.infinitic.common.transport.ServiceExecutorTopic
import io.infinitic.common.transport.ServiceTagTopic
import io.infinitic.common.transport.WorkflowCmdTopic
import io.infinitic.common.transport.WorkflowEngineTopic
import io.infinitic.common.transport.WorkflowEventsTopic
import io.infinitic.common.transport.WorkflowTagTopic
import io.infinitic.common.transport.WorkflowTaskEventsTopic
import io.infinitic.common.transport.WorkflowTaskExecutorTopic
import io.infinitic.common.workflows.engine.messages.WorkflowCmdMessage
import io.infinitic.events.toServiceCloudEvent
import io.infinitic.events.toWorkflowCloudEvent
import io.infinitic.pulsar.PulsarInfiniticConsumerAsync
import io.infinitic.tasks.Task
import io.infinitic.tasks.executor.TaskEventHandler
import io.infinitic.tasks.executor.TaskExecutor
import io.infinitic.tasks.tag.TaskTagEngine
import io.infinitic.transport.config.TransportConfig
import io.infinitic.workers.config.WorkerConfig
import io.infinitic.workers.config.WorkerConfigInterface
import io.infinitic.workers.register.InfiniticRegister
import io.infinitic.workers.register.InfiniticRegisterImpl
import io.infinitic.workflows.engine.WorkflowCmdHandler
import io.infinitic.workflows.engine.WorkflowEngine
import io.infinitic.workflows.engine.WorkflowEventHandler
import io.infinitic.workflows.tag.WorkflowTagEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CompletableFuture
import kotlin.system.exitProcess

@Suppress("unused")
class InfiniticWorker(
  val register: InfiniticRegister,
  val consumerAsync: InfiniticConsumerAsync,
  val producerAsync: InfiniticProducerAsync,
  val source: String
) : AutoCloseable, InfiniticRegister by register {

  private val logger = KotlinLogging.logger {}

  /** Infinitic Client */
  val client = InfiniticClient(consumerAsync, producerAsync)

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
    client.close()
    autoClose()
  }


  /**
   * Start worker synchronously
   * (blocks the current thread)
   */
  fun start(): Unit = try {
    startAsync().get()
  } catch (e: Throwable) {
    logger.error(e) { "Exiting" }
    // this will trigger the shutdown hook
    exitProcess(1)
  }

  /**
   * Start worker asynchronously
   */
  fun startAsync(): CompletableFuture<Unit> {
    runBlocking(Dispatchers.IO) {

      val logMessageSentToDLQ = { message: Message?, e: Exception ->
        logger.error(e) { "Sending message to DLQ ${message ?: "(Not Deserialized)"}" }
      }

      workerRegistry.workflowTags.forEach {
        // WORKFLOW-TAG
        launch {
          val workflowTagEngine = WorkflowTagEngine(it.value.storage, producerAsync)
          consumerAsync.start(
              subscription = MainSubscription(WorkflowTagTopic),
              entity = it.key.toString(),
              handler = workflowTagEngine::handle,
              beforeDlq = logMessageSentToDLQ,
              concurrency = it.value.concurrency,
          )
        }
      }

      workerRegistry.workflowEngines.forEach {
        // WORKFLOW-CMD
        launch {
          val workflowCmdHandler = WorkflowCmdHandler(producerAsync)
          consumerAsync.start(
              subscription = MainSubscription(WorkflowCmdTopic),
              entity = it.key.toString(),
              handler = workflowCmdHandler::handle,
              beforeDlq = logMessageSentToDLQ,
              concurrency = it.value.concurrency,
          )
        }

        // WORKFLOW-ENGINE
        launch {
          val workflowEngine = WorkflowEngine(it.value.storage, producerAsync)
          consumerAsync.start(
              subscription = MainSubscription(WorkflowEngineTopic),
              entity = it.key.toString(),
              handler = workflowEngine::handle,
              beforeDlq = logMessageSentToDLQ,
              concurrency = it.value.concurrency,
          )
        }

        // WORKFLOW-DELAY
        launch {
          consumerAsync.start(
              subscription = MainSubscription(DelayedWorkflowEngineTopic),
              entity = it.key.toString(),
              handler = { msg, _ ->
                with(delayedWorkflowProducer) { msg.sendTo(WorkflowEngineTopic) }
              },
              beforeDlq = logMessageSentToDLQ,
              concurrency = it.value.concurrency,
          )
        }

        // WORKFLOW-EVENTS
        launch {
          val workflowEventHandler = WorkflowEventHandler(producerAsync)
          consumerAsync.start(
              subscription = MainSubscription(WorkflowEventsTopic),
              entity = it.key.toString(),
              handler = workflowEventHandler::handle,
              beforeDlq = logMessageSentToDLQ,
              concurrency = it.value.concurrency,
          )
        }
      }

      workerRegistry.workflowExecutors.forEach {
        // WORKFLOW-TASK_EXECUTOR
        launch {
          val workflowTaskExecutor = TaskExecutor(workerRegistry, producerAsync, client)
          consumerAsync.start(
              subscription = MainSubscription(WorkflowTaskExecutorTopic),
              entity = it.key.toString(),
              handler = workflowTaskExecutor::handle,
              beforeDlq = { message, cause ->
                logMessageSentToDLQ(message, cause)
                message?.let {
                  workflowTaskExecutor.sendTaskFailed(it, cause, Task.meta, sendingDlqMessage)
                }
              },
              concurrency = it.value.concurrency,
          )
        }

        // WORKFLOW-TASK_EXECUTOR-DELAY
        launch {
          consumerAsync.start(
              subscription = MainSubscription(DelayedWorkflowTaskExecutorTopic),
              entity = it.key.toString(),
              handler = { msg, _ ->
                with(delayedTaskProducer) { msg.sendTo(WorkflowTaskExecutorTopic) }
              },
              beforeDlq = logMessageSentToDLQ,
              concurrency = it.value.concurrency,
          )
        }

        // WORKFLOW-TASK-EVENT
        launch {
          val workflowTaskEventHandler = TaskEventHandler(producerAsync)
          consumerAsync.start(
              subscription = MainSubscription(WorkflowTaskEventsTopic),
              entity = it.key.toString(),
              handler = workflowTaskEventHandler::handle,
              beforeDlq = logMessageSentToDLQ,
              concurrency = it.value.concurrency,
          )
        }
      }

      workerRegistry.serviceTags.forEach {
        // TASK-TAG
        launch {
          val tagEngine = TaskTagEngine(it.value.storage, producerAsync)
          consumerAsync.start(
              subscription = MainSubscription(ServiceTagTopic),
              entity = it.key.toString(),
              handler = tagEngine::handle,
              beforeDlq = logMessageSentToDLQ,
              concurrency = it.value.concurrency,
          )
        }
      }

      workerRegistry.serviceExecutors.forEach {
        // TASK-EXECUTOR
        launch {
          val taskExecutor = TaskExecutor(workerRegistry, producerAsync, client)
          consumerAsync.start(
              subscription = MainSubscription(ServiceExecutorTopic),
              entity = it.key.toString(),
              handler = taskExecutor::handle,
              beforeDlq = { message, cause ->
                logMessageSentToDLQ(message, cause)
                message?.let {
                  taskExecutor.sendTaskFailed(it, cause, Task.meta, sendingDlqMessage)
                }
              },
              concurrency = it.value.concurrency,
          )
        }

        // TASK-EXECUTOR-DELAY
        launch {
          consumerAsync.start(
              subscription = MainSubscription(DelayedServiceExecutorTopic),
              entity = it.key.toString(),
              handler = { msg, _ -> with(delayedTaskProducer) { msg.sendTo(ServiceExecutorTopic) } },
              beforeDlq = logMessageSentToDLQ,
              concurrency = it.value.concurrency,
          )
        }

        // TASK-EVENTS
        launch {
          val taskEventHandler = TaskEventHandler(producerAsync)
          consumerAsync.start(
              subscription = MainSubscription(ServiceEventsTopic),
              entity = it.key.toString(),
              handler = taskEventHandler::handle,
              beforeDlq = logMessageSentToDLQ,
              concurrency = it.value.concurrency,
          )
        }
      }

      workerRegistry.serviceListeners.forEach { (serviceName, registeredEventListener) ->
        val eventHandler = { message: Message, publishedAt: MillisInstant ->
          message.toServiceCloudEvent(publishedAt, source)?.let {
            registeredEventListener.eventListener.onEvent(it)
          } ?: Unit
        }
        val subscriptionName = registeredEventListener.subscriptionName
        // TASK-EXECUTOR topic
        launch {
          consumerAsync.start(
              subscription = ListenerSubscription(ServiceExecutorTopic, subscriptionName),
              entity = serviceName.toString(),
              handler = eventHandler,
              beforeDlq = logMessageSentToDLQ,
              concurrency = registeredEventListener.concurrency,
          )
        }
        // TASK-EXECUTOR-DELAY topic
        launch {
          consumerAsync.start(
              subscription = ListenerSubscription(DelayedServiceExecutorTopic, subscriptionName),
              entity = serviceName.toString(),
              handler = eventHandler,
              beforeDlq = logMessageSentToDLQ,
              concurrency = registeredEventListener.concurrency,
          )
        }
        // TASK-EVENTS topic
        launch {
          consumerAsync.start(
              subscription = ListenerSubscription(ServiceEventsTopic, subscriptionName),
              entity = serviceName.toString(),
              handler = eventHandler,
              beforeDlq = logMessageSentToDLQ,
              concurrency = registeredEventListener.concurrency,
          )
        }
      }

      workerRegistry.workflowListeners.forEach { (workflowName, registeredEventListener) ->
        val serviceEventHandler = { message: Message, publishedAt: MillisInstant ->
          message.toServiceCloudEvent(publishedAt, source)?.let {
            registeredEventListener.eventListener.onEvent(it)
          } ?: Unit
        }
        val workflowEventHandler = { message: Message, publishedAt: MillisInstant ->
          message.toWorkflowCloudEvent(publishedAt, source)?.let {
            registeredEventListener.eventListener.onEvent(it)
          } ?: Unit
        }
        val subscriptionName = registeredEventListener.subscriptionName
        // WORKFLOW-TASK-EXECUTOR topic
        launch {
          consumerAsync.start(
              subscription = ListenerSubscription(WorkflowTaskExecutorTopic, subscriptionName),
              entity = workflowName.toString(),
              handler = serviceEventHandler,
              beforeDlq = logMessageSentToDLQ,
              concurrency = registeredEventListener.concurrency,
          )
        }
        // WORKFLOW-TASK-EXECUTOR-DELAY topic
        launch {
          consumerAsync.start(
              subscription = ListenerSubscription(
                  DelayedWorkflowTaskExecutorTopic,
                  subscriptionName,
              ),
              entity = workflowName.toString(),
              handler = serviceEventHandler,
              beforeDlq = logMessageSentToDLQ,
              concurrency = registeredEventListener.concurrency,
          )
        }
        // WORKFLOW-TASK-EVENTS topic
        launch {
          consumerAsync.start(
              subscription = ListenerSubscription(WorkflowTaskEventsTopic, subscriptionName),
              entity = workflowName.toString(),
              handler = serviceEventHandler,
              beforeDlq = logMessageSentToDLQ,
              concurrency = registeredEventListener.concurrency,
          )
        }
        // WORKFLOW-CMD topic
        launch {
          consumerAsync.start(
              subscription = ListenerSubscription(WorkflowCmdTopic, subscriptionName),
              entity = workflowName.toString(),
              handler = workflowEventHandler,
              beforeDlq = logMessageSentToDLQ,
              concurrency = registeredEventListener.concurrency,
          )
        }
        // WORKFLOW-ENGINE topic
        launch {
          consumerAsync.start(
              subscription = ListenerSubscription(WorkflowEngineTopic, subscriptionName),
              entity = workflowName.toString(),
              handler = { message: Message, publishedAt: MillisInstant ->
                // the event handler is not applied for WorkflowCmdMessage from clients
                // as the event has already been handled in the workflow-cmd topic
                if (message !is WorkflowCmdMessage) workflowEventHandler(message, publishedAt)
              },
              beforeDlq = logMessageSentToDLQ,
              concurrency = registeredEventListener.concurrency,
          )
        }
        // WORKFLOW-EVENTS topic
        launch {
          consumerAsync.start(
              subscription = ListenerSubscription(WorkflowEventsTopic, subscriptionName),
              entity = workflowName.toString(),
              handler = workflowEventHandler,
              beforeDlq = logMessageSentToDLQ,
              concurrency = registeredEventListener.concurrency,
          )
        }
      }
    }

    logger.info {
      "Worker \"${producerAsync.producerName}\" ready" + when (consumerAsync is PulsarInfiniticConsumerAsync) {
        true -> " (shutdownGracePeriodInSeconds=${consumerAsync.shutdownGracePeriodInSeconds}s)"
        false -> ""
      }
    }

    return CompletableFuture.supplyAsync { consumerAsync.join() }
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

      // set name, if it exists in the worker configuration
      name?.let { producerAsync.producerName = it }

      /** Infinitic Register */
      // if an exception is thrown, we ensure to close the previously created resource
      val register = try {
        InfiniticRegisterImpl.fromConfig(this).apply {
          logName = InfiniticWorker::class.java.name
        }
      } catch (e: Exception) {
        consumerAsync.close()
        throw e
      }

      /** Infinitic Worker */
      InfiniticWorker(
          register,
          consumerAsync,
          producerAsync,
          transportConfig.source,
      ).also {
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

    /** Create [InfiniticWorker] from yaml strings */
    @JvmStatic
    fun fromConfigYaml(vararg yamls: String): InfiniticWorker =
        fromConfig(WorkerConfig.fromYaml(*yamls))
  }
}
