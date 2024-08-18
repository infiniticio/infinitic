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
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.tasks.tags.storage.TaskTagStorage
import io.infinitic.common.transport.InfiniticConsumerAsync
import io.infinitic.common.transport.InfiniticProducerAsync
import io.infinitic.common.transport.LoggedInfiniticProducer
import io.infinitic.common.transport.MainSubscription
import io.infinitic.common.transport.RetryServiceExecutorTopic
import io.infinitic.common.transport.RetryWorkflowTaskExecutorTopic
import io.infinitic.common.transport.ServiceEventsTopic
import io.infinitic.common.transport.ServiceExecutorTopic
import io.infinitic.common.transport.ServiceTagTopic
import io.infinitic.common.transport.SubscriptionType
import io.infinitic.common.transport.TimerWorkflowStateEngineTopic
import io.infinitic.common.transport.WorkflowCmdTopic
import io.infinitic.common.transport.WorkflowEventsTopic
import io.infinitic.common.transport.WorkflowStateEngineTopic
import io.infinitic.common.transport.WorkflowTagEngineTopic
import io.infinitic.common.transport.WorkflowTaskEventsTopic
import io.infinitic.common.transport.WorkflowTaskExecutorTopic
import io.infinitic.common.transport.create
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.messages.WorkflowCmdMessage
import io.infinitic.common.workflows.engine.storage.WorkflowStateStorage
import io.infinitic.common.workflows.tags.storage.WorkflowTagStorage
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CompletableFuture
import kotlin.system.exitProcess

