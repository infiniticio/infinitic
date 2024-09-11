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
import io.infinitic.clients.InfiniticClient
import io.infinitic.cloudEvents.logs.CLOUD_EVENTS_SERVICE_EXECUTOR
import io.infinitic.cloudEvents.logs.CLOUD_EVENTS_SERVICE_TAG_ENGINE
import io.infinitic.cloudEvents.logs.CLOUD_EVENTS_WORKFLOW_EXECUTOR
import io.infinitic.cloudEvents.logs.CLOUD_EVENTS_WORKFLOW_STATE_ENGINE
import io.infinitic.cloudEvents.logs.CLOUD_EVENTS_WORKFLOW_TAG_ENGINE
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.events.messages.ServiceExecutorEventMessage
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.tasks.tags.messages.ServiceTagMessage
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
import io.infinitic.common.workflows.emptyWorkflowContext
import io.infinitic.common.workflows.engine.messages.WorkflowCmdMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEventMessage
import io.infinitic.common.workflows.engine.messages.WorkflowStateEngineMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagEngineMessage
import io.infinitic.events.toJsonString
import io.infinitic.events.toServiceCloudEvent
import io.infinitic.events.toWorkflowCloudEvent
import io.infinitic.logger.ignoreNull
import io.infinitic.tasks.Task
import io.infinitic.tasks.WithTimeout
import io.infinitic.tasks.executor.TaskEventHandler
import io.infinitic.tasks.executor.TaskExecutor
import io.infinitic.tasks.executor.TaskRetryHandler
import io.infinitic.tasks.tag.TaskTagEngine
import io.infinitic.tasks.tag.storage.LoggedTaskTagStorage
import io.infinitic.workers.config.ConfigGetterInterface
import io.infinitic.workers.config.EventListenerConfig
import io.infinitic.workers.config.InfiniticWorkerConfig
import io.infinitic.workers.config.InfiniticWorkerConfigInterface
import io.infinitic.workers.config.ServiceConfig
import io.infinitic.workers.config.ServiceExecutorConfig
import io.infinitic.workers.config.ServiceTagEngineConfig
import io.infinitic.workers.config.WorkflowConfig
import io.infinitic.workers.config.WorkflowExecutorConfig
import io.infinitic.workers.config.WorkflowStateEngineConfig
import io.infinitic.workers.config.WorkflowTagEngineConfig
import io.infinitic.workers.registry.ExecutorRegistry
import io.infinitic.workflows.Workflow
import io.infinitic.workflows.engine.WorkflowStateCmdHandler
import io.infinitic.workflows.engine.WorkflowStateEngine
import io.infinitic.workflows.engine.WorkflowStateEventHandler
import io.infinitic.workflows.engine.WorkflowStateTimerHandler
import io.infinitic.workflows.engine.storage.LoggedWorkflowStateStorage
import io.infinitic.workflows.tag.WorkflowTagEngine
import io.infinitic.workflows.tag.storage.LoggedWorkflowTagStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.future
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException
import kotlin.system.exitProcess

