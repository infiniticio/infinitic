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

import io.cloudevents.CloudEvent
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.autoclose.addAutoCloseResource
import io.infinitic.autoclose.autoClose
import io.infinitic.clients.InfiniticClient
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.logs.SERVICE_EXECUTOR_CLOUD_EVENTS
import io.infinitic.common.logs.SERVICE_TAG_ENGINE_CLOUD_EVENTS
import io.infinitic.common.logs.WORKFLOW_EXECUTOR_CLOUD_EVENTS
import io.infinitic.common.logs.WORKFLOW_STATE_ENGINE_CLOUD_EVENTS
import io.infinitic.common.logs.WORKFLOW_TAG_ENGINE_CLOUD_EVENTS
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.events.messages.ServiceExecutorEventMessage
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.tasks.tags.messages.ServiceTagMessage
import io.infinitic.common.tasks.tags.storage.TaskTagStorage
import io.infinitic.common.transport.InfiniticConsumer
import io.infinitic.common.transport.InfiniticProducerAsync
import io.infinitic.common.transport.LoggedInfiniticConsumer
import io.infinitic.common.transport.LoggedInfiniticProducer
import io.infinitic.common.transport.MainSubscription
import io.infinitic.common.transport.RetryServiceExecutorTopic
import io.infinitic.common.transport.RetryWorkflowExecutorTopic
import io.infinitic.common.transport.ServiceExecutorEventTopic
import io.infinitic.common.transport.ServiceExecutorTopic
import io.infinitic.common.transport.ServiceTagEngineTopic
import io.infinitic.common.transport.SubscriptionType
import io.infinitic.common.transport.WorkflowExecutorEventTopic
import io.infinitic.common.transport.WorkflowExecutorTopic
import io.infinitic.common.transport.WorkflowStateCmdTopic
import io.infinitic.common.transport.WorkflowStateEngineTopic
import io.infinitic.common.transport.WorkflowStateEventTopic
import io.infinitic.common.transport.WorkflowStateTimerTopic
import io.infinitic.common.transport.WorkflowTagEngineTopic
import io.infinitic.common.transport.create
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.messages.WorkflowCmdMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEventMessage
import io.infinitic.common.workflows.engine.messages.WorkflowStateEngineMessage
import io.infinitic.common.workflows.engine.storage.WorkflowStateStorage
import io.infinitic.common.workflows.tags.messages.WorkflowTagEngineMessage
import io.infinitic.common.workflows.tags.storage.WorkflowTagStorage
import io.infinitic.events.toJsonString
import io.infinitic.events.toServiceCloudEvent
import io.infinitic.events.toWorkflowCloudEvent
import io.infinitic.logger.ignoreNull
import io.infinitic.pulsar.PulsarInfiniticConsumer
import io.infinitic.tasks.Task
import io.infinitic.tasks.executor.TaskEventHandler
import io.infinitic.tasks.executor.TaskExecutor
import io.infinitic.tasks.executor.TaskRetriedHandler
import io.infinitic.tasks.tag.TaskTagEngine
import io.infinitic.tasks.tag.storage.LoggedTaskTagStorage
import io.infinitic.transport.config.TransportConfig
import io.infinitic.workers.config.WorkerConfig
import io.infinitic.workers.config.WorkerConfigInterface
import io.infinitic.workers.register.InfiniticRegister
import io.infinitic.workers.register.InfiniticRegisterImpl
import io.infinitic.workflows.engine.WorkflowStateCmdHandler
import io.infinitic.workflows.engine.WorkflowStateEngine
import io.infinitic.workflows.engine.WorkflowStateEventHandler
import io.infinitic.workflows.engine.WorkflowStateTimerHandler
import io.infinitic.workflows.engine.storage.LoggedWorkflowStateStorage
import io.infinitic.workflows.tag.WorkflowTagEngine
import io.infinitic.workflows.tag.storage.LoggedWorkflowTagStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CompletableFuture
import kotlin.system.exitProcess