@Suppress("unused")
class InfiniticWorker private constructor(
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
        InfiniticRegisterImpl.fromConfig(this)
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

  private val delayedTaskProducer =
      LoggedInfiniticProducer(TaskExecutor::class.java.name, producerAsync)

  private val delayedWorkflowProducer =
      LoggedInfiniticProducer(WorkflowEngine::class.java.name, producerAsync)

  private val workerRegistry = register.registry

  private val sendingDlqMessage = { "Unable to process message, sending to Dead Letter Queue" }

  val logMessageSentToDLQ = { message: Message?, e: Exception ->
    logger.error(e) { "Sending message to DLQ ${message ?: "(Not Deserialized)"}" }
  }

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

      workerRegistry.workflowTagEngines.forEach { (workflowName, registeredWorkflowTag) ->
        // WORKFLOW TAG ENGINE
        startWorkflowTagEngine(
            workflowName,
            registeredWorkflowTag.concurrency,
            registeredWorkflowTag.storage,
        )
      }

      workerRegistry.workflowStateEngines.forEach { (workflowName, registeredWorkflowEngine) ->
        // WORKFLOW STATE ENGINE
        startWorkflowStateEngine(
            workflowName,
            registeredWorkflowEngine.concurrency,
            registeredWorkflowEngine.storage,
        )
      }

      workerRegistry.workflowExecutors.forEach { (workflowName, registeredWorkflowExecutor) ->
        // WORKFLOW EXECUTOR
        startWorkflowExecutor(
            workflowName,
            registeredWorkflowExecutor.concurrency,
        )
      }

      workerRegistry.serviceTagEngines.forEach { (serviceName, registeredServiceTag) ->
        // SERVICE TAG ENGINE
        startServiceTagEngine(
            serviceName,
            registeredServiceTag.concurrency,
            registeredServiceTag.storage,
        )
      }

      workerRegistry.serviceExecutors.forEach { (serviceName, registeredEventListener) ->
        // SERVICE EXECUTOR
        startServiceExecutor(
            serviceName,
            registeredEventListener.concurrency,
        )
      }

      workerRegistry.workflowEventListeners.forEach { (workflowName, registeredEventListener) ->
        // WORKFLOW TASK EVENT LISTENER
        startWorkflowTaskEventConsumer(
            workflowName,
            registeredEventListener.concurrency,
            registeredEventListener.subscriptionName,
            SubscriptionType.EVENT_LISTENER,
        ) { message, publishedAt ->
          message.toServiceCloudEvent(publishedAt, source)?.let {
            registeredEventListener.eventListener.onEvent(it)
          } ?: Unit
        }

        // WORKFLOW EVENT LISTENER
        startWorkflowEventConsumer(
            workflowName,
            registeredEventListener.concurrency,
            registeredEventListener.subscriptionName,
            SubscriptionType.EVENT_LISTENER,
        ) { message, publishedAt ->
          message.toWorkflowCloudEvent(publishedAt, source)?.let {
            registeredEventListener.eventListener.onEvent(it)
          } ?: Unit
        }
      }

      workerRegistry.workflowEventLoggers.forEach { (workflowName, registeredEventLogger) ->
        // WORKFLOW TASK EVENT LOGGER
//        startWorkflowTaskEventConsumer(
//            workflowName,
//            registeredEventLogger.concurrency,
//            registeredEventLogger.subscriptionName,
//        ) { message, publishedAt ->
//          message.toServiceCloudEvent(publishedAt, source)?.let {
//            registeredEventLogger.log { it }
//          } ?: Unit
//        }

        // WORKFLOW EVENT LOGGER
        startWorkflowEventConsumer(
            workflowName,
            registeredEventLogger.concurrency,
            registeredEventLogger.subscriptionName,
            SubscriptionType.EVENT_LOGGER,
        ) { message, publishedAt ->
          message.toWorkflowCloudEvent(publishedAt, source)?.let {
            registeredEventLogger.log(it)
          } ?: Unit
        }
      }

      workerRegistry.serviceEventListeners.forEach { (serviceName, registeredEventListener) ->
        // SERVICE EVENT LISTENER
        startServiceEventConsumer(
            serviceName,
            registeredEventListener.concurrency,
            registeredEventListener.subscriptionName,
            SubscriptionType.EVENT_LISTENER,
        ) { message: Message, publishedAt: MillisInstant ->
          message.toServiceCloudEvent(publishedAt, source)?.let {
            registeredEventListener.eventListener.onEvent(it)
          } ?: Unit
        }
      }

      workerRegistry.serviceEventLoggers.forEach { (serviceName, registeredEventLogger) ->
        // SERVICE EVENT LOGGER
        startServiceEventConsumer(
            serviceName,
            registeredEventLogger.concurrency,
            registeredEventLogger.subscriptionName,
            SubscriptionType.EVENT_LOGGER,
        ) { message: Message, publishedAt: MillisInstant ->
          message.toServiceCloudEvent(publishedAt, source)?.let { registeredEventLogger.log(it) }
            ?: Unit
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



  private fun CoroutineScope.startWorkflowTagEngine(
    workflowName: WorkflowName,
    concurrency: Int,
    storage: WorkflowTagStorage
  ) {
    // WORKFLOW-TAG
    launch {
      val workflowTagEngine = WorkflowTagEngine(storage, producerAsync)
      consumerAsync.start(
          subscription = MainSubscription(WorkflowTagEngineTopic),
          entity = workflowName.toString(),
          handler = workflowTagEngine::handle,
          beforeDlq = logMessageSentToDLQ,
          concurrency = concurrency,
      )
    }
  }

  private fun CoroutineScope.startWorkflowStateEngine(
    workflowName: WorkflowName,
    concurrency: Int,
    storage: WorkflowStateStorage
  ) {
    // WORKFLOW-CMD
    launch {
      val workflowCmdHandler = WorkflowCmdHandler(producerAsync)
      consumerAsync.start(
          subscription = MainSubscription(WorkflowCmdTopic),
          entity = workflowName.toString(),
          handler = workflowCmdHandler::handle,
          beforeDlq = logMessageSentToDLQ,
          concurrency = concurrency,
      )
    }

    // WORKFLOW-STATE-ENGINE
    launch {
      val workflowEngine = WorkflowEngine(storage, producerAsync)
      consumerAsync.start(
          subscription = MainSubscription(WorkflowStateEngineTopic),
          entity = workflowName.toString(),
          handler = workflowEngine::handle,
          beforeDlq = logMessageSentToDLQ,
          concurrency = concurrency,
      )
    }

    // WORKFLOW-DELAY
    launch {
      consumerAsync.start(
          subscription = MainSubscription(TimerWorkflowStateEngineTopic),
          entity = workflowName.toString(),
          handler = { msg, _ ->
            with(delayedWorkflowProducer) { msg.sendTo(WorkflowStateEngineTopic) }
          },
          beforeDlq = logMessageSentToDLQ,
          concurrency = concurrency,
      )
    }

    // WORKFLOW-EVENTS
    launch {
      val workflowEventHandler = WorkflowEventHandler(producerAsync)
      consumerAsync.start(
          subscription = MainSubscription(WorkflowEventsTopic),
          entity = workflowName.toString(),
          handler = workflowEventHandler::handle,
          beforeDlq = logMessageSentToDLQ,
          concurrency = concurrency,
      )
    }
  }

  private fun CoroutineScope.startWorkflowExecutor(
    workflowName: WorkflowName,
    concurrency: Int
  ) {
    // WORKFLOW-TASK_EXECUTOR
    launch {
      val workflowTaskExecutor = TaskExecutor(workerRegistry, producerAsync, client)
      consumerAsync.start(
          subscription = MainSubscription(WorkflowTaskExecutorTopic),
          entity = workflowName.toString(),
          handler = workflowTaskExecutor::handle,
          beforeDlq = { message, cause ->
            when (message) {
              null -> Unit
              is ExecuteTask -> {
                logMessageSentToDLQ(message, cause)
                with(workflowTaskExecutor) {
                  message.sendTaskFailed(cause, Task.meta, sendingDlqMessage)
                }
              }
            }
          },
          concurrency = concurrency,
      )
    }

    // WORKFLOW-TASK_EXECUTOR-DELAY
    launch {
      consumerAsync.start(
          subscription = MainSubscription(RetryWorkflowTaskExecutorTopic),
          entity = workflowName.toString(),
          handler = { msg, _ ->
            with(delayedTaskProducer) { msg.sendTo(WorkflowTaskExecutorTopic) }
          },
          beforeDlq = logMessageSentToDLQ,
          concurrency = concurrency,
      )
    }

    // WORKFLOW-TASK-EVENT
    launch {
      val workflowTaskEventHandler = TaskEventHandler(producerAsync)
      consumerAsync.start(
          subscription = MainSubscription(WorkflowTaskEventsTopic),
          entity = workflowName.toString(),
          handler = workflowTaskEventHandler::handle,
          beforeDlq = logMessageSentToDLQ,
          concurrency = concurrency,
      )
    }
  }

  private fun CoroutineScope.startServiceTagEngine(
    serviceName: ServiceName,
    concurrency: Int,
    storage: TaskTagStorage
  ) {
    // TASK-TAG
    launch {
      val tagEngine = TaskTagEngine(storage, producerAsync)
      consumerAsync.start(
          subscription = MainSubscription(ServiceTagTopic),
          entity = serviceName.toString(),
          handler = tagEngine::handle,
          beforeDlq = logMessageSentToDLQ,
          concurrency = concurrency,
      )
    }
  }

  private fun CoroutineScope.startServiceExecutor(
    serviceName: ServiceName,
    concurrency: Int
  ) {
    // TASK-EXECUTOR
    launch {
      val taskExecutor = TaskExecutor(workerRegistry, producerAsync, client)
      consumerAsync.start(
          subscription = MainSubscription(ServiceExecutorTopic),
          entity = serviceName.toString(),
          handler = taskExecutor::handle,
          beforeDlq = { message, cause ->
            when (message) {
              null -> Unit
              is ExecuteTask -> {
                logMessageSentToDLQ(message, cause)
                with(taskExecutor) {
                  message.sendTaskFailed(cause, Task.meta, sendingDlqMessage)
                }
              }
            }
          },
          concurrency = concurrency,
      )
    }

    // TASK-EXECUTOR-DELAY
    launch {
      consumerAsync.start(
          subscription = MainSubscription(RetryServiceExecutorTopic),
          entity = serviceName.toString(),
          handler = { msg, _ -> with(delayedTaskProducer) { msg.sendTo(ServiceExecutorTopic) } },
          beforeDlq = logMessageSentToDLQ,
          concurrency = concurrency,
      )
    }

    // TASK-EVENTS
    launch {
      val taskEventHandler = TaskEventHandler(producerAsync)
      consumerAsync.start(
          subscription = MainSubscription(ServiceEventsTopic),
          entity = serviceName.toString(),
          handler = taskEventHandler::handle,
          beforeDlq = logMessageSentToDLQ,
          concurrency = concurrency,
      )
    }
  }

  private fun CoroutineScope.startServiceEventConsumer(
    serviceName: ServiceName,
    concurrency: Int,
    subscriptionName: String?,
    subscriptionType: SubscriptionType,
    consumer: (Message, MillisInstant) -> Unit
  ) {
    // TASK-EXECUTOR topic
    launch {
      consumerAsync.start(
          subscription = subscriptionType.create(ServiceExecutorTopic, subscriptionName),
          entity = serviceName.toString(),
          handler = consumer,
          beforeDlq = logMessageSentToDLQ,
          concurrency = concurrency,
      )
    }
    // TASK-EXECUTOR-DELAY topic
    launch {
      consumerAsync.start(
          subscription = subscriptionType.create(RetryServiceExecutorTopic, subscriptionName),
          entity = serviceName.toString(),
          handler = consumer,
          beforeDlq = logMessageSentToDLQ,
          concurrency = concurrency,
      )
    }
    // TASK-EVENTS topic
    launch {
      consumerAsync.start(
          subscription = subscriptionType.create(ServiceEventsTopic, subscriptionName),
          entity = serviceName.toString(),
          handler = consumer,
          beforeDlq = logMessageSentToDLQ,
          concurrency = concurrency,
      )
    }
  }

  private fun CoroutineScope.startWorkflowTaskEventConsumer(
    workflowName: WorkflowName,
    concurrency: Int,
    subscriptionName: String?,
    subscriptionType: SubscriptionType,
    consumer: (Message, MillisInstant) -> Unit
  ) {
    // WORKFLOW-TASK-EXECUTOR topic
    launch {
      consumerAsync.start(
          subscription = subscriptionType.create(WorkflowTaskExecutorTopic, subscriptionName),
          entity = workflowName.toString(),
          handler = consumer,
          beforeDlq = logMessageSentToDLQ,
          concurrency = concurrency,
      )
    }
    // WORKFLOW-TASK-EXECUTOR-DELAY topic
    launch {
      consumerAsync.start(
          subscription = subscriptionType.create(RetryWorkflowTaskExecutorTopic, subscriptionName),
          entity = workflowName.toString(),
          handler = consumer,
          beforeDlq = logMessageSentToDLQ,
          concurrency = concurrency,
      )
    }
    // WORKFLOW-TASK-EVENTS topic
    launch {
      consumerAsync.start(
          subscription = subscriptionType.create(WorkflowTaskEventsTopic, subscriptionName),
          entity = workflowName.toString(),
          handler = consumer,
          beforeDlq = logMessageSentToDLQ,
          concurrency = concurrency,
      )
    }
  }

  private fun CoroutineScope.startWorkflowEventConsumer(
    workflowName: WorkflowName,
    concurrency: Int,
    subscriptionName: String?,
    subscriptionType: SubscriptionType,
    consumer: (Message, MillisInstant) -> Unit
  ) {
    // WORKFLOW-CMD topic
    launch {
      consumerAsync.start(
          subscription = subscriptionType.create(WorkflowCmdTopic, subscriptionName),
          entity = workflowName.toString(),
          handler = consumer,
          beforeDlq = logMessageSentToDLQ,
          concurrency = concurrency,
      )
    }
    // WORKFLOW-STATE-ENGINE topic
    launch {
      consumerAsync.start(
          subscription = subscriptionType.create(WorkflowStateEngineTopic, subscriptionName),
          entity = workflowName.toString(),
          handler = { message: Message, publishedAt: MillisInstant ->
            // the event handler is not applied for WorkflowCmdMessage from clients
            // as the event has already been handled in the workflow-cmd topic
            if (message !is WorkflowCmdMessage) consumer(message, publishedAt)
          },
          beforeDlq = logMessageSentToDLQ,
          concurrency = concurrency,
      )
    }
    // WORKFLOW-EVENTS topic
    launch {
      consumerAsync.start(
          subscription = subscriptionType.create(WorkflowEventsTopic, subscriptionName),
          entity = workflowName.toString(),
          handler = consumer,
          beforeDlq = logMessageSentToDLQ,
          concurrency = concurrency,
      )
    }
  }
}
