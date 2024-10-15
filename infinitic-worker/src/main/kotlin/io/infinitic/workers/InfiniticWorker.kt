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

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.clients.InfiniticClient
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.events.messages.ServiceExecutorEventMessage
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.tasks.tags.messages.ServiceTagMessage
import io.infinitic.common.transport.MainSubscription
import io.infinitic.common.transport.ServiceExecutorEventTopic
import io.infinitic.common.transport.ServiceExecutorRetryTopic
import io.infinitic.common.transport.ServiceExecutorTopic
import io.infinitic.common.transport.ServiceTagEngineTopic
import io.infinitic.common.transport.WorkflowExecutorEventTopic
import io.infinitic.common.transport.WorkflowExecutorRetryTopic
import io.infinitic.common.transport.WorkflowExecutorTopic
import io.infinitic.common.transport.WorkflowStateCmdTopic
import io.infinitic.common.transport.WorkflowStateEngineTopic
import io.infinitic.common.transport.WorkflowStateEventTopic
import io.infinitic.common.transport.WorkflowStateTimerTopic
import io.infinitic.common.transport.WorkflowTagEngineTopic
import io.infinitic.common.transport.logged.LoggedInfiniticProducer
import io.infinitic.common.workflows.emptyWorkflowContext
import io.infinitic.common.workflows.engine.messages.WorkflowStateEngineMessage
import io.infinitic.common.workflows.engine.messages.WorkflowStateEventMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagEngineMessage
import io.infinitic.events.CloudEventLogger
import io.infinitic.events.config.EventListenerConfig
import io.infinitic.events.listeners.startCloudEventListener
import io.infinitic.tasks.Task
import io.infinitic.tasks.WithTimeout
import io.infinitic.tasks.executor.TaskEventHandler
import io.infinitic.tasks.executor.TaskExecutor
import io.infinitic.tasks.executor.TaskRetryHandler
import io.infinitic.tasks.tag.TaskTagEngine
import io.infinitic.tasks.tag.storage.LoggedTaskTagStorage
import io.infinitic.workers.config.ConfigGetterInterface
import io.infinitic.workers.config.InfiniticWorkerConfig
import io.infinitic.workers.config.InfiniticWorkerConfigInterface
import io.infinitic.workers.config.ServiceConfig
import io.infinitic.workers.config.ServiceExecutorConfig
import io.infinitic.workers.config.ServiceTagEngineConfig
import io.infinitic.workers.config.WorkflowConfig
import io.infinitic.workers.config.WorkflowExecutorConfig
import io.infinitic.workers.config.WorkflowStateEngineConfig
import io.infinitic.workers.config.WorkflowTagEngineConfig
import io.infinitic.workers.config.initBatchMethods
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.future.future
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
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

  /**
   * Indicates whether the InfiniticWorker instance is started.
   */
  private var isStarted: AtomicBoolean = AtomicBoolean(false)

  /** Coroutine scope used to launch consumers and await their termination */
  private lateinit var scope: CoroutineScope

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
    } else {
      logger.warn { "Worker has not started, or is already closing." }
    }
  }

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
    config.transport.producer.apply { config.name?.let { setSuggestedName(it) } }
  }

  private val shutdownGracePeriodSeconds = config.transport.shutdownGracePeriodSeconds
  private val cloudEventSourcePrefix = config.transport.cloudEventSourcePrefix
  private val beautifyLogs = config.logs.beautify

  /** Infinitic Client */
  val client by lazy { InfiniticClient(config) }

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

  private lateinit var completableStart: CompletableFuture<Unit>


  /**
   * Start worker asynchronously
   */
  fun startAsync(): CompletableFuture<Unit> {
    if (isStarted.compareAndSet(false, true)) {
      // Add close hook
      Runtime.getRuntime().addShutdownHook(Thread { close() })

      // create a new scope
      scope = CoroutineScope(Dispatchers.IO)

      completableStart = scope.future {

        val jobs = mutableListOf<Job>()

        config.services.forEach { serviceConfig ->
          with(logger) {
            info { "Service ${serviceConfig.name}:" }
            // Start SERVICE TAG ENGINE
            serviceConfig.tagEngine?.let { jobs.add(startServiceTagEngine(it)) }
            // Start SERVICE EXECUTOR
            serviceConfig.executor?.let { jobs.addAll(startServiceExecutor(it)) }
          }
        }

        config.workflows.forEach { workflowConfig ->
          with(logger) {
            info { "Workflow ${workflowConfig.name}:" }
            // Start WORKFLOW TAG ENGINE
            workflowConfig.tagEngine?.let { jobs.add(startWorkflowTagEngine(it)) }
            // Start WORKFLOW STATE ENGINE
            workflowConfig.stateEngine?.let { jobs.addAll(startWorkflowStateEngine(it)) }
            // Start WORKFLOW EXECUTOR
            workflowConfig.executor?.let { jobs.addAll(startWorkflowExecutor(it)) }
          }
        }

        config.eventListener?.let {
          logEventListenerStart(it)

          with(logger) {
            jobs.add(
                consumer.startCloudEventListener(resources, it, cloudEventSourcePrefix),
            )
          }
        }

        val workerName = producer.getName()

        logger.info {
          "Worker '$workerName' ready (shutdownGracePeriodSeconds=${shutdownGracePeriodSeconds}s)"
        }

        jobs.joinAll()
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

  private val sendingMessageToDLQ = { "Unable to process message, sending to Dead Letter Queue" }


  context(CoroutineScope)
  private suspend fun startServiceTagEngine(config: ServiceTagEngineConfig): Job {
    // Log Service Tag Engine configuration
    logServiceTagEngineStart(config)

    // TASK-TAG
    return with(TaskTagEngine.logger) {
      val loggedStorage = LoggedTaskTagStorage(this, config.serviceTagStorage)
      val loggedProducer = LoggedInfiniticProducer(this, producer)
      val taskTagEngine = TaskTagEngine(loggedStorage, loggedProducer)

      val cloudEventLogger = CloudEventLogger(
          ServiceTagEngineTopic,
          config.serviceName,
          cloudEventSourcePrefix,
          beautifyLogs,
      )

      val process: suspend (ServiceTagMessage, MillisInstant) -> Unit =
          { message, publishedAt ->
            cloudEventLogger.log(message, publishedAt)
            taskTagEngine.handle(message, publishedAt)
          }

      consumer.startAsync(
          subscription = MainSubscription(ServiceTagEngineTopic),
          entity = config.serviceName,
          concurrency = config.concurrency,
          process = process,
      )
    }
  }

  context(CoroutineScope)
  private suspend fun startServiceExecutor(config: ServiceExecutorConfig): List<Job> {
    // Log Service Executor configuration
    logServiceExecutorStart(config)

    // init batch methods for current factory
    config.initBatchMethods()

    // TASK-EXECUTOR
    val jobExecutor = with(TaskExecutor.logger) {
      val loggedProducer = LoggedInfiniticProducer(this, producer)
      val taskExecutor = TaskExecutor(registry, loggedProducer, client)

      val cloudEventLogger = CloudEventLogger(
          ServiceExecutorTopic,
          config.serviceName,
          cloudEventSourcePrefix,
          beautifyLogs,
      )

      val process: suspend (ServiceExecutorMessage, MillisInstant) -> Unit =
          { message, publishedAt ->
            cloudEventLogger.log(message, publishedAt)
            taskExecutor.handle(message, publishedAt)
          }

      val batchProcess: suspend (List<ServiceExecutorMessage>, List<MillisInstant>) -> Unit =
          { messages, publishedAtList ->
            messages.zip(publishedAtList).forEach { (message, publishedAt) ->
              cloudEventLogger.log(message, publishedAt)
            }
            taskExecutor.handleBatch(messages)
          }

      val beforeDlq: suspend (ServiceExecutorMessage, Exception) -> Unit = { message, cause ->
        when (message) {
          is ExecuteTask -> with(taskExecutor) {
            message.sendTaskFailed(cause, Task.meta, sendingMessageToDLQ)
          }
        }
      }

      consumer.startAsync(
          subscription = MainSubscription(ServiceExecutorTopic),
          entity = config.serviceName,
          concurrency = config.concurrency,
          process = process,
          beforeDlq = beforeDlq,
          batchConfig = { msg -> taskExecutor.getBatchConfig(msg) },
          batchProcess = batchProcess,
      )
    }

    // TASK-EXECUTOR-RETRY
    val jobRetry = with(TaskRetryHandler.logger) {
      val loggedProducer = LoggedInfiniticProducer(this, producer)
      val taskRetryHandler = TaskRetryHandler(loggedProducer)

      consumer.startAsync(
          subscription = MainSubscription(ServiceExecutorRetryTopic),
          entity = config.serviceName,
          concurrency = config.concurrency,
          process = taskRetryHandler::handle,
      )
    }

    // TASK-EVENTS
    val jobEvents = with(TaskEventHandler.logger) {
      val loggedProducer = LoggedInfiniticProducer(this, producer)
      val taskEventHandler = TaskEventHandler(loggedProducer)

      val cloudEventLogger = CloudEventLogger(
          ServiceExecutorEventTopic,
          config.serviceName,
          cloudEventSourcePrefix,
          beautifyLogs,
      )

      val process: suspend (ServiceExecutorEventMessage, MillisInstant) -> Unit =
          { message, publishedAt ->
            cloudEventLogger.log(message, publishedAt)
            taskEventHandler.handle(message, publishedAt)
          }

      consumer.startAsync(
          subscription = MainSubscription(ServiceExecutorEventTopic),
          entity = config.serviceName,
          concurrency = config.concurrency,
          process = process,
      )
    }

    return listOf(jobExecutor, jobRetry, jobEvents)
  }

  context(CoroutineScope)
  private suspend fun startWorkflowTagEngine(config: WorkflowTagEngineConfig): Job {
    // Log Workflow State Engine configuration
    logWorkflowTagEngineStart(config)

    // WORKFLOW-TAG
    return with(WorkflowTagEngine.logger) {
      val loggedStorage = LoggedWorkflowTagStorage(this, config.workflowTagStorage)
      val loggedProducer = LoggedInfiniticProducer(this, producer)
      val workflowTagEngine = WorkflowTagEngine(loggedStorage, loggedProducer)

      val cloudEventLogger = CloudEventLogger(
          WorkflowTagEngineTopic,
          config.workflowName,
          cloudEventSourcePrefix,
          beautifyLogs,
      )

      val process: suspend (WorkflowTagEngineMessage, MillisInstant) -> Unit =
          { message, publishedAt ->
            cloudEventLogger.log(message, publishedAt)
            workflowTagEngine.handle(message, publishedAt)
          }

      consumer.startAsync(
          subscription = MainSubscription(WorkflowTagEngineTopic),
          entity = config.workflowName,
          concurrency = config.concurrency,
          process = process,
      )
    }
  }

  context(CoroutineScope)
  private suspend fun startWorkflowStateEngine(config: WorkflowStateEngineConfig): List<Job> {
    // Log Workflow State Engine configuration
    logWorkflowStateEngineStart(config)

    // WORKFLOW-STATE-CMD
    val jobCmd = with(WorkflowStateCmdHandler.logger) {
      val loggedProducer = LoggedInfiniticProducer(this, producer)
      val workflowStateCmdHandler = WorkflowStateCmdHandler(loggedProducer)

      val cloudEventLogger = CloudEventLogger(
          WorkflowStateCmdTopic,
          config.workflowName,
          cloudEventSourcePrefix,
          beautifyLogs,
      )

      val process: suspend (WorkflowStateEngineMessage, MillisInstant) -> Unit =
          { message, publishedAt ->
            cloudEventLogger.log(message, publishedAt)
            workflowStateCmdHandler.handle(message, publishedAt)
          }

      consumer.startAsync(
          subscription = MainSubscription(WorkflowStateCmdTopic),
          entity = config.workflowName,
          concurrency = config.concurrency,
          process = process,
      )
    }

    // WORKFLOW-STATE-ENGINE
    val jobEngine = with(WorkflowStateEngine.logger) {
      val loggedStorage = LoggedWorkflowStateStorage(this, config.workflowStateStorage)
      val loggedProducer = LoggedInfiniticProducer(this, producer)
      val workflowStateEngine = WorkflowStateEngine(loggedStorage, loggedProducer)

      val cloudEventLogger = CloudEventLogger(
          WorkflowStateEngineTopic,
          config.workflowName,
          cloudEventSourcePrefix,
          beautifyLogs,
      )

      val process: suspend (WorkflowStateEngineMessage, MillisInstant) -> Unit =
          { message, publishedAt ->
            cloudEventLogger.log(message, publishedAt)
            workflowStateEngine.handle(message, publishedAt)
          }

      consumer.startAsync(
          subscription = MainSubscription(WorkflowStateEngineTopic),
          entity = config.workflowName,
          concurrency = config.concurrency,
          process = process,
      )
    }

    // WORKFLOW-STATE-TIMERS
    val jobTimers = with(WorkflowStateTimerHandler.logger) {
      val loggedProducer = LoggedInfiniticProducer(this, producer)
      val workflowStateTimerHandler = WorkflowStateTimerHandler(loggedProducer)

      consumer.startAsync(
          subscription = MainSubscription(WorkflowStateTimerTopic),
          entity = config.workflowName,
          concurrency = config.concurrency,
          process = workflowStateTimerHandler::handle,
      )
    }

    // WORKFLOW-STATE-EVENTS
    val jobEvents = with(WorkflowStateEventHandler.logger) {
      val loggedProducer = LoggedInfiniticProducer(this, producer)
      val workflowStateEventHandler = WorkflowStateEventHandler(loggedProducer)

      val cloudEventLogger = CloudEventLogger(
          WorkflowStateEventTopic,
          config.workflowName,
          cloudEventSourcePrefix,
          beautifyLogs,
      )

      val process: suspend (WorkflowStateEventMessage, MillisInstant) -> Unit =
          { message, publishedAt ->
            cloudEventLogger.log(message, publishedAt)
            workflowStateEventHandler.handle(message, publishedAt)
          }

      consumer.startAsync(
          subscription = MainSubscription(WorkflowStateEventTopic),
          entity = config.workflowName,
          concurrency = config.concurrency,
          process = process,
      )
    }

    return listOf(jobCmd, jobEngine, jobTimers, jobEvents)
  }

  context(CoroutineScope, KLogger)
  private suspend fun startWorkflowExecutor(config: WorkflowExecutorConfig): List<Job> {
    // Log Workflow Executor configuration
    logWorkflowExecutorStart(config)

    // WORKFLOW-EXECUTOR
    val jobExecutor = with(TaskExecutor.logger) {
      val loggedProducer = LoggedInfiniticProducer(this, producer)
      val workflowTaskExecutor = TaskExecutor(registry, loggedProducer, client)

      val cloudEventLogger = CloudEventLogger(
          WorkflowExecutorTopic,
          config.workflowName,
          cloudEventSourcePrefix,
          beautifyLogs,
      )

      val process: suspend (ServiceExecutorMessage, MillisInstant) -> Unit =
          { message, publishedAt ->
            cloudEventLogger.log(message, publishedAt)
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
      consumer.startAsync(
          subscription = MainSubscription(WorkflowExecutorTopic),
          entity = config.workflowName,
          concurrency = config.concurrency,
          process = process,
          beforeDlq = beforeDlq,
      )
    }

    // WORKFLOW-EXECUTOR-RETRY
    val jobRetry = with(TaskRetryHandler.logger) {
      val loggedProducer = LoggedInfiniticProducer(this, producer)
      val taskRetryHandler = TaskRetryHandler(loggedProducer)

      consumer.startAsync(
          subscription = MainSubscription(WorkflowExecutorRetryTopic),
          entity = config.workflowName,
          concurrency = config.concurrency,
          process = taskRetryHandler::handle,
      )
    }

    // WORKFLOW-EXECUTOR-EVENT
    val jobEvents = with(TaskEventHandler.logger) {
      val loggedProducer = LoggedInfiniticProducer(this, producer)
      val workflowTaskEventHandler = TaskEventHandler(loggedProducer)

      val cloudEventLogger = CloudEventLogger(
          WorkflowExecutorEventTopic,
          config.workflowName,
          cloudEventSourcePrefix,
          beautifyLogs,
      )

      val handler: suspend (ServiceExecutorEventMessage, MillisInstant) -> Unit =
          { message, publishedAt ->
            cloudEventLogger.log(message, publishedAt)
            workflowTaskEventHandler.handle(message, publishedAt)
          }

      consumer.startAsync(
          subscription = MainSubscription(WorkflowExecutorEventTopic),
          entity = config.workflowName,
          concurrency = config.concurrency,
          process = handler,
      )
    }

    return listOf(jobExecutor, jobRetry, jobEvents)
  }

  private val logMessageSentToDLQ = { message: Message?, e: Exception ->
    logger.error(e) { "Sending message to DLQ ${message ?: "(Not Deserialized)"}" }
  }
}