@Suppress("unused")
class InfiniticWorker private constructor(
  val register: InfiniticRegister,
  val consumerAsync: InfiniticConsumer,
  val producerAsync: InfiniticProducerAsync,
  val source: String
) : AutoCloseable, InfiniticRegister by register {

  private val logger = KotlinLogging.logger {}

  /** Infinitic Client */
  val client = InfiniticClient(consumerAsync, producerAsync)

  init {
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
        startTaskTagEngine(
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
        startWorkflowExecutorEventListener(
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
        startWorkflowStateEventListener(
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

      workerRegistry.serviceEventListeners.forEach { (serviceName, registeredEventListener) ->
        // SERVICE EVENT LISTENER
        startServiceEventListener(
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
    }

    logger.info {
      "Worker \"${producerAsync.producerName}\" ready" + when (consumerAsync is PulsarInfiniticConsumer) {
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
    val eventLogger = KotlinLogging.logger("$WORKFLOW_TAG_ENGINE_CLOUD_EVENTS.$workflowName")
        .ignoreNull()

    // WORKFLOW-TAG
    launch {
      val engineLogger = KotlinLogging.logger(WorkflowTagEngine::class.java.name)
      val loggedStorage = LoggedWorkflowTagStorage(engineLogger, storage)
      val loggedConsumer = LoggedInfiniticConsumer(engineLogger, consumerAsync)
      val loggedProducer = LoggedInfiniticProducer(engineLogger, producerAsync)

      val workflowTagEngine = WorkflowTagEngine(loggedStorage, loggedProducer)

      val handler: suspend (WorkflowTagEngineMessage, MillisInstant) -> Unit =
          { message, publishedAt ->
            eventLogger.logWorkflowCloudEvent(message, publishedAt, source)
            workflowTagEngine.handle(message, publishedAt)
          }

      loggedConsumer.start(
          subscription = MainSubscription(WorkflowTagEngineTopic),
          entity = workflowName.toString(),
          handler = handler,
          beforeDlq = null,
          concurrency = concurrency,
      )
    }
  }

  private fun CoroutineScope.startWorkflowStateEngine(
    workflowName: WorkflowName,
    concurrency: Int,
    storage: WorkflowStateStorage
  ) {
    val eventLogger = KotlinLogging.logger("$WORKFLOW_STATE_ENGINE_CLOUD_EVENTS.$workflowName")
        .ignoreNull()
    // WORKFLOW-CMD
    launch {
      val cmdHandlerLogger = KotlinLogging.logger(WorkflowStateCmdHandler::class.java.name)
      val loggedConsumer = LoggedInfiniticConsumer(cmdHandlerLogger, consumerAsync)
      val loggedProducer = LoggedInfiniticProducer(cmdHandlerLogger, producerAsync)
      val workflowStateCmdHandler = WorkflowStateCmdHandler(loggedProducer)

      val handler: suspend (WorkflowStateEngineMessage, MillisInstant) -> Unit =
          { message, publishedAt ->
            eventLogger.logWorkflowCloudEvent(message, publishedAt, source)
            workflowStateCmdHandler.handle(message, publishedAt)
          }

      loggedConsumer.start(
          subscription = MainSubscription(WorkflowStateCmdTopic),
          entity = workflowName.toString(),
          handler = handler,
          beforeDlq = null,
          concurrency = concurrency,
      )
    }

    // WORKFLOW-STATE-ENGINE
    launch {
      val engineLogger = KotlinLogging.logger(WorkflowStateEngine::class.java.name)
      val loggedStorage = LoggedWorkflowStateStorage(engineLogger, storage)
      val loggedConsumer = LoggedInfiniticConsumer(engineLogger, consumerAsync)
      val loggedProducer = LoggedInfiniticProducer(engineLogger, producerAsync)
      val workflowStateEngine = WorkflowStateEngine(loggedStorage, loggedProducer)

      val handler: suspend (WorkflowStateEngineMessage, MillisInstant) -> Unit =
          { message, publishedAt ->
            if (message !is WorkflowCmdMessage) {
              eventLogger.logWorkflowCloudEvent(message, publishedAt, source)
            }
            workflowStateEngine.handle(message, publishedAt)
          }

      loggedConsumer.start(
          subscription = MainSubscription(WorkflowStateEngineTopic),
          entity = workflowName.toString(),
          handler = handler,
          beforeDlq = null,
          concurrency = concurrency,
      )
    }

    // WORKFLOW TIMERS
    launch {
      val timerLogger = KotlinLogging.logger(WorkflowStateTimerHandler::class.java.name)
      val loggedConsumer = LoggedInfiniticConsumer(timerLogger, consumerAsync)
      val loggedProducer = LoggedInfiniticProducer(timerLogger, producerAsync)
      val workflowStateTimerHandler = WorkflowStateTimerHandler(loggedProducer)

      // we do not use loggedConsumer to avoid logging twice the messages coming from delayed topics
      loggedConsumer.start(
          subscription = MainSubscription(WorkflowStateTimerTopic),
          entity = workflowName.toString(),
          handler = workflowStateTimerHandler::handle,
          beforeDlq = null,
          concurrency = concurrency,
      )
    }

    // WORKFLOW-EVENTS
    launch {
      val eventHandlerLogger = KotlinLogging.logger(WorkflowStateEventHandler::class.java.name)
      val loggedConsumer = LoggedInfiniticConsumer(eventHandlerLogger, consumerAsync)
      val loggedProducer = LoggedInfiniticProducer(eventHandlerLogger, producerAsync)
      val workflowStateEventHandler = WorkflowStateEventHandler(loggedProducer)

      val handler: suspend (WorkflowEventMessage, MillisInstant) -> Unit =
          { message, publishedAt ->
            eventLogger.logWorkflowCloudEvent(message, publishedAt, source)
            workflowStateEventHandler.handle(message, publishedAt)
          }

      loggedConsumer.start(
          subscription = MainSubscription(WorkflowStateEventTopic),
          entity = workflowName.toString(),
          handler = handler,
          beforeDlq = null,
          concurrency = concurrency,
      )
    }
  }

  private fun CoroutineScope.startWorkflowExecutor(
    workflowName: WorkflowName,
    concurrency: Int
  ) {
    val logger = KotlinLogging.logger("$WORKFLOW_EXECUTOR_CLOUD_EVENTS.$workflowName")
        .ignoreNull()

    // WORKFLOW-TASK_EXECUTOR
    launch {
      val executorLogger = KotlinLogging.logger(TaskExecutor::class.java.name)
      val loggedConsumer = LoggedInfiniticConsumer(executorLogger, consumerAsync)
      val loggedProducer = LoggedInfiniticProducer(executorLogger, producerAsync)
      val workflowTaskExecutor = TaskExecutor(workerRegistry, loggedProducer, client)

      val handler: suspend (ServiceExecutorMessage, MillisInstant) -> Unit =
          { message, publishedAt ->
            logger.logServiceCloudEvent(message, publishedAt, source)
            workflowTaskExecutor.handle(message, publishedAt)
          }

      val beforeDlq: suspend (ServiceExecutorMessage?, Exception) -> Unit = { message, cause ->
        when (message) {
          null -> Unit
          is ExecuteTask -> with(workflowTaskExecutor) {
            message.sendTaskFailed(cause, Task.meta, sendingDlqMessage)
          }
        }
      }

      loggedConsumer.start(
          subscription = MainSubscription(WorkflowExecutorTopic),
          entity = workflowName.toString(),
          handler = handler,
          beforeDlq = beforeDlq,
          concurrency = concurrency,
      )
    }

    // WORKFLOW-TASK_EXECUTOR-DELAY
    launch {
      val retriedLogger = KotlinLogging.logger(TaskRetriedHandler::class.java.name)
      val loggedConsumer = LoggedInfiniticConsumer(retriedLogger, consumerAsync)
      val loggedProducer = LoggedInfiniticProducer(retriedLogger, producerAsync)
      val taskRetriedHandler = TaskRetriedHandler(loggedProducer)

      // we do not use loggedConsumer to avoid logging twice the messages coming from delayed topics
      loggedConsumer.start(
          subscription = MainSubscription(RetryWorkflowExecutorTopic),
          entity = workflowName.toString(),
          handler = taskRetriedHandler::handle,
          beforeDlq = { message, publishedAt -> },
          concurrency = concurrency,
      )
    }

    // WORKFLOW-TASK-EVENT
    launch {
      val taskEventLogger = KotlinLogging.logger(TaskEventHandler::class.java.name)
      val loggedConsumer = LoggedInfiniticConsumer(taskEventLogger, consumerAsync)
      val loggedProducer = LoggedInfiniticProducer(taskEventLogger, producerAsync)
      val workflowTaskEventHandler = TaskEventHandler(loggedProducer)

      val handler: suspend (ServiceExecutorEventMessage, MillisInstant) -> Unit =
          { message, publishedAt ->
            logger.logServiceCloudEvent(message, publishedAt, source)
            workflowTaskEventHandler.handle(message, publishedAt)
          }

      loggedConsumer.start(
          subscription = MainSubscription(WorkflowExecutorEventTopic),
          entity = workflowName.toString(),
          handler = handler,
          beforeDlq = null,
          concurrency = concurrency,
      )
    }
  }

  private fun CoroutineScope.startTaskTagEngine(
    serviceName: ServiceName,
    concurrency: Int,
    storage: TaskTagStorage
  ) {
    val logger = KotlinLogging.logger("$SERVICE_TAG_ENGINE_CLOUD_EVENTS.$serviceName")
        .ignoreNull()

    // TASK-TAG
    launch {
      val engineLogger = KotlinLogging.logger(TaskTagEngine::class.java.name)
      val loggedStorage = LoggedTaskTagStorage(engineLogger, storage)
      val loggedConsumer = LoggedInfiniticConsumer(engineLogger, consumerAsync)
      val loggedProducer = LoggedInfiniticProducer(engineLogger, producerAsync)
      val taskTagEngine = TaskTagEngine(loggedStorage, loggedProducer)

      val handler: suspend (ServiceTagMessage, MillisInstant) -> Unit =
          { message, publishedAt ->
            logger.logServiceCloudEvent(message, publishedAt, source)
            taskTagEngine.handle(message, publishedAt)
          }

      loggedConsumer.start(
          subscription = MainSubscription(ServiceTagEngineTopic),
          entity = serviceName.toString(),
          handler = handler,
          beforeDlq = null,
          concurrency = concurrency,
      )
    }
  }

  private fun CoroutineScope.startServiceExecutor(
    serviceName: ServiceName,
    concurrency: Int
  ) {
    val logger = KotlinLogging.logger("$SERVICE_EXECUTOR_CLOUD_EVENTS.$serviceName")
        .ignoreNull()

    // TASK-EXECUTOR
    launch {
      val taskExecutorLogger = KotlinLogging.logger(TaskExecutor::class.java.name)
      val loggedConsumer = LoggedInfiniticConsumer(taskExecutorLogger, consumerAsync)
      val loggedProducer = LoggedInfiniticProducer(taskExecutorLogger, producerAsync)
      val taskExecutor = TaskExecutor(workerRegistry, loggedProducer, client)

      val handler: suspend (ServiceExecutorMessage, MillisInstant) -> Unit =
          { message, publishedAt ->
            logger.logServiceCloudEvent(message, publishedAt, source)
            taskExecutor.handle(message, publishedAt)
          }

      val beforeDlq: suspend (ServiceExecutorMessage?, Exception) -> Unit = { message, cause ->
        when (message) {
          null -> Unit
          is ExecuteTask -> with(taskExecutor) {
            message.sendTaskFailed(cause, Task.meta, sendingDlqMessage)
          }
        }
      }

      loggedConsumer.start(
          subscription = MainSubscription(ServiceExecutorTopic),
          entity = serviceName.toString(),
          handler = handler,
          beforeDlq = beforeDlq,
          concurrency = concurrency,
      )
    }

    // TASK-EXECUTOR-DELAY
    launch {
      val retriedLogger = KotlinLogging.logger(TaskRetriedHandler::class.java.name)
      val loggedConsumer = LoggedInfiniticConsumer(retriedLogger, consumerAsync)
      val loggedProducer = LoggedInfiniticProducer(retriedLogger, producerAsync)
      val taskRetriedHandler = TaskRetriedHandler(loggedProducer)

      loggedConsumer.start(
          subscription = MainSubscription(RetryServiceExecutorTopic),
          entity = serviceName.toString(),
          handler = taskRetriedHandler::handle,
          beforeDlq = null,
          concurrency = concurrency,
      )
    }

    // TASK-EVENTS
    launch {
      val eventHandlerLogger = KotlinLogging.logger(TaskEventHandler::class.java.name)
      val loggedConsumer = LoggedInfiniticConsumer(eventHandlerLogger, consumerAsync)
      val loggedProducer = LoggedInfiniticProducer(eventHandlerLogger, producerAsync)
      val taskEventHandler = TaskEventHandler(loggedProducer)

      val handler: suspend (ServiceExecutorEventMessage, MillisInstant) -> Unit =
          { message, publishedAt ->
            logger.logServiceCloudEvent(message, publishedAt, source)
            taskEventHandler.handle(message, publishedAt)
          }

      loggedConsumer.start(
          subscription = MainSubscription(ServiceExecutorEventTopic),
          entity = serviceName.toString(),
          handler = handler,
          beforeDlq = null,
          concurrency = concurrency,
      )
    }
  }

  private val logMessageSentToDLQ = { message: Message?, e: Exception ->
    logger.error(e) { "Sending message to DLQ ${message ?: "(Not Deserialized)"}" }
  }

  private fun CoroutineScope.startServiceEventListener(
    serviceName: ServiceName,
    concurrency: Int,
    subscriptionName: String?,
    subscriptionType: SubscriptionType,
    handler: (Message, MillisInstant) -> Unit
  ) {
    // TASK-EXECUTOR topic
    launch {
      consumerAsync.start(
          subscription = subscriptionType.create(ServiceExecutorTopic, subscriptionName),
          entity = serviceName.toString(),
          handler = handler,
          beforeDlq = logMessageSentToDLQ,
          concurrency = concurrency,
      )
    }
    // TASK-EXECUTOR-DELAY topic
    launch {
      consumerAsync.start(
          subscription = subscriptionType.create(RetryServiceExecutorTopic, subscriptionName),
          entity = serviceName.toString(),
          handler = handler,
          beforeDlq = logMessageSentToDLQ,
          concurrency = concurrency,
      )
    }
    // TASK-EVENTS topic
    launch {
      consumerAsync.start(
          subscription = subscriptionType.create(ServiceExecutorEventTopic, subscriptionName),
          entity = serviceName.toString(),
          handler = handler,
          beforeDlq = logMessageSentToDLQ,
          concurrency = concurrency,
      )
    }
  }

  private fun CoroutineScope.startWorkflowExecutorEventListener(
    workflowName: WorkflowName,
    concurrency: Int,
    subscriptionName: String?,
    subscriptionType: SubscriptionType,
    consumer: (Message, MillisInstant) -> Unit
  ) {
    // WORKFLOW-TASK-EXECUTOR topic
    launch {
      consumerAsync.start(
          subscription = subscriptionType.create(WorkflowExecutorTopic, subscriptionName),
          entity = workflowName.toString(),
          handler = consumer,
          beforeDlq = logMessageSentToDLQ,
          concurrency = concurrency,
      )
    }
    // WORKFLOW-TASK-EXECUTOR-DELAY topic
    launch {
      consumerAsync.start(
          subscription = subscriptionType.create(RetryWorkflowExecutorTopic, subscriptionName),
          entity = workflowName.toString(),
          handler = consumer,
          beforeDlq = logMessageSentToDLQ,
          concurrency = concurrency,
      )
    }
    // WORKFLOW-TASK-EVENTS topic
    launch {
      consumerAsync.start(
          subscription = subscriptionType.create(WorkflowExecutorEventTopic, subscriptionName),
          entity = workflowName.toString(),
          handler = consumer,
          beforeDlq = logMessageSentToDLQ,
          concurrency = concurrency,
      )
    }
  }

  private fun CoroutineScope.startWorkflowStateEventListener(
    workflowName: WorkflowName,
    concurrency: Int,
    subscriptionName: String?,
    subscriptionType: SubscriptionType,
    consumer: (Message, MillisInstant) -> Unit
  ) {
    // WORKFLOW-CMD topic
    launch {
      consumerAsync.start(
          subscription = subscriptionType.create(WorkflowStateCmdTopic, subscriptionName),
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
          subscription = subscriptionType.create(WorkflowStateEventTopic, subscriptionName),
          entity = workflowName.toString(),
          handler = consumer,
          beforeDlq = logMessageSentToDLQ,
          concurrency = concurrency,
      )
    }
  }

  private fun KLogger.logCloudEvent(
    message: Message,
    publishedAt: MillisInstant,
    prefix: String,
    eventProducer: Message.(MillisInstant, String) -> CloudEvent?
  ) {
    try {
      info {
        message.eventProducer(publishedAt, prefix)?.toJsonString(register.logsConfig.beautify)
      }
    } catch (e: Exception) {
      // Failure to log shouldn't break the application
      try {
        error(e) { "Error while logging the CloudEvent json of: $message" }
      } catch (error: Exception) {
        System.err.println("Failed to log the original exception due to ${error.message}\n${error.stackTraceToString()}")
      }
    }
  }

  private fun KLogger.logWorkflowCloudEvent(
    message: Message,
    publishedAt: MillisInstant,
    prefix: String,
  ) = logCloudEvent(message, publishedAt, prefix, Message::toWorkflowCloudEvent)

  private fun KLogger.logServiceCloudEvent(
    message: Message,
    publishedAt: MillisInstant,
    prefix: String,
  ) = logCloudEvent(message, publishedAt, prefix, Message::toServiceCloudEvent)
}
