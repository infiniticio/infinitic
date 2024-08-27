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
import io.infinitic.cloudEvents.logs.CLOUD_EVENTS_SERVICE_EXECUTOR
import io.infinitic.cloudEvents.logs.CLOUD_EVENTS_SERVICE_TAG_ENGINE
import io.infinitic.cloudEvents.logs.CLOUD_EVENTS_WORKFLOW_EXECUTOR
import io.infinitic.cloudEvents.logs.CLOUD_EVENTS_WORKFLOW_STATE_ENGINE
import io.infinitic.cloudEvents.logs.CLOUD_EVENTS_WORKFLOW_TAG_ENGINE
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.events.messages.ServiceExecutorEventMessage
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.tasks.tags.messages.ServiceTagMessage
import io.infinitic.common.tasks.tags.storage.TaskTagStorage
import io.infinitic.common.transport.InfiniticConsumer
import io.infinitic.common.transport.InfiniticProducer
import io.infinitic.common.transport.InfiniticResources
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
import io.infinitic.common.transport.logged.LoggedInfiniticConsumer
import io.infinitic.common.transport.logged.LoggedInfiniticProducer
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
import io.infinitic.tasks.executor.TaskRetryHandler
import io.infinitic.tasks.tag.TaskTagEngine
import io.infinitic.tasks.tag.storage.LoggedTaskTagStorage
import io.infinitic.transport.config.TransportConfig
import io.infinitic.workers.config.WorkerConfig
import io.infinitic.workers.config.WorkerConfigInterface
import io.infinitic.workers.register.InfiniticRegister
import io.infinitic.workers.registrable.Registrable
import io.infinitic.workers.registrable.ServiceDefault
import io.infinitic.workers.registrable.ServiceExecutor
import io.infinitic.workers.registrable.WorkflowDefault
import io.infinitic.workers.registrable.applyDefault
import io.infinitic.workers.registrable.build
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
  val resources: InfiniticResources,
  val consumer: InfiniticConsumer,
  val producer: InfiniticProducer,
  private val source: String
) : AutoCloseable {

  /** Infinitic Client */
  val client = InfiniticClient(consumer, producer)

  init {
    consumer.workerLogger = logger

    Runtime.getRuntime().addShutdownHook(
        Thread {
          logger.info { "Closing worker..." }
          close()
          logger.info { "Worker closed." }
        },
    )
  }

  companion object {
    private val logger = KotlinLogging.logger {}

    /** Create [InfiniticWorker] from config */
    @JvmStatic
    fun fromConfig(workerConfig: WorkerConfigInterface): InfiniticWorker = with(workerConfig) {

      val transportConfig = TransportConfig(transport, pulsar, shutdownGracePeriodInSeconds)

      /** Infinitic resources */
      val resources = transportConfig.resources

      /** Infinitic Consumer */
      val consumer = transportConfig.consumer

      /** Infinitic  Producer */
      val producer = transportConfig.producer

      // set name, if it exists in the worker configuration
      name?.let { producer.name = it }

      /** Infinitic Register */
      // if an exception is thrown, we ensure to close the previously created resource
      val register = try {
        InfiniticRegister.fromConfig(this)
      } catch (e: Exception) {
        consumer.close()
        throw e
      }

      /** Infinitic Worker */
      InfiniticWorker(register, resources, consumer, producer, transportConfig.source).also {
        // close consumer with the worker
        it.addAutoCloseResource(consumer)
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

  private val sendingMessageToDLQ = { "Unable to process message, sending to Dead Letter Queue" }

  override fun close() {
    client.close()
    autoClose()
  }

  private val registrable = mutableListOf<Registrable>()

  fun register(vararg items: Registrable) {
    items.forEach { registrable.add(it) }
  }

  private fun execRegistration() {
    var serviceDefault: ServiceDefault? = null
    var workflowDefault: WorkflowDefault? = null

    registrable.forEach {
      when (it) {
        is ServiceDefault -> serviceDefault = it
        is WorkflowDefault -> workflowDefault = it
        else -> Unit
      }
    }

    registrable.forEach {
      when (it) {
        is ServiceDefault, is WorkflowDefault -> Unit
        is ServiceExecutor -> it.applyDefault(serviceDefault).build()
        else -> thisShouldNotHappen()
      }
    }
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

      workerRegistry.eventListener?.let {
        with(it) {
          // for all services
          resources.getServices().forEach { serviceName ->
            // if this service is included in the RegisteredEventListener.services
            if (services.isIncluded(serviceName)) {
              startServiceEventListener(
                  ServiceName(serviceName),
                  concurrency,
                  subscriptionName,
                  SubscriptionType.EVENT_LISTENER,
              ) { message: Message, publishedAt: MillisInstant ->
                message.toServiceCloudEvent(publishedAt, source)?.let { cloudEvent ->
                  eventListener.onEvent(cloudEvent)
                } ?: Unit
              }
            }
          }
          // for all workflows
          resources.getWorkflows().forEach { workflowName ->
            // if this workflow is included in the RegisteredEventListener.workflows
            if (workflows.isIncluded(workflowName)) {
              startWorkflowExecutorEventListener(
                  WorkflowName(workflowName),
                  concurrency,
                  subscriptionName,
                  SubscriptionType.EVENT_LISTENER,
              ) { message, publishedAt ->
                message.toServiceCloudEvent(publishedAt, source)?.let { cloudEvent ->
                  eventListener.onEvent(cloudEvent)
                } ?: Unit
              }
              startWorkflowStateEventListener(
                  WorkflowName(workflowName),
                  concurrency,
                  subscriptionName,
                  SubscriptionType.EVENT_LISTENER,
              ) { message, publishedAt ->
                message.toWorkflowCloudEvent(publishedAt, source)?.let { cloudEvent ->
                  eventListener.onEvent(cloudEvent)
                } ?: Unit
              }
            }
          }
        }
      }
    }

    logger.info {
      "Worker \"${producer.name}\" ready" + when (consumer is PulsarInfiniticConsumer) {
        true -> " (shutdownGracePeriodInSeconds=${consumer.shutdownGracePeriodInSeconds}s)"
        false -> ""
      }
    }

    return CompletableFuture.supplyAsync { consumer.join() }
  }

  private fun CoroutineScope.startWorkflowTagEngine(
    workflowName: WorkflowName,
    concurrency: Int,
    storage: WorkflowTagStorage
  ) {
    val eventLogger = KotlinLogging.logger("$CLOUD_EVENTS_WORKFLOW_TAG_ENGINE.$workflowName")
        .ignoreNull()

    // WORKFLOW-TAG
    launch {
      val logger = WorkflowTagEngine.logger
      val loggedStorage = LoggedWorkflowTagStorage(logger, storage)
      val loggedConsumer = LoggedInfiniticConsumer(logger, consumer)
      val loggedProducer = LoggedInfiniticProducer(logger, producer)

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
    val eventLogger = KotlinLogging.logger("$CLOUD_EVENTS_WORKFLOW_STATE_ENGINE.$workflowName")
        .ignoreNull()
    // WORKFLOW-CMD
    launch {
      val logger = WorkflowStateCmdHandler.logger
      val loggedConsumer = LoggedInfiniticConsumer(logger, consumer)
      val loggedProducer = LoggedInfiniticProducer(logger, producer)

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
      val logger = WorkflowStateEngine.logger
      val loggedStorage = LoggedWorkflowStateStorage(logger, storage)
      val loggedConsumer = LoggedInfiniticConsumer(logger, consumer)
      val loggedProducer = LoggedInfiniticProducer(logger, producer)

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
      val logger = WorkflowStateTimerHandler.logger
      val loggedConsumer = LoggedInfiniticConsumer(logger, consumer)
      val loggedProducer = LoggedInfiniticProducer(logger, producer)

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
      val logger = WorkflowStateEventHandler.logger
      val loggedConsumer = LoggedInfiniticConsumer(logger, consumer)
      val loggedProducer = LoggedInfiniticProducer(logger, producer)

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
    val eventLogger = KotlinLogging.logger("$CLOUD_EVENTS_WORKFLOW_EXECUTOR.$workflowName")
        .ignoreNull()

    // WORKFLOW-TASK_EXECUTOR
    launch {
      val logger = TaskExecutor.logger
      val loggedConsumer = LoggedInfiniticConsumer(logger, consumer)
      val loggedProducer = LoggedInfiniticProducer(logger, producer)

      val workflowTaskExecutor = TaskExecutor(workerRegistry, loggedProducer, client)

      val handler: suspend (ServiceExecutorMessage, MillisInstant) -> Unit =
          { message, publishedAt ->
            eventLogger.logServiceCloudEvent(message, publishedAt, source)
            workflowTaskExecutor.handle(message, publishedAt)
          }

      val beforeDlq: suspend (ServiceExecutorMessage?, Exception) -> Unit = { message, cause ->
        when (message) {
          null -> Unit
          is ExecuteTask -> with(workflowTaskExecutor) {
            message.sendTaskFailed(cause, Task.meta, sendingMessageToDLQ)
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
      val logger = TaskRetryHandler.logger
      val loggedConsumer = LoggedInfiniticConsumer(logger, consumer)
      val loggedProducer = LoggedInfiniticProducer(logger, producer)
      val taskRetryHandler = TaskRetryHandler(loggedProducer)

      // we do not use loggedConsumer to avoid logging twice the messages coming from delayed topics
      loggedConsumer.start(
          subscription = MainSubscription(RetryWorkflowExecutorTopic),
          entity = workflowName.toString(),
          handler = taskRetryHandler::handle,
          beforeDlq = null,
          concurrency = concurrency,
      )
    }

    // WORKFLOW-TASK-EVENT
    launch {
      val logger = TaskEventHandler.logger
      val loggedConsumer = LoggedInfiniticConsumer(logger, consumer)
      val loggedProducer = LoggedInfiniticProducer(logger, producer)

      val workflowTaskEventHandler = TaskEventHandler(loggedProducer)

      val handler: suspend (ServiceExecutorEventMessage, MillisInstant) -> Unit =
          { message, publishedAt ->
            eventLogger.logServiceCloudEvent(message, publishedAt, source)
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
    val eventLogger = KotlinLogging.logger("$CLOUD_EVENTS_SERVICE_TAG_ENGINE.$serviceName")
        .ignoreNull()

    // TASK-TAG
    launch {
      val logger = TaskTagEngine.logger
      val loggedStorage = LoggedTaskTagStorage(logger, storage)
      val loggedConsumer = LoggedInfiniticConsumer(logger, consumer)
      val loggedProducer = LoggedInfiniticProducer(logger, producer)

      val taskTagEngine = TaskTagEngine(loggedStorage, loggedProducer)

      val handler: suspend (ServiceTagMessage, MillisInstant) -> Unit =
          { message, publishedAt ->
            eventLogger.logServiceCloudEvent(message, publishedAt, source)
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
    val eventLogger = KotlinLogging.logger("$CLOUD_EVENTS_SERVICE_EXECUTOR.$serviceName")
        .ignoreNull()

    // TASK-EXECUTOR
    launch {
      val logger = TaskExecutor.logger
      val loggedConsumer = LoggedInfiniticConsumer(logger, consumer)
      val loggedProducer = LoggedInfiniticProducer(logger, producer)
      val taskExecutor = TaskExecutor(workerRegistry, loggedProducer, client)

      val handler: suspend (ServiceExecutorMessage, MillisInstant) -> Unit =
          { message, publishedAt ->
            eventLogger.logServiceCloudEvent(message, publishedAt, source)
            taskExecutor.handle(message, publishedAt)
          }

      val beforeDlq: suspend (ServiceExecutorMessage?, Exception) -> Unit = { message, cause ->
        when (message) {
          null -> Unit
          is ExecuteTask -> with(taskExecutor) {
            message.sendTaskFailed(cause, Task.meta, sendingMessageToDLQ)
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
      val logger = TaskRetryHandler.logger
      val loggedConsumer = LoggedInfiniticConsumer(logger, consumer)
      val loggedProducer = LoggedInfiniticProducer(logger, producer)

      val taskRetryHandler = TaskRetryHandler(loggedProducer)

      loggedConsumer.start(
          subscription = MainSubscription(RetryServiceExecutorTopic),
          entity = serviceName.toString(),
          handler = taskRetryHandler::handle,
          beforeDlq = null,
          concurrency = concurrency,
      )
    }

    // TASK-EVENTS
    launch {
      val logger = TaskEventHandler.logger
      val loggedConsumer = LoggedInfiniticConsumer(logger, consumer)
      val loggedProducer = LoggedInfiniticProducer(logger, producer)
      val taskEventHandler = TaskEventHandler(loggedProducer)

      val handler: suspend (ServiceExecutorEventMessage, MillisInstant) -> Unit =
          { message, publishedAt ->
            eventLogger.logServiceCloudEvent(message, publishedAt, source)
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
      consumer.start(
          subscription = subscriptionType.create(ServiceExecutorTopic, subscriptionName),
          entity = serviceName.toString(),
          handler = handler,
          beforeDlq = logMessageSentToDLQ,
          concurrency = concurrency,
      )
    }
    // TASK-EXECUTOR-DELAY topic
    launch {
      consumer.start(
          subscription = subscriptionType.create(RetryServiceExecutorTopic, subscriptionName),
          entity = serviceName.toString(),
          handler = handler,
          beforeDlq = logMessageSentToDLQ,
          concurrency = concurrency,
      )
    }
    // TASK-EVENTS topic
    launch {
      consumer.start(
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
    handler: (Message, MillisInstant) -> Unit
  ) {
    // WORKFLOW-TASK-EXECUTOR topic
    launch {
      consumer.start(
          subscription = subscriptionType.create(WorkflowExecutorTopic, subscriptionName),
          entity = workflowName.toString(),
          handler = handler,
          beforeDlq = logMessageSentToDLQ,
          concurrency = concurrency,
      )
    }
    // WORKFLOW-TASK-EXECUTOR-DELAY topic
    launch {
      consumer.start(
          subscription = subscriptionType.create(RetryWorkflowExecutorTopic, subscriptionName),
          entity = workflowName.toString(),
          handler = handler,
          beforeDlq = logMessageSentToDLQ,
          concurrency = concurrency,
      )
    }
    // WORKFLOW-TASK-EVENTS topic
    launch {
      consumer.start(
          subscription = subscriptionType.create(WorkflowExecutorEventTopic, subscriptionName),
          entity = workflowName.toString(),
          handler = handler,
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
    handler: (Message, MillisInstant) -> Unit
  ) {
    // WORKFLOW-CMD topic
    launch {
      consumer.start(
          subscription = subscriptionType.create(WorkflowStateCmdTopic, subscriptionName),
          entity = workflowName.toString(),
          handler = handler,
          beforeDlq = logMessageSentToDLQ,
          concurrency = concurrency,
      )
    }
    // WORKFLOW-STATE-ENGINE topic
    launch {
      consumer.start(
          subscription = subscriptionType.create(WorkflowStateEngineTopic, subscriptionName),
          entity = workflowName.toString(),
          handler = { message: Message, publishedAt: MillisInstant ->
            // the event handler is not applied for WorkflowCmdMessage from clients
            // as the event has already been handled in the workflow-cmd topic
            if (message !is WorkflowCmdMessage) handler(message, publishedAt)
          },
          beforeDlq = logMessageSentToDLQ,
          concurrency = concurrency,
      )
    }
    // WORKFLOW-EVENTS topic
    launch {
      consumer.start(
          subscription = subscriptionType.create(WorkflowStateEventTopic, subscriptionName),
          entity = workflowName.toString(),
          handler = handler,
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
