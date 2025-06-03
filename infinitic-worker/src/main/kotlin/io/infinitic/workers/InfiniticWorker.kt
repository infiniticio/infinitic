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
import io.infinitic.clients.InfiniticClient
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.events.messages.ServiceExecutorEventMessage
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.tasks.tags.messages.ServiceTagMessage
import io.infinitic.common.tasks.tags.storage.TaskTagStorage
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
import io.infinitic.common.transport.config.BatchConfig
import io.infinitic.common.transport.consumers.startBatchProcessingWithKey
import io.infinitic.common.transport.consumers.startBatchProcessingWithoutKey
import io.infinitic.common.transport.consumers.startProcessingWithKey
import io.infinitic.common.transport.consumers.startProcessingWithoutKey
import io.infinitic.common.transport.interfaces.InfiniticProducer
import io.infinitic.common.transport.interfaces.TransportConsumer
import io.infinitic.common.transport.interfaces.TransportMessage
import io.infinitic.common.transport.logged.LoggedInfiniticProducer
import io.infinitic.common.transport.logged.LoggerWithCounter
import io.infinitic.common.workflows.emptyWorkflowContext
import io.infinitic.common.workflows.engine.messages.WorkflowStateCmdMessage
import io.infinitic.common.workflows.engine.messages.WorkflowStateEngineMessage
import io.infinitic.common.workflows.engine.messages.WorkflowStateEventMessage
import io.infinitic.common.workflows.engine.storage.WorkflowStateStorage
import io.infinitic.common.workflows.tags.messages.WorkflowTagEngineMessage
import io.infinitic.common.workflows.tags.storage.WorkflowTagStorage
import io.infinitic.config.notNullPropertiesToString
import io.infinitic.events.CloudEventLogger
import io.infinitic.events.config.EventListenerConfig
import io.infinitic.events.listeners.startCloudEventListener
import io.infinitic.tasks.Task
import io.infinitic.tasks.UNSET_WITH_RETRY
import io.infinitic.tasks.UNSET_WITH_TIMEOUT
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
import io.infinitic.workers.registry.ExecutorRegistry
import io.infinitic.workflows.Workflow
import io.infinitic.workflows.engine.WorkflowStateCmdHandler
import io.infinitic.workflows.engine.WorkflowStateEngine
import io.infinitic.workflows.engine.WorkflowStateEventHandler
import io.infinitic.workflows.engine.WorkflowStateTimerHandler
import io.infinitic.workflows.engine.storage.LoggedWorkflowStateStorage
import io.infinitic.workflows.tag.WorkflowTagEngine
import io.infinitic.workflows.tag.storage.LoggedWorkflowTagStorage
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException
import kotlin.system.exitProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

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

  /** List of executors used by the worker */
  private val executors = mutableListOf<ExecutorService>()

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
        logger.info { "In Flight Messages:" }
        if (config.services.any { it.tagEngine != null }) {
          with(TaskTagEngine.logger) {
            logger.info { "* TaskTagEngine: $remaining ($received received)" }
          }
        }
        if (config.services.any { it.executor != null } || config.workflows.any { it.executor != null }) {
          with(TaskExecutor.logger) {
            logger.info { "* TaskExecutor: $remaining ($received received)" }
          }
          with(TaskRetryHandler.logger) {
            logger.info { "* TaskRetryHandler: $remaining ($received received)" }
          }
          with(TaskEventHandler.logger) {
            logger.info { "* TaskEventHandler: $remaining ($received received)" }
          }
        }
        if (config.workflows.any { it.tagEngine != null }) {
          with(WorkflowTagEngine.logger) {
            logger.info { "* WorkflowTagEngine: $remaining ($received received)" }
          }
        }
        if (config.workflows.any { it.stateEngine != null }) {
          with(WorkflowStateEngine.logger) {
            logger.info { "* WorkflowStateEngine: $remaining ($received received)" }
          }
          with(WorkflowStateEventHandler.logger) {
            logger.info { "* WorkflowStateEventHandler: $remaining ($received received)" }
          }
          with(WorkflowStateCmdHandler.logger) {
            logger.info { "* WorkflowStateCmdHandler: $remaining ($received received)" }
          }
        }
        // closing Executors
        synchronized(executors) {
          executors.forEach { it.shutdownNow() }
          executors.clear()
        }
        // closing client
        client.close()
      }
      logger.info { "Worker closed." }
    } else {
      logger.warn { "Worker has not started, or is already closing." }
    }
  }

  internal val registry = ExecutorRegistry(config.services, config.workflows)

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

  private val consumerFactory by lazy {
    config.transport.consumerFactory
  }

  private val producerFactory by lazy {
    config.transport.producerFactory.apply { config.name?.let { setName(it) } }
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

    /** Create [InfiniticWorker] from YAML resources */
    @JvmStatic
    fun fromYamlResource(vararg resources: String) =
        InfiniticWorker(InfiniticWorkerConfig.fromYamlResource(*resources))

    /** Create [InfiniticWorker] from YAML files */
    @JvmStatic
    fun fromYamlFile(vararg files: String): InfiniticWorker =
        InfiniticWorker(InfiniticWorkerConfig.fromYamlFile(*files))

    /** Create [InfiniticWorker] from YAML strings */
    @JvmStatic
    fun fromYamlString(vararg yamls: String): InfiniticWorker =
        InfiniticWorker(InfiniticWorkerConfig.fromYamlString(*yamls))
  }

  /**
   * Start worker synchronously. This blocks the current thread.
   */
  fun start(): Unit = try {
    startAsync().join()
  } catch (e: CancellationException) {
    // worker has been closed
    logger.info(e) { "Exiting" }
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
      Runtime.getRuntime().addShutdownHook(
          Thread {
            close()
          }.apply { isDaemon = false },
      )

      // create a new scope
      scope = CoroutineScope(Dispatchers.IO)

      completableStart = CompletableFuture()

      scope.launch {
        try {
          coroutineScope {
            config.services.forEach { serviceConfig ->
              logger.info { "Service ${serviceConfig.name}:" }
              // Start SERVICE TAG ENGINE
              serviceConfig.tagEngine?.let { startServiceTagEngine(this, it) }
              // Start SERVICE EXECUTOR
              serviceConfig.executor?.let { startServiceExecutor(this, it) }
            }

            config.workflows.forEach { workflowConfig ->
              logger.info { "Workflow ${workflowConfig.name}:" }
              // Start WORKFLOW TAG ENGINE
              workflowConfig.tagEngine?.let { startWorkflowTagEngine(this, it) }
              // Start WORKFLOW STATE ENGINE
              workflowConfig.stateEngine?.let { startWorkflowStateEngine(this, it) }
              // Start WORKFLOW EXECUTOR
              workflowConfig.executor?.let { startWorkflowExecutor(this, it) }
            }

            // Start Event Listener
            config.eventListener?.let { startEventListener(this, it) }

            val workerName = producerFactory.getName()

            logger.info {
              "Worker '${workerName}' ready (shutdownGracePeriodSeconds=${shutdownGracePeriodSeconds}s)"
            }
          }
        } catch (e: CancellationException) {
          // coroutine has been canceled
          completableStart.complete(Unit)
        } catch (e: Exception) {
          completableStart.completeExceptionally(e)
        }
      }
    }

    return completableStart
  }

  private fun WithTimeout?.toLog() =
      this?.getTimeoutSeconds()?.let { String.format("%.2fs", it) } ?: NONE

  private fun logEventListenerStart(config: EventListenerConfig) {
    logger.info {
      buildString {
        append("* Event Listener".padEnd(25))
        append(": (")
        append("concurrency: ${config.concurrency}, ")
        append("class: ${config.listener::class.java.simpleName}")
        append(", batch: ${config.batchConfig.notNullPropertiesToString()}")
        if (config.subscriptionName != null) append(", subscription: ${config.subscriptionName}")
        append(")")
      }
    }
  }

  private fun logServiceExecutorStart(config: ServiceExecutorConfig) {
    logger.info {
      buildString {
        append("* Service Executor".padEnd(25))
        append(": (concurrency: ${config.concurrency}")
        config.factory?.let { append(", class: ${it()::class.simpleName}") }
        config.withTimeout?.let { if (it != UNSET_WITH_TIMEOUT) append(", timeout: ${it.toLog()}") }
        config.withRetry?.let { if (it != UNSET_WITH_RETRY) append(", withRetry: $it") }
        config.batch?.let { append(", batch: ${it.notNullPropertiesToString()}") }
        if (config.retryHandlerConcurrency != config.concurrency) {
          append(", retryHandlerConcurrency: ${config.retryHandlerConcurrency}")
        }
        if (config.eventHandlerConcurrency != config.concurrency) {
          append(", eventHandlerConcurrency: ${config.eventHandlerConcurrency}")
        }
        append(")")
      }
    }
  }

  private fun logServiceTagEngineStart(config: ServiceTagEngineConfig) {
    logger.info {
      buildString {
        append("* Service Tag Engine".padEnd(25))
        append(": (concurrency: ${config.concurrency}")
        append(", storage: ${config.storage?.type}")
        config.storage?.cache?.let { append(", cache: ${it.type}") }
        config.storage?.compression?.let { append(", compression: ${it}") }
        append(")")
      }
    }
  }

  private fun logWorkflowExecutorStart(config: WorkflowExecutorConfig) {
    Workflow.setContext(emptyWorkflowContext)
    logger.info {
      buildString {
        append("* Workflow Executor".padEnd(25))
        append(": (concurrency: ${config.concurrency}")
        config.factories.forEachIndexed { index, factory ->
          if (index == 0) append(", classes: ") else append(", ")
          append(factory.invoke()::class.simpleName)
        }
        config.withTimeout?.let { if (it != UNSET_WITH_TIMEOUT) append(", timeout: ${it.toLog()}") }
        config.withRetry?.let { if (it != UNSET_WITH_RETRY) append(", withRetry: $it") }
        config.batch?.let { append(", batch: ${it.notNullPropertiesToString()}") }
        if (config.checkMode != null) append(", checkMode: ${config.checkMode}")
        if (config.retryHandlerConcurrency != config.concurrency) {
          append(", retryHandlerConcurrency: ${config.retryHandlerConcurrency}")
        }
        if (config.eventHandlerConcurrency != config.concurrency) {
          append(", eventHandlerConcurrency: ${config.eventHandlerConcurrency}")
        }
        append(")")
      }
    }
  }

  private fun logWorkflowTagEngineStart(config: WorkflowTagEngineConfig) {
    logger.info {
      buildString {
        append("* Workflow Tag Engine".padEnd(25))
        append(": (concurrency: ${config.concurrency}")
        append(", storage: ${config.storage?.type}")
        config.storage?.cache?.let { append(", cache: ${it.type}") }
        config.storage?.compression?.let { append(", compression: $it") }
        config.batch?.let { append(", batch: ${it.notNullPropertiesToString()}") }
        append(")")
      }
    }
  }

  private fun logWorkflowStateEngineStart(config: WorkflowStateEngineConfig) {
    logger.info {
      buildString {
        append("* Workflow State Engine".padEnd(25))
        append(": (concurrency: ${config.concurrency}")
        append(", storage: ${config.storage?.type}")
        config.storage?.cache?.let { append(", cache: ${it.type}") }
        config.storage?.compression?.let { append(", compression: $it") }
        config.batch?.let { append(", batch: ${it.notNullPropertiesToString()}") }
        if (config.commandHandlerConcurrency != config.concurrency) {
          append(", commandHandlerConcurrency: ${config.commandHandlerConcurrency}")
        }
        if (config.timerHandlerConcurrency != config.concurrency) {
          append(", timerHandlerConcurrency: ${config.timerHandlerConcurrency}")
        }
        if (config.eventHandlerConcurrency != config.concurrency) {
          append(", eventHandlerConcurrency: ${config.eventHandlerConcurrency}")
        }
        append(")")
      }
    }
  }

  private val sendingMessageToDLQ = { "Unable to process message, sending to Dead Letter Queue" }

  /**
   * Starts the Event Listener with the given configuration.
   */
  internal fun startEventListener(
    scope: CoroutineScope,
    config: EventListenerConfig
  ) {
    logEventListenerStart(config)

    with(LoggerWithCounter(logger)) {
      scope.startCloudEventListener(
          consumerFactory = consumerFactory,
          resources = resources,
          config = config,
          cloudEventSourcePrefix = cloudEventSourcePrefix,
      )
    }
  }

  /**
   * Starts the Service Tag Engine with the given configuration.
   */
  internal suspend fun startServiceTagEngine(
    scope: CoroutineScope,
    config: ServiceTagEngineConfig
  ) {
    if (config.concurrency > 0) {
      // Log Service Tag Engine configuration
      logServiceTagEngineStart(config)

      val serviceName = config.serviceName

      // Create a producer with the batchConfig
      val producer = producerFactory.newProducer(null)

      val logger = TaskTagEngine.logger
      val loggedProducer = LoggedInfiniticProducer(logger, producer)
      val storage = LoggedTaskTagStorage(logger, config.serviceTagStorage)
      val consumer = consumerFactory.newConsumer(
          subscription = MainSubscription(ServiceTagEngineTopic),
          entity = serviceName,
          batchReceivingConfig = null,
      )
      scope.startServiceTagEngine(
          logger = logger,
          consumer = consumer,
          producer = loggedProducer,
          storage = storage,
          serviceName = serviceName,
          concurrency = config.concurrency,
      )
    }
  }

  /**
   * Starts the Service Executor with the given configuration.
   */
  internal suspend fun startServiceExecutor(
    scope: CoroutineScope,
    config: ServiceExecutorConfig
  ) {
    // init batch methods for the current factory
    config.initBatchProcessorMethods()

    // Log Service Executor configuration
    logServiceExecutorStart(config)

    val serviceName = config.serviceName
    val batchConfig = config.batch

    // Create a producer with the batchConfig
    val producer = producerFactory.newProducer(batchConfig)

    // Service Executors
    if (config.concurrency > 0) {
      val logger = TaskExecutor.logger
      val loggedProducer = LoggedInfiniticProducer(logger, producer)
      val consumer = consumerFactory.newConsumer(
          subscription = MainSubscription(ServiceExecutorTopic),
          entity = serviceName,
          batchReceivingConfig = batchConfig,
      )
      scope.startServiceExecutor(
          logger = logger,
          consumer = consumer,
          producer = loggedProducer,
          serviceName = serviceName,
          concurrency = config.concurrency,
          batchConfig = batchConfig,
      )
    }

    // Service Executor Retry Handlers
    if (config.retryHandlerConcurrency > 0) {
      val logger = TaskRetryHandler.logger
      val loggedProducer = LoggedInfiniticProducer(logger, producer)
      val consumer = consumerFactory.newConsumer(
          subscription = MainSubscription(ServiceExecutorRetryTopic),
          entity = serviceName,
          batchReceivingConfig = batchConfig,
      )
      scope.startServiceExecutorRetry(
          logger = logger,
          consumer = consumer,
          producer = loggedProducer,
          concurrency = config.retryHandlerConcurrency,
          batchConfig = batchConfig,
      )
    }

    // Executor-Event
    if (config.eventHandlerConcurrency > 0) {
      val logger = TaskEventHandler.logger
      val loggedProducer = LoggedInfiniticProducer(logger, producer)
      val consumer = consumerFactory.newConsumer(
          subscription = MainSubscription(ServiceExecutorEventTopic),
          entity = serviceName,
          batchReceivingConfig = batchConfig,
      )
      scope.startServiceExecutorEvent(
          logger = logger,
          consumer = consumer,
          producer = loggedProducer,
          serviceName = serviceName,
          concurrency = config.eventHandlerConcurrency,
          batchConfig = batchConfig,
      )
    }
  }

  /**
   * Starts the Workflow Tag Engine with the given configuration.
   */
  internal suspend fun startWorkflowTagEngine(
    scope: CoroutineScope,
    config: WorkflowTagEngineConfig
  ) {
    if (config.concurrency > 0) {
      // Log Workflow State Engine configuration
      logWorkflowTagEngineStart(config)

      val workflowName = config.workflowName
      val batchConfig = config.batch
      val producer = producerFactory.newProducer(batchConfig)

      // Tag-Engine
      val logger = WorkflowTagEngine.logger
      val storage = LoggedWorkflowTagStorage(logger, config.workflowTagStorage)
      val loggedProducer = LoggedInfiniticProducer(logger, producer)
      val consumer = consumerFactory.newConsumer(
          subscription = MainSubscription(WorkflowTagEngineTopic),
          entity = workflowName,
          batchReceivingConfig = batchConfig,
      )
      scope.startWorkflowTagEngine(
          logger = logger,
          consumer = consumer,
          producer = loggedProducer,
          storage = storage,
          workflowName = workflowName,
          concurrency = config.concurrency,
          batchConfig = batchConfig,
      )
    }
  }

  /**
   * Starts the Workflow State Engine with the given configuration.
   */
  internal suspend fun startWorkflowStateEngine(
    scope: CoroutineScope,
    config: WorkflowStateEngineConfig
  ) {
    // Log Workflow State Engine configuration
    logWorkflowStateEngineStart(config)

    val workflowName = config.workflowName
    val batchConfig = config.batch
    val producer = producerFactory.newProducer(batchConfig)

    // State-Cmd
    if (config.commandHandlerConcurrency > 0) {
      val logger = WorkflowStateCmdHandler.logger
      val loggedProducer = LoggedInfiniticProducer(logger, producer)
      val consumer = consumerFactory.newConsumer(
          subscription = MainSubscription(WorkflowStateCmdTopic),
          entity = workflowName,
          batchReceivingConfig = batchConfig,
      )
      scope.startWorkflowStateCmd(
          logger = logger,
          consumer = consumer,
          producer = loggedProducer,
          workflowName = workflowName,
          concurrency = config.commandHandlerConcurrency,
          batchConfig = batchConfig,
      )
    }

    // Workflow State Engines
    if (config.concurrency > 0) {
      val logger = WorkflowStateEngine.logger
      val loggedProducer = LoggedInfiniticProducer(logger, producer)
      val consumer = consumerFactory.newConsumer(
          subscription = MainSubscription(WorkflowStateEngineTopic),
          entity = workflowName,
          batchReceivingConfig = batchConfig,
      )
      val storage = LoggedWorkflowStateStorage(logger, config.workflowStateStorage)
      scope.startWorkflowStateEngine(
          logger = logger,
          consumer = consumer,
          producer = loggedProducer,
          storage = storage,
          workflowName = workflowName,
          concurrency = config.concurrency,
          batchConfig = batchConfig,
      )
    }

    // Workflow State Timer Handlers
    if (config.timerHandlerConcurrency > 0) {
      val logger = WorkflowStateTimerHandler.logger
      val loggedProducer = LoggedInfiniticProducer(logger, producer)
      val consumer = consumerFactory.newConsumer(
          subscription = MainSubscription(WorkflowStateTimerTopic),
          entity = workflowName,
          batchReceivingConfig = batchConfig,
      )
      scope.startWorkflowStateTimer(
          logger = logger,
          consumer = consumer,
          producer = loggedProducer,
          concurrency = config.timerHandlerConcurrency,
          batchConfig = batchConfig,
          pastDueSeconds = config.timerHandlerPastDueSeconds,
      )
    }

    // Workflow State Event Handlers
    if (config.eventHandlerConcurrency > 0) {
      val logger = WorkflowStateEventHandler.logger
      val loggedProducer = LoggedInfiniticProducer(logger, producer)
      val consumer = consumerFactory.newConsumer(
          subscription = MainSubscription(WorkflowStateEventTopic),
          entity = workflowName,
          batchReceivingConfig = batchConfig,
      )
      scope.startWorkflowStateEvent(
          logger = logger,
          consumer = consumer,
          producer = loggedProducer,
          workflowName = workflowName,
          concurrency = config.eventHandlerConcurrency,
          batchConfig = batchConfig,
      )
    }
  }

  /**
   * Starts the Workflow Executor with the given configuration.
   */
  internal suspend fun startWorkflowExecutor(
    scope: CoroutineScope,
    config: WorkflowExecutorConfig
  ) {
    // Log Workflow Executor configuration
    logWorkflowExecutorStart(config)

    val workflowName = config.workflowName
    val configBatch = config.batch
    val producer = producerFactory.newProducer(configBatch)

    // Workflow Executors
    if (config.concurrency > 0) {
      val logger = TaskExecutor.logger
      val loggedProducer = LoggedInfiniticProducer(logger, producer)
      val consumer = consumerFactory.newConsumer(
          subscription = MainSubscription(WorkflowExecutorTopic),
          entity = workflowName,
          batchReceivingConfig = configBatch,
      )
      scope.startWorkflowExecutor(
          logger = logger,
          consumer = consumer,
          producer = loggedProducer,
          workflowName = workflowName,
          concurrency = config.concurrency,
          batchConfig = configBatch,
      )
    }

    // Workflow Executor Retry Handlers
    if (config.retryHandlerConcurrency > 0) {
      val logger = TaskRetryHandler.logger
      val loggedProducer = LoggedInfiniticProducer(logger, producer)
      val consumer = consumerFactory.newConsumer(
          subscription = MainSubscription(WorkflowExecutorRetryTopic),
          entity = workflowName,
          batchReceivingConfig = configBatch,
      )
      scope.startWorkflowExecutorRetry(
          logger = logger,
          consumer = consumer,
          producer = loggedProducer,
          concurrency = config.retryHandlerConcurrency,
          batchConfig = configBatch,
      )
    }

    // Workflow Executor Event Handlers
    if (config.eventHandlerConcurrency > 0) {
      val logger = TaskEventHandler.logger
      val loggedProducer = LoggedInfiniticProducer(logger, producer)
      val consumer = consumerFactory.newConsumer(
          subscription = MainSubscription(WorkflowExecutorEventTopic),
          entity = workflowName,
          batchReceivingConfig = configBatch,
      )
      scope.startWorkflowExecutorEvent(
          logger = logger,
          consumer = consumer,
          producer = loggedProducer,
          workflowName = workflowName,
          concurrency = config.eventHandlerConcurrency,
          batchConfig = configBatch,
      )
    }
  }

  // TASK-TAG
  internal fun CoroutineScope.startServiceTagEngine(
    logger: LoggerWithCounter,
    consumer: TransportConsumer<out TransportMessage<ServiceTagMessage>>,
    producer: InfiniticProducer,
    storage: TaskTagStorage,
    serviceName: String,
    concurrency: Int,
  ) {
    val taskTagEngine = TaskTagEngine(storage, producer)

    val cloudEventLogger = CloudEventLogger(
        ServiceTagEngineTopic,
        serviceName,
        cloudEventSourcePrefix,
        beautifyLogs,
    )

    val processor: suspend (ServiceTagMessage, MillisInstant) -> Unit =
        { message, publishedAt ->
          cloudEventLogger.log(message, publishedAt)
          taskTagEngine.process(message, publishedAt)
        }

    startProcessingWithKey(
        logger = logger,
        consumer = consumer,
        concurrency = concurrency,
        processor = processor,
    )
  }

  // TASK-EXECUTOR
  internal fun CoroutineScope.startServiceExecutor(
    logger: LoggerWithCounter,
    consumer: TransportConsumer<out TransportMessage<ServiceExecutorMessage>>,
    producer: InfiniticProducer,
    serviceName: String,
    concurrency: Int,
    batchConfig: BatchConfig?
  ) {
    val executor = Executors.newFixedThreadPool(concurrency)
    synchronized<Unit>(executors) { executors.add(executor) }
    val taskExecutor = TaskExecutor(executor, registry, producer, client)

    val cloudEventLogger = CloudEventLogger(
        ServiceExecutorTopic,
        serviceName,
        cloudEventSourcePrefix,
        beautifyLogs,
    )

    val beforeDlq: suspend (ServiceExecutorMessage, Exception) -> Unit = { message, cause ->
      when (message) {
        is ExecuteTask -> with(taskExecutor) {
          message.sendTaskFailed(cause, Task.meta, sendingMessageToDLQ)
        }
      }
    }

    when (batchConfig) {
      null -> startProcessingWithoutKey(
          logger = logger,
          consumer = consumer,
          concurrency = concurrency,
          processor = { message, publishedAt ->
            cloudEventLogger.log(message, publishedAt)
            taskExecutor.process(message)
          },
          beforeDlq = beforeDlq,
      )

      else -> startBatchProcessingWithoutKey(
          logger = logger,
          consumer = consumer,
          concurrency = concurrency,
          batchConfig = batchConfig,
          getBatchKey = { msg -> taskExecutor.getBatchKey(msg) ?: "" },
          processor = { messageAndPublishingTimes ->
            coroutineScope {
              messageAndPublishingTimes.forEach { (message, publishedAt) ->
                launch { cloudEventLogger.log(message, publishedAt) }
              }
              launch { taskExecutor.batchProcess(messageAndPublishingTimes.map { it.first }) }
            }
          },
          beforeDlq = beforeDlq,
      )
    }
  }

  // TASK-EXECUTOR-RETRY
  internal fun CoroutineScope.startServiceExecutorRetry(
    logger: LoggerWithCounter,
    consumer: TransportConsumer<out TransportMessage<ServiceExecutorMessage>>,
    producer: InfiniticProducer,
    concurrency: Int,
    batchConfig: BatchConfig?
  ) {
    val taskRetryHandler = TaskRetryHandler(producer)

    when (batchConfig) {
      null -> startProcessingWithoutKey(
          logger = logger,
          consumer = consumer,
          concurrency = concurrency,
          processor = taskRetryHandler::process,
      )

      else -> startBatchProcessingWithoutKey(
          logger = logger,
          consumer = consumer,
          concurrency = concurrency,
          batchConfig = batchConfig,
          processor = { messages ->
            coroutineScope {
              messages.forEach { (message, publishedAt) ->
                launch {
                  taskRetryHandler.process(message, publishedAt)
                }
              }
            }
          },
      )
    }
  }

  // TASK-EVENTS
  internal fun CoroutineScope.startServiceExecutorEvent(
    logger: LoggerWithCounter,
    consumer: TransportConsumer<out TransportMessage<ServiceExecutorEventMessage>>,
    producer: InfiniticProducer,
    serviceName: String,
    concurrency: Int,
    batchConfig: BatchConfig?
  ) {
    val taskEventHandler = TaskEventHandler(producer)

    val cloudEventLogger = CloudEventLogger(
        ServiceExecutorEventTopic,
        serviceName,
        cloudEventSourcePrefix,
        beautifyLogs,
    )

    when (batchConfig) {
      null -> startProcessingWithoutKey(
          logger = logger,
          consumer = consumer,
          concurrency = concurrency,
          processor = { message, publishedAt ->
            coroutineScope {
              launch { cloudEventLogger.log(message, publishedAt) }
              launch { taskEventHandler.process(message, publishedAt) }
            }
          },
      )

      else -> startBatchProcessingWithoutKey(
          logger = logger,
          consumer = consumer,
          concurrency = concurrency,
          batchConfig = batchConfig,
          processor = { messages ->
            coroutineScope {
              messages.forEach { (message, publishedAt) ->
                launch { cloudEventLogger.log(message, publishedAt) }
                launch { taskEventHandler.process(message, publishedAt) }
              }
            }
          },
      )
    }
  }

  // WORKFLOW-TAG
  internal fun CoroutineScope.startWorkflowTagEngine(
    logger: LoggerWithCounter,
    consumer: TransportConsumer<out TransportMessage<WorkflowTagEngineMessage>>,
    producer: InfiniticProducer,
    storage: WorkflowTagStorage,
    workflowName: String,
    concurrency: Int,
    batchConfig: BatchConfig?
  ) {
    val workflowTagEngine = WorkflowTagEngine(storage, producer)

    val cloudEventLogger = CloudEventLogger(
        WorkflowTagEngineTopic,
        workflowName,
        cloudEventSourcePrefix,
        beautifyLogs,
    )

    when (batchConfig) {
      null -> startProcessingWithKey(
          logger = logger,
          consumer = consumer,
          concurrency = concurrency,
          processor = { message, publishedAt ->
            cloudEventLogger.log(message, publishedAt)
            workflowTagEngine.process(message, publishedAt)
          },
      )

      else -> startBatchProcessingWithKey(
          logger = logger,
          consumer = consumer,
          concurrency = concurrency,
          batchConfig = batchConfig,
          batchProcessor = { messages ->
            coroutineScope {
              messages.forEach { (message, publishedAt) ->
                launch { cloudEventLogger.log(message, publishedAt) }
              }
              launch { workflowTagEngine.batchProcess(messages) }
            }
          },
      )
    }
  }

  // WORKFLOW-STATE-CMD
  internal fun CoroutineScope.startWorkflowStateCmd(
    logger: LoggerWithCounter,
    consumer: TransportConsumer<out TransportMessage<WorkflowStateCmdMessage>>,
    producer: InfiniticProducer,
    workflowName: String,
    concurrency: Int,
    batchConfig: BatchConfig?
  ) {
    val workflowStateCmdHandler = WorkflowStateCmdHandler(producer)

    val cloudEventLogger = CloudEventLogger(
        WorkflowStateCmdTopic,
        workflowName,
        cloudEventSourcePrefix,
        beautifyLogs,
    )

    when (batchConfig) {
      null -> startProcessingWithKey(
          logger = logger,
          consumer = consumer,
          concurrency = concurrency,
          processor = { message, publishedAt ->
            coroutineScope {
              launch { cloudEventLogger.log(message, publishedAt) }
              launch { workflowStateCmdHandler.process(message, publishedAt) }
            }
          },
      )

      else -> startBatchProcessingWithKey(
          logger = logger,
          consumer = consumer,
          concurrency = concurrency,
          batchConfig = batchConfig,
          batchProcessor = { messages ->
            coroutineScope {
              messages.forEach { (message, publishedAt) ->
                launch { cloudEventLogger.log(message, publishedAt) }
              }
              launch { workflowStateCmdHandler.batchProcess(messages) }
            }
          },
      )
    }
  }

  // WORKFLOW-STATE-ENGINE
  internal fun CoroutineScope.startWorkflowStateEngine(
    logger: LoggerWithCounter,
    consumer: TransportConsumer<out TransportMessage<WorkflowStateEngineMessage>>,
    producer: InfiniticProducer,
    storage: WorkflowStateStorage,
    workflowName: String,
    concurrency: Int,
    batchConfig: BatchConfig?
  ) {
    val workflowStateEngine = WorkflowStateEngine(storage, producer)

    val cloudEventLogger = CloudEventLogger(
        WorkflowStateEngineTopic,
        workflowName,
        cloudEventSourcePrefix,
        beautifyLogs,
    )

    when (batchConfig) {
      null -> startProcessingWithKey(
          logger = logger,
          consumer = consumer,
          concurrency = concurrency,
          processor = { message, publishedAt ->
            coroutineScope {
              launch { cloudEventLogger.log(message, publishedAt) }
              launch { workflowStateEngine.process(message, publishedAt) }
            }
          },
      )

      else -> startBatchProcessingWithKey(
          logger = logger,
          consumer = consumer,
          concurrency = concurrency,
          batchConfig = batchConfig,
          batchProcessor = { messages ->
            coroutineScope {
              messages.forEach { (message, publishedAt) ->
                launch { cloudEventLogger.log(message, publishedAt) }
              }
              launch { workflowStateEngine.batchProcess(messages) }
            }
          },
      )
    }
  }

  // WORKFLOW-STATE-TIMERS
  internal fun CoroutineScope.startWorkflowStateTimer(
    logger: LoggerWithCounter,
    consumer: TransportConsumer<out TransportMessage<WorkflowStateEngineMessage>>,
    producer: InfiniticProducer,
    concurrency: Int,
    batchConfig: BatchConfig?,
    pastDueSeconds: Long
  ) {
    val workflowStateTimerHandler = WorkflowStateTimerHandler(producer, pastDueSeconds)

    when (batchConfig) {
      null -> startProcessingWithoutKey(
          logger = logger,
          consumer = consumer,
          concurrency = concurrency,
          processor = workflowStateTimerHandler::process,
      )

      else -> startBatchProcessingWithoutKey(
          logger = logger,
          consumer = consumer,
          concurrency = concurrency,
          batchConfig = batchConfig,
          processor = { messages ->
            coroutineScope {
              messages.forEach { (message, publishedAt) ->
                launch { workflowStateTimerHandler.process(message, publishedAt) }
              }
            }
          },
      )
    }
  }

  // WORKFLOW-STATE-EVENTS
  internal fun CoroutineScope.startWorkflowStateEvent(
    logger: LoggerWithCounter,
    consumer: TransportConsumer<out TransportMessage<WorkflowStateEventMessage>>,
    producer: InfiniticProducer,
    workflowName: String,
    concurrency: Int,
    batchConfig: BatchConfig?
  ) {
    val workflowStateEventHandler = WorkflowStateEventHandler(producer)

    val cloudEventLogger = CloudEventLogger(
        WorkflowStateEventTopic,
        workflowName,
        cloudEventSourcePrefix,
        beautifyLogs,
    )

    when (batchConfig) {
      null -> startProcessingWithoutKey(
          logger = logger,
          consumer = consumer,
          concurrency = concurrency,
          processor = { message, publishedAt ->
            coroutineScope {
              launch { cloudEventLogger.log(message, publishedAt) }
              launch { workflowStateEventHandler.process(message, publishedAt) }
            }
          },
      )

      else -> startBatchProcessingWithoutKey(
          logger = logger,
          consumer = consumer,
          concurrency = concurrency,
          batchConfig = batchConfig,
          processor = { messages ->
            coroutineScope {
              messages.forEach { (message, publishedAt) ->
                launch { cloudEventLogger.log(message, publishedAt) }
                launch { workflowStateEventHandler.process(message, publishedAt) }
              }
            }
          },
      )
    }
  }

  // WORKFLOW-EXECUTOR
  internal fun CoroutineScope.startWorkflowExecutor(
    logger: LoggerWithCounter,
    consumer: TransportConsumer<out TransportMessage<ServiceExecutorMessage>>,
    producer: InfiniticProducer,
    workflowName: String,
    concurrency: Int,
    batchConfig: BatchConfig?
  ) {
    val executor = Executors.newFixedThreadPool(concurrency * (batchConfig?.maxMessages ?: 1))
    synchronized<Unit>(executors) { executors.add(executor) }
    val workflowTaskExecutor = TaskExecutor(executor, registry, producer, client)

    val cloudEventLogger = CloudEventLogger(
        WorkflowExecutorTopic,
        workflowName,
        cloudEventSourcePrefix,
        beautifyLogs,
    )

    val beforeDlq: suspend (ServiceExecutorMessage?, Exception) -> Unit = { message, cause ->
      when (message) {
        null -> Unit
        is ExecuteTask -> with(workflowTaskExecutor) {
          message.sendTaskFailed(cause, Task.meta, sendingMessageToDLQ)
        }
      }
    }

    when (batchConfig) {
      null -> startProcessingWithoutKey(
          logger = logger,
          consumer = consumer,
          concurrency = concurrency,
          processor = { message, publishedAt ->
            coroutineScope {
              launch { cloudEventLogger.log(message, publishedAt) }
              launch { workflowTaskExecutor.process(message) }
            }
          },
          beforeDlq = beforeDlq,
      )

      else -> startBatchProcessingWithoutKey(
          logger = logger,
          consumer = consumer,
          concurrency = concurrency,
          batchConfig = batchConfig,
          processor = { messages ->
            coroutineScope {
              messages.forEach { (message, publishedAt) ->
                launch { cloudEventLogger.log(message, publishedAt) }
                launch { workflowTaskExecutor.process(message) }
              }
            }
          },
          beforeDlq = beforeDlq,
      )
    }
  }

  // WORKFLOW-EXECUTOR-RETRY
  internal fun CoroutineScope.startWorkflowExecutorRetry(
    logger: LoggerWithCounter,
    consumer: TransportConsumer<out TransportMessage<ServiceExecutorMessage>>,
    producer: InfiniticProducer,
    concurrency: Int,
    batchConfig: BatchConfig?
  ) {
    val taskRetryHandler = TaskRetryHandler(producer)

    when (batchConfig) {
      null -> startProcessingWithoutKey(
          logger = logger,
          consumer = consumer,
          concurrency = concurrency,
          processor = taskRetryHandler::process,
      )

      else -> startBatchProcessingWithoutKey(
          logger = logger,
          consumer = consumer,
          concurrency = concurrency,
          batchConfig = batchConfig,
          processor = { messages ->
            coroutineScope {
              messages.forEach { (message, publishedAt) ->
                launch { taskRetryHandler.process(message, publishedAt) }
              }
            }
          },
      )
    }
  }

  // WORKFLOW-EXECUTOR-EVENT
  internal fun CoroutineScope.startWorkflowExecutorEvent(
    logger: LoggerWithCounter,
    consumer: TransportConsumer<out TransportMessage<ServiceExecutorEventMessage>>,
    producer: InfiniticProducer,
    workflowName: String,
    concurrency: Int,
    batchConfig: BatchConfig?
  ) {
    val workflowTaskEventHandler = TaskEventHandler(producer)

    val cloudEventLogger = CloudEventLogger(
        WorkflowExecutorEventTopic,
        workflowName,
        cloudEventSourcePrefix,
        beautifyLogs,
    )

    when (batchConfig) {
      null -> startProcessingWithoutKey(
          logger = logger,
          consumer = consumer,
          concurrency = concurrency,
          processor = { message, publishedAt ->
            coroutineScope {
              launch { cloudEventLogger.log(message, publishedAt) }
              launch { workflowTaskEventHandler.process(message, publishedAt) }
            }
          },
      )

      else -> startBatchProcessingWithoutKey(
          logger = logger,
          consumer = consumer,
          concurrency = concurrency,
          batchConfig = batchConfig,
          processor = { messages ->
            coroutineScope {
              messages.forEach { (message, publishedAt) ->
                launch { cloudEventLogger.log(message, publishedAt) }
                launch { workflowTaskEventHandler.process(message, publishedAt) }
              }
            }
          },
      )
    }
  }

  private val logMessageSentToDLQ = { message: Message?, e: Exception ->
    logger.error(e) { "Sending message to DLQ ${message ?: "(Not Deserialized)"}" }
  }
}