@Suppress("unused")
class InfiniticWorker(
  val config: InfiniticWorkerConfigInterface,
) : AutoCloseable, ConfigGetterInterface {

  private val registry = ExecutorRegistry(config.services, config.workflows)

  private fun getService(serviceName: String): ServiceConfig? =
      config.services.firstOrNull { it.name == serviceName }

  private fun getWorkflow(workflowName: String): WorkflowConfig? =
      config.workflows.firstOrNull { it.name == workflowName }

  override fun getEventListenerConfig() =
      config.eventListener

  override fun getServiceExecutorConfigs() =
      config.services.mapNotNull { it.executor }

  override fun getServiceTagEngineConfigs() =
      config.services.mapNotNull { it.tagEngine }

  override fun getWorkflowExecutorConfigs() =
      config.workflows.mapNotNull { it.executor }

  override fun getWorkflowTagEngineConfigs() =
      config.workflows.mapNotNull { it.tagEngine }

  override fun getWorkflowStateEngineConfigs() =
      config.workflows.mapNotNull { it.stateEngine }

  override fun getServiceExecutorConfig(serviceName: String) =
      getService(serviceName)?.executor

  override fun getServiceTagEngineConfig(serviceName: String) =
      getService(serviceName)?.tagEngine

  override fun getWorkflowExecutorConfig(workflowName: String) =
      getWorkflow(workflowName)?.executor

  override fun getWorkflowTagEngineConfig(workflowName: String) =
      getWorkflow(workflowName)?.tagEngine

  override fun getWorkflowStateEngineConfig(workflowName: String) =
      getWorkflow(workflowName)?.stateEngine

  private val resources by lazy {
    config.transport.resources
  }
  private val consumer by lazy {
    config.transport.consumer
  }
  private val producer by lazy {
    config.transport.producer.apply { config.name?.let { name = it } }
  }

  private val shutdownGracePeriodSeconds = config.transport.shutdownGracePeriodSeconds
  private val source = config.transport.cloudEventSource
  private val beautifyLogs = config.logs.beautify

  /**
   * Indicates whether the InfiniticWorker instance is started.
   *
   * This variable is used to manage the state of the worker, ensuring that it can
   * be safely closed and that no operations occur once the worker is marked as closed.
   * When set to `false`, the worker is closed or not yet started;
   * when set to `true`, the worker is operational.
   */
  private var isStarted: AtomicBoolean = AtomicBoolean(false)

  private lateinit var completableStart: CompletableFuture<Unit>

  /** Coroutine scope used to launch consumers and await their termination */
  private var scope = CoroutineScope(Dispatchers.IO)

  /** Infinitic Client */
  val client = InfiniticClient(config)

  init {
    Runtime.getRuntime().addShutdownHook(Thread { close() })
  }

  override fun close() {
    if (isStarted.compareAndSet(true, false)) runBlocking {
      logger.info { "Closing worker..." }
      try {
        scope.cancel()
        logger.info { "Processing ongoing messages..." }
        withTimeout((shutdownGracePeriodSeconds * 1000).toLong()) {
          scope.coroutineContext.job.join()
          logger.info { "All ongoing messages have been processed." }
        }
      } catch (e: TimeoutCancellationException) {
        logger.warn {
          "The grace period (${shutdownGracePeriodSeconds}s) allotted when closing the worker was insufficient. " +
              "Some ongoing messages may not have been processed properly."
        }
      } finally {
        client.close()
      }
      logger.info { "Worker closed." }
    }
  }


  companion object {
    private val logger = KotlinLogging.logger {}
    private const val NONE = "none"

    @JvmStatic
    fun builder() = InfiniticWorkerBuilder()

    /** Create [InfiniticWorker] from yaml resources */
    @JvmStatic
    fun fromYamlResource(vararg resources: String) =
        InfiniticWorker(InfiniticWorkerConfig.fromYamlResource(*resources))

    /** Create [InfiniticWorker] from yaml files */
    @JvmStatic
    fun fromYamlFile(vararg files: String): InfiniticWorker =
        InfiniticWorker(InfiniticWorkerConfig.fromYamlFile(*files))

    /** Create [InfiniticWorker] from yaml strings */
    @JvmStatic
    fun fromYamlString(vararg yamls: String): InfiniticWorker =
        InfiniticWorker(InfiniticWorkerConfig.fromYamlString(*yamls))
  }

  private val sendingMessageToDLQ = { "Unable to process message, sending to Dead Letter Queue" }

  /**
   * Start worker synchronously
   * (blocks the current thread)
   */
  fun start(): Unit = try {
    startAsync().join()
  } catch (e: CancellationException) {
    // do nothing, the worker has been closed
  } catch (e: Throwable) {
    logger.error(e) { "Error: exiting" }
    // this will trigger the shutdown hook
    exitProcess(1)
  }

  /**
   * Start worker asynchronously
   */
  fun startAsync(): CompletableFuture<Unit> {
    if (isStarted.compareAndSet(false, true)) {
      scope = CoroutineScope(Dispatchers.IO)

      completableStart = scope.future {

        getServiceTagEngineConfigs().forEach { startServiceTagEngine(it) }

        config.services.forEach { serviceConfig ->
          logger.info { "Service ${serviceConfig.name}:" }
          // Start SERVICE TAG ENGINE
          serviceConfig.tagEngine?.let { startServiceTagEngine(it) }
          // Start SERVICE EXECUTOR
          serviceConfig.executor?.let { startServiceExecutor(it) }
        }

        config.workflows.forEach { workflowConfig ->
          logger.info { "Workflow ${workflowConfig.name}:" }
          // Start WORKFLOW TAG ENGINE
          workflowConfig.tagEngine?.let { startWorkflowTagEngine(it) }
          // Start WORKFLOW STATE ENGINE
          workflowConfig.stateEngine?.let { startWorkflowStateEngine(it) }
          // Start WORKFLOW EXECUTOR
          workflowConfig.executor?.let { startWorkflowExecutor(it) }
        }

        config.eventListener?.let { startEventListener(it) }

        logger.info {
          "Worker \"${producer.name}\" ready (shutdownGracePeriodSeconds=${shutdownGracePeriodSeconds}s)"
        }
      }
    }
    return completableStart
  }

  private fun WithTimeout?.toLog() =
      this?.getTimeoutSeconds()?.let { String.format("%.2fs", it) } ?: NONE

  private fun logEventListenerStart(config: EventListenerConfig) {
    logger.info {
      "* Service Event Listener".padEnd(25) + ": (" +
          "concurrency: ${config.concurrency}, " +
          "class: ${config.listener::class.java.name}" +
          (config.subscriptionName?.let { ", subscription: $it" } ?: "") +
          ")"
    }
  }

  private fun logServiceExecutorStart(config: ServiceExecutorConfig) {
    logger.info {
      "* Service Executor".padEnd(25) + ": (" +
          "concurrency: ${config.concurrency}, " +
          "class: ${config.factory()::class.java.name}, " +
          "timeout: ${config.withTimeout?.toLog()}, " +
          "withRetry: ${config.withRetry ?: NONE})"
    }
  }

  private fun logServiceTagEngineStart(config: ServiceTagEngineConfig) {
    logger.info {
      "* Service Tag Engine".padEnd(25) + ": (" +
          "concurrency: ${config.concurrency}, " +
          "storage: ${config.storage?.type}, " +
          "cache: ${config.storage?.cache?.type ?: NONE}, " +
          "compression: ${config.storage?.compression ?: NONE})"
    }
  }

  private fun logWorkflowExecutorStart(config: WorkflowExecutorConfig) {
    Workflow.setContext(emptyWorkflowContext)
    logger.info {
      "* Workflow Executor".padEnd(25) + ": (" +
          "concurrency: ${config.concurrency}, " +
          "classes: ${
            config.factories.map { it.invoke()::class.java }.joinToString { it.name }
          }, " +
          "timeout: ${config.withTimeout?.toLog()}, " +
          "withRetry: ${config.withRetry ?: NONE}" +
          (config.checkMode?.let { ", checkMode: $it" } ?: "") +
          ")"
    }
  }

  private fun logWorkflowTagEngineStart(config: WorkflowTagEngineConfig) {
    logger.info {
      "* Workflow Tag Engine".padEnd(25) + ": (" +
          "concurrency: ${config.concurrency}, " +
          "storage: ${config.storage?.type}, " +
          "cache: ${config.storage?.cache?.type ?: NONE}, " +
          "compression: ${config.storage?.compression ?: NONE})"
    }
  }

  private fun logWorkflowStateEngineStart(config: WorkflowStateEngineConfig) {
    logger.info {
      "* Workflow State Engine".padEnd(25) + ": (" +
          "concurrency: ${config.concurrency}, " +
          "storage: ${config.storage?.type}, " +
          "cache: ${config.storage?.cache?.type ?: NONE}, " +
          "compression: ${config.storage?.compression ?: NONE})"
    }
  }

  private fun CoroutineScope.startServiceTagEngine(config: ServiceTagEngineConfig) {
    // Log Service Tag Engine configuration
    logServiceTagEngineStart(config)

    val logsEventLogger = KotlinLogging.logger(
        "$CLOUD_EVENTS_SERVICE_TAG_ENGINE.${config.serviceName}",
    ).ignoreNull()

    // TASK-TAG
    launch {
      val logger = TaskTagEngine.logger
      val loggedStorage = LoggedTaskTagStorage(logger, config.serviceTagStorage)
      val loggedConsumer = LoggedInfiniticConsumer(logger, consumer)
      val loggedProducer = LoggedInfiniticProducer(logger, producer)

      val taskTagEngine = TaskTagEngine(loggedStorage, loggedProducer)

      val handler: suspend (ServiceTagMessage, MillisInstant) -> Unit =
          { message, publishedAt ->
            logsEventLogger.logServiceCloudEvent(message, publishedAt, source)
            taskTagEngine.handle(message, publishedAt)
          }

      loggedConsumer.start(
          subscription = MainSubscription(ServiceTagEngineTopic),
          entity = config.serviceName,
          handler = handler,
          beforeDlq = null,
          concurrency = config.concurrency,
      )
    }
  }

  private fun CoroutineScope.startServiceExecutor(config: ServiceExecutorConfig) {
    // Log Service Executor configuration
    logServiceExecutorStart(config)

    val logsEventLogger = KotlinLogging.logger(
        "$CLOUD_EVENTS_SERVICE_EXECUTOR.${config.serviceName}",
    ).ignoreNull()

    // TASK-EXECUTOR
    launch {
      val logger = TaskExecutor.logger
      val loggedConsumer = LoggedInfiniticConsumer(logger, consumer)
      val loggedProducer = LoggedInfiniticProducer(logger, producer)
      val taskExecutor = TaskExecutor(registry, loggedProducer, client)

      val handler: suspend (ServiceExecutorMessage, MillisInstant) -> Unit =
          { message, publishedAt ->
            logsEventLogger.logServiceCloudEvent(message, publishedAt, source)
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
          entity = config.serviceName,
          handler = handler,
          beforeDlq = beforeDlq,
          concurrency = config.concurrency,
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
          entity = config.serviceName,
          handler = taskRetryHandler::handle,
          beforeDlq = null,
          concurrency = config.concurrency,
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
            logsEventLogger.logServiceCloudEvent(message, publishedAt, source)
            taskEventHandler.handle(message, publishedAt)
          }

      loggedConsumer.start(
          subscription = MainSubscription(ServiceExecutorEventTopic),
          entity = config.serviceName,
          handler = handler,
          beforeDlq = null,
          concurrency = config.concurrency,
      )
    }
  }

  private fun CoroutineScope.startWorkflowTagEngine(config: WorkflowTagEngineConfig) {
    // Log Workflow State Engine configuration
    logWorkflowTagEngineStart(config)

    val logsEventLogger = KotlinLogging.logger(
        "$CLOUD_EVENTS_WORKFLOW_TAG_ENGINE.${config.workflowName}",
    ).ignoreNull()

    // WORKFLOW-TAG
    launch {
      val logger = WorkflowTagEngine.logger
      val loggedStorage = LoggedWorkflowTagStorage(logger, config.workflowTagStorage)
      val loggedConsumer = LoggedInfiniticConsumer(logger, consumer)
      val loggedProducer = LoggedInfiniticProducer(logger, producer)

      val workflowTagEngine = WorkflowTagEngine(loggedStorage, loggedProducer)

      val handler: suspend (WorkflowTagEngineMessage, MillisInstant) -> Unit =
          { message, publishedAt ->
            logsEventLogger.logWorkflowCloudEvent(message, publishedAt, source)
            workflowTagEngine.handle(message, publishedAt)
          }

      loggedConsumer.start(
          subscription = MainSubscription(WorkflowTagEngineTopic),
          entity = config.workflowName,
          handler = handler,
          beforeDlq = null,
          concurrency = config.concurrency,
      )
    }
  }

  private fun CoroutineScope.startWorkflowStateEngine(config: WorkflowStateEngineConfig) {
    // Log Workflow State Engine configuration
    logWorkflowStateEngineStart(config)

    val logsEventLogger = KotlinLogging.logger(
        "$CLOUD_EVENTS_WORKFLOW_STATE_ENGINE.${config.workflowName}",
    ).ignoreNull()

    // WORKFLOW-CMD
    launch {
      val logger = WorkflowStateCmdHandler.logger
      val loggedConsumer = LoggedInfiniticConsumer(logger, consumer)
      val loggedProducer = LoggedInfiniticProducer(logger, producer)

      val workflowStateCmdHandler = WorkflowStateCmdHandler(loggedProducer)

      val handler: suspend (WorkflowStateEngineMessage, MillisInstant) -> Unit =
          { message, publishedAt ->
            logsEventLogger.logWorkflowCloudEvent(message, publishedAt, source)
            workflowStateCmdHandler.handle(message, publishedAt)
          }

      loggedConsumer.start(
          subscription = MainSubscription(WorkflowStateCmdTopic),
          entity = config.workflowName,
          handler = handler,
          beforeDlq = null,
          concurrency = config.concurrency,
      )
    }

    // WORKFLOW-STATE-ENGINE
    launch {
      val logger = WorkflowStateEngine.logger
      val loggedStorage = LoggedWorkflowStateStorage(logger, config.workflowStateStorage)
      val loggedConsumer = LoggedInfiniticConsumer(logger, consumer)
      val loggedProducer = LoggedInfiniticProducer(logger, producer)

      val workflowStateEngine = WorkflowStateEngine(loggedStorage, loggedProducer)

      val handler: suspend (WorkflowStateEngineMessage, MillisInstant) -> Unit =
          { message, publishedAt ->
            if (message !is WorkflowCmdMessage) {
              logsEventLogger.logWorkflowCloudEvent(message, publishedAt, source)
            }
            workflowStateEngine.handle(message, publishedAt)
          }

      loggedConsumer.start(
          subscription = MainSubscription(WorkflowStateEngineTopic),
          entity = config.workflowName,
          handler = handler,
          beforeDlq = null,
          concurrency = config.concurrency,
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
          entity = config.workflowName,
          handler = workflowStateTimerHandler::handle,
          beforeDlq = null,
          concurrency = config.concurrency,
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
            logsEventLogger.logWorkflowCloudEvent(message, publishedAt, source)
            workflowStateEventHandler.handle(message, publishedAt)
          }

      loggedConsumer.start(
          subscription = MainSubscription(WorkflowStateEventTopic),
          entity = config.workflowName,
          handler = handler,
          beforeDlq = null,
          concurrency = config.concurrency,
      )
    }
  }

  private fun CoroutineScope.startWorkflowExecutor(config: WorkflowExecutorConfig) {
    // Log Workflow Executor configuration
    logWorkflowExecutorStart(config)

    val logsEventLogger = KotlinLogging.logger(
        "$CLOUD_EVENTS_WORKFLOW_EXECUTOR.${config.workflowName}",
    ).ignoreNull()

    // WORKFLOW-TASK_EXECUTOR
    launch {
      val logger = TaskExecutor.logger
      val loggedConsumer = LoggedInfiniticConsumer(logger, consumer)
      val loggedProducer = LoggedInfiniticProducer(logger, producer)

      val workflowTaskExecutor = TaskExecutor(registry, loggedProducer, client)

      val handler: suspend (ServiceExecutorMessage, MillisInstant) -> Unit =
          { message, publishedAt ->
            logsEventLogger.logServiceCloudEvent(message, publishedAt, source)
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
          entity = config.workflowName,
          handler = handler,
          beforeDlq = beforeDlq,
          concurrency = config.concurrency,
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
          entity = config.workflowName,
          handler = taskRetryHandler::handle,
          beforeDlq = null,
          concurrency = config.concurrency,
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
            logsEventLogger.logServiceCloudEvent(message, publishedAt, source)
            workflowTaskEventHandler.handle(message, publishedAt)
          }

      loggedConsumer.start(
          subscription = MainSubscription(WorkflowExecutorEventTopic),
          entity = config.workflowName,
          handler = handler,
          beforeDlq = null,
          concurrency = config.concurrency,
      )
    }
  }

  private fun CoroutineScope.startEventListener(config: EventListenerConfig) {
    logEventListenerStart(config)

    launch {
      checkNewServices(config) { serviceName ->
        logger.info { "EventListener starting listening Service $serviceName" }

        startServiceEventListener(
            ServiceName(serviceName),
            config.concurrency,
            config.subscriptionName,
            SubscriptionType.EVENT_LISTENER,
        ) { message: Message, publishedAt: MillisInstant ->
          message.toServiceCloudEvent(publishedAt, source)?.let { cloudEvent ->
            config.listener.onEvent(cloudEvent)
          }
        }
      }
    }

    launch {
      checkNewWorkflows(config) { workflowName ->
        logger.info { "EventListener starting listening Workflow $workflowName" }
        startWorkflowExecutorEventListener(
            WorkflowName(workflowName),
            config.concurrency,
            config.subscriptionName,
            SubscriptionType.EVENT_LISTENER,
        ) { message, publishedAt ->
          message.toServiceCloudEvent(publishedAt, source)?.let { cloudEvent ->
            config.listener.onEvent(cloudEvent)
          }
        }
        startWorkflowStateEventListener(
            WorkflowName(workflowName),
            config.concurrency,
            config.subscriptionName,
            SubscriptionType.EVENT_LISTENER,
        ) { message, publishedAt ->
          message.toWorkflowCloudEvent(publishedAt, source)?.let { cloudEvent ->
            config.listener.onEvent(cloudEvent)
          }
        }
      }
    }
  }

  private fun CoroutineScope.checkNewServices(
    config: EventListenerConfig,
    starter: CoroutineScope.(String) -> Unit
  ) = launch {
    val processedServices = mutableSetOf<String>()

    while (true) {
      // Retrieve the list of services
      val currentServices = resources.getServices().filter { config.includeService(it) }

      // Determine new services that haven't been processed
      val newServices = currentServices.filterNot { it in processedServices }

      // Launch starter for each new service
      for (service in newServices) {
        starter(service)
        // Add the service to the set of processed services
        processedServices.add(service)
      }

      delay((config.refreshDelaySeconds * 1000).toLong())
    }
  }

  private fun CoroutineScope.checkNewWorkflows(
    config: EventListenerConfig,
    starter: CoroutineScope.(String) -> Unit
  ) = launch {
    val processedWorkflows = mutableSetOf<String>()

    while (true) {
      // Retrieve the list of workflows
      val currentServices = resources.getWorkflows().filter { config.includeWorkflow(it) }

      // Determine new workflows that haven't been processed
      val newWorkflows = currentServices.filterNot { it in processedWorkflows }

      // Launch starter for each new workflow
      for (workflow in newWorkflows) {
        starter(workflow)
        // Add the workflow to the set of processed workflows
        processedWorkflows.add(workflow)
      }

      delay((config.refreshDelaySeconds * 1000).toLong())
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
        message.eventProducer(publishedAt, prefix)?.toJsonString(beautifyLogs)
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
