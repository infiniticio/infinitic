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
import io.infinitic.workers.config.initBatchProcessorMethods
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException
import kotlin.system.exitProcess

fun main() {
  // Enregistre un hook d'arrêt pour capturer "Ctrl+C" et autres événements d'arrêt de la JVM
  Runtime.getRuntime().addShutdownHook(
      Thread {
        println("closing worker...")
        // Code de nettoyage ici
      },
  )

  println("L'application s'exécute, appuyez sur Ctrl+C pour la fermer...")

  try {
    // Simule une application en cours d'exécution
    Thread.sleep(Long.MAX_VALUE)
  } catch (e: InterruptedException) {

    Thread.currentThread().interrupt()
  }
}

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

            config.eventListener?.let {
              logEventListenerStart(it)

              with(logger) {
                consumerFactory.startCloudEventListener(resources, it, cloudEventSourcePrefix)
              }
            }

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
      "* Service Event Listener".padEnd(25) + ": (" +
          "concurrency: ${config.concurrency}, " +
          "class: ${config.listener::class.java.name}" +
          (config.subscriptionName?.let { ", subscription: $it" } ?: "") +
          "batch: ${config.batchConfig.notNullPropertiesToString()})" +
          ")"
    }
  }

  private fun logServiceExecutorStart(config: ServiceExecutorConfig) {
    logger.info {
      "* Service Executor".padEnd(25) + ": (" +
          "concurrency: ${config.concurrency}, " +
          "class: ${config.factory()::class.java.name}, " +
          "timeout: ${config.withTimeout?.toLog()}, " +
          "withRetry: ${config.withRetry ?: NONE}, " +
          "batch: ${config.batch?.notNullPropertiesToString() ?: NONE})"

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
          "withRetry: ${config.withRetry ?: NONE}, " +
          "batch: ${config.batch?.notNullPropertiesToString() ?: NONE}" +
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
          "compression: ${config.storage?.compression ?: NONE}, " +
          "batch: ${config.batch?.notNullPropertiesToString() ?: NONE})"
    }
  }

  private val sendingMessageToDLQ = { "Unable to process message, sending to Dead Letter Queue" }

  /**
   * Starts the Service Tag Engine with the given configuration.
   */
  private suspend fun startServiceTagEngine(
    config: ServiceTagEngineConfig
  ) {
    // Log Service Tag Engine configuration
    logServiceTagEngineStart(config)

    val serviceName = config.serviceName
    val concurrency = config.concurrency

    // Create a producer with the batchConfig
    val producer = producerFactory.newProducer(null)

    with(TaskTagEngine.logger) {
      // producer
      val loggedProducer = LoggedInfiniticProducer(this, producer)
      // storage
      val storage = LoggedTaskTagStorage(this, config.serviceTagStorage)
      // consumer
      val consumer = consumerFactory.newConsumer(
          subscription = MainSubscription(ServiceTagEngineTopic),
          entity = serviceName,
          batchReceivingConfig = null,
      )
      scope.startServiceTagEngine(consumer, loggedProducer, storage, serviceName, concurrency)
    }
  }

  /**
   * Starts the Service Executor with the given configuration.
   */
  private suspend fun startServiceExecutor(
    config: ServiceExecutorConfig
  ) {
    // init batch methods for current factory
    config.initBatchProcessorMethods()

    // Log Service Executor configuration
    logServiceExecutorStart(config)

    val serviceName = config.serviceName
    val concurrency = config.concurrency
    val batchConfig = config.batch

    // Create a producer with the batchConfig
    val producer = producerFactory.newProducer(batchConfig)

    // Executor
    with(TaskExecutor.logger) {
      val loggedProducer = LoggedInfiniticProducer(this, producer)
      val consumer = consumerFactory.newConsumer(
          subscription = MainSubscription(ServiceExecutorTopic),
          entity = serviceName,
          batchReceivingConfig = batchConfig,
      )
      scope.startServiceExecutor(consumer, loggedProducer, serviceName, concurrency, batchConfig)
    }

    // Executor-Retry
    with(TaskRetryHandler.logger) {
      val loggedProducer = LoggedInfiniticProducer(this, producer)
      val consumer = consumerFactory.newConsumer(
          subscription = MainSubscription(ServiceExecutorRetryTopic),
          entity = serviceName,
          batchReceivingConfig = batchConfig,
      )
      scope.startServiceExecutorRetry(consumer, loggedProducer, concurrency, batchConfig)
    }

    // Executor-Event
    with(TaskEventHandler.logger) {
      val loggedProducer = LoggedInfiniticProducer(this, producer)
      val consumer = consumerFactory.newConsumer(
          subscription = MainSubscription(ServiceExecutorEventTopic),
          entity = config.serviceName,
          batchReceivingConfig = config.batch,
      )
      scope.startServiceExecutorEvent(consumer, loggedProducer, config)
    }
  }

  /**
   * Starts the Workflow Tag Engine with the given configuration.
   */
  private suspend fun startWorkflowTagEngine(config: WorkflowTagEngineConfig) {
    // Log Workflow State Engine configuration
    logWorkflowTagEngineStart(config)

    val workflowName = config.workflowName
    val concurrency = config.concurrency

    // Tag-Engine
    with(WorkflowTagEngine.logger) {
      val storage = LoggedWorkflowTagStorage(this, config.workflowTagStorage)
      val producer = LoggedInfiniticProducer(this, producerFactory.newProducer(null))

      val consumer = consumerFactory.newConsumer(
          subscription = MainSubscription(WorkflowTagEngineTopic),
          entity = workflowName,
          batchReceivingConfig = null,
      )
      scope.startWorkflowTagEngine(consumer, producer, storage, workflowName, concurrency)
    }
  }

  /**
   * Starts the Workflow State Engine with the given configuration.
   */
  private suspend fun startWorkflowStateEngine(config: WorkflowStateEngineConfig) {
    // Log Workflow State Engine configuration
    logWorkflowStateEngineStart(config)

    val workflowName = config.workflowName
    val concurrency = config.concurrency
    val configBatch = config.batch
    val producer = producerFactory.newProducer(configBatch)

    // State-Cmd
    with(WorkflowStateCmdHandler.logger) {
      val loggedProducer = LoggedInfiniticProducer(this, producer)
      val consumer = consumerFactory.newConsumer(
          subscription = MainSubscription(WorkflowStateCmdTopic),
          entity = workflowName,
          batchReceivingConfig = configBatch,
      )
      scope.startWorkflowStateCmd(
          consumer,
          loggedProducer,
          workflowName,
          concurrency,
          configBatch,
      )
    }

    // State-Engine
    with(WorkflowStateEngine.logger) {
      val loggedProducer = LoggedInfiniticProducer(this, producer)
      val consumer = consumerFactory.newConsumer(
          subscription = MainSubscription(WorkflowStateEngineTopic),
          entity = workflowName,
          batchReceivingConfig = configBatch,
      )
      val storage = LoggedWorkflowStateStorage(this, config.workflowStateStorage)
      scope.startWorkflowStateEngine(
          consumer,
          loggedProducer,
          storage,
          workflowName,
          concurrency,
          configBatch,
      )
    }

    // State-Timer
    with(WorkflowStateTimerHandler.logger) {
      val loggedProducer = LoggedInfiniticProducer(this, producer)
      val consumer = consumerFactory.newConsumer(
          subscription = MainSubscription(WorkflowStateTimerTopic),
          entity = workflowName,
          batchReceivingConfig = configBatch,
      )
      scope.startWorkflowStateTimer(
          consumer,
          loggedProducer,
          concurrency,
          configBatch,
      )
    }

    // State-Event
    with(WorkflowStateEventHandler.logger) {
      val loggedProducer = LoggedInfiniticProducer(this, producer)
      val consumer = consumerFactory.newConsumer(
          subscription = MainSubscription(WorkflowStateEventTopic),
          entity = workflowName,
          batchReceivingConfig = configBatch,
      )
      scope.startWorkflowStateEvent(
          consumer,
          loggedProducer,
          workflowName,
          concurrency,
          configBatch,
      )
    }
  }

  /**
   * Starts the Workflow Executor with the given configuration.
   */
  private suspend fun startWorkflowExecutor(config: WorkflowExecutorConfig) {
    // Log Workflow Executor configuration
    logWorkflowExecutorStart(config)

    val workflowName = config.workflowName
    val concurrency = config.concurrency
    val configBatch = config.batch
    val producer = producerFactory.newProducer(configBatch)

    // Executor
    with(TaskExecutor.logger) {
      val loggedProducer = LoggedInfiniticProducer(this, producer)
      val consumer = consumerFactory.newConsumer(
          subscription = MainSubscription(WorkflowExecutorTopic),
          entity = workflowName,
          batchReceivingConfig = configBatch,
      )
      scope.startWorkflowExecutor(
          consumer,
          loggedProducer,
          workflowName,
          concurrency,
          configBatch,
      )
    }

    // Executor-Retry
    with(TaskRetryHandler.logger) {
      val loggedProducer = LoggedInfiniticProducer(this, producer)
      val consumer = consumerFactory.newConsumer(
          subscription = MainSubscription(WorkflowExecutorRetryTopic),
          entity = workflowName,
          batchReceivingConfig = configBatch,
      )
      scope.startWorkflowExecutorRetry(
          consumer,
          loggedProducer,
          concurrency,
          configBatch,
      )
    }

    // Executor-Events
    with(TaskEventHandler.logger) {
      val loggedProducer = LoggedInfiniticProducer(this, producer)
      val consumer = consumerFactory.newConsumer(
          subscription = MainSubscription(WorkflowExecutorEventTopic),
          entity = workflowName,
          batchReceivingConfig = configBatch,
      )
      scope.startWorkflowExecutorEvent(
          consumer,
          loggedProducer,
          workflowName,
          concurrency,
          configBatch,
      )
    }
  }

  // TASK-TAG
  context(KLogger)
  private fun CoroutineScope.startServiceTagEngine(
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
        logger = this@KLogger,
        consumer = consumer,
        concurrency = concurrency,
        processor = processor,
    )
  }

  // TASK-EXECUTOR
  context(KLogger)
  private fun CoroutineScope.startServiceExecutor(
    consumer: TransportConsumer<out TransportMessage<ServiceExecutorMessage>>,
    producer: InfiniticProducer,
    serviceName: String,
    concurrency: Int,
    batchConfig: BatchConfig?
  ) {
    val taskExecutor = TaskExecutor(registry, producer, client)

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
          logger = this@KLogger,
          consumer = consumer,
          concurrency = concurrency,
          processor = { message, publishedAt ->
            cloudEventLogger.log(message, publishedAt)
            taskExecutor.process(message)
          },
          beforeDlq = beforeDlq,
      )

      else -> startBatchProcessingWithoutKey(
          logger = this@KLogger,
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
  context(KLogger)
  private fun CoroutineScope.startServiceExecutorRetry(
    consumer: TransportConsumer<out TransportMessage<ServiceExecutorMessage>>,
    producer: InfiniticProducer,
    concurrency: Int,
    batchConfig: BatchConfig?
  ) {
    val taskRetryHandler = TaskRetryHandler(producer)

    when (batchConfig) {
      null -> startProcessingWithoutKey(
          logger = this@KLogger,
          consumer = consumer,
          concurrency = concurrency,
          processor = taskRetryHandler::process,
      )

      else -> startBatchProcessingWithoutKey(
          logger = this@KLogger,
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
  context(KLogger)
  private fun CoroutineScope.startServiceExecutorEvent(
    consumer: TransportConsumer<out TransportMessage<ServiceExecutorEventMessage>>,
    producer: InfiniticProducer,
    config: ServiceExecutorConfig
  ) {
    val taskEventHandler = TaskEventHandler(producer)
    val batchConfig = config.batch

    val cloudEventLogger = CloudEventLogger(
        ServiceExecutorEventTopic,
        config.serviceName,
        cloudEventSourcePrefix,
        beautifyLogs,
    )

    when (batchConfig) {
      null -> startProcessingWithoutKey(
          logger = this@KLogger,
          consumer = consumer,
          concurrency = config.concurrency,
          processor = { message, publishedAt ->
            coroutineScope {
              launch { cloudEventLogger.log(message, publishedAt) }
              launch { taskEventHandler.process(message, publishedAt) }
            }
          },
      )

      else -> startBatchProcessingWithoutKey(
          logger = this@KLogger,
          consumer = consumer,
          concurrency = config.concurrency,
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
  context(KLogger)
  private fun CoroutineScope.startWorkflowTagEngine(
    consumer: TransportConsumer<out TransportMessage<WorkflowTagEngineMessage>>,
    producer: InfiniticProducer,
    storage: WorkflowTagStorage,
    workflowName: String,
    concurrency: Int
  ) {
    val workflowTagEngine = WorkflowTagEngine(storage, producer)

    val cloudEventLogger = CloudEventLogger(
        WorkflowTagEngineTopic,
        workflowName,
        cloudEventSourcePrefix,
        beautifyLogs,
    )

    startProcessingWithKey(
        logger = this@KLogger,
        consumer = consumer,
        concurrency = concurrency,
        processor = { message, publishedAt ->
          cloudEventLogger.log(message, publishedAt)
          workflowTagEngine.process(message, publishedAt)
        },
    )
  }

  // WORKFLOW-STATE-CMD
  context(KLogger)
  private fun CoroutineScope.startWorkflowStateCmd(
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
          logger = this@KLogger,
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
          logger = this@KLogger,
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
  context(KLogger)
  private fun CoroutineScope.startWorkflowStateEngine(
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
          logger = this@KLogger,
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
          logger = this@KLogger,
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
  context(KLogger)
  private fun CoroutineScope.startWorkflowStateTimer(
    consumer: TransportConsumer<out TransportMessage<WorkflowStateEngineMessage>>,
    producer: InfiniticProducer,
    concurrency: Int,
    batchConfig: BatchConfig?
  ) {
    val workflowStateTimerHandler = WorkflowStateTimerHandler(producer)

    when (batchConfig) {
      null -> startProcessingWithoutKey(
          logger = this@KLogger,
          consumer = consumer,
          concurrency = concurrency,
          processor = workflowStateTimerHandler::process,
      )

      else -> startBatchProcessingWithoutKey(
          logger = this@KLogger,
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
  context(KLogger)
  private fun CoroutineScope.startWorkflowStateEvent(
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
          logger = this@KLogger,
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
          logger = this@KLogger,
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
  context(KLogger)
  private fun CoroutineScope.startWorkflowExecutor(
    consumer: TransportConsumer<out TransportMessage<ServiceExecutorMessage>>,
    producer: InfiniticProducer,
    workflowName: String,
    concurrency: Int,
    batchConfig: BatchConfig?
  ) {
    val workflowTaskExecutor = TaskExecutor(registry, producer, client)

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
          logger = this@KLogger,
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
          logger = this@KLogger,
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
  context(KLogger)
  private fun CoroutineScope.startWorkflowExecutorRetry(
    consumer: TransportConsumer<out TransportMessage<ServiceExecutorMessage>>,
    producer: InfiniticProducer,
    concurrency: Int,
    batchConfig: BatchConfig?
  ) {
    val taskRetryHandler = TaskRetryHandler(producer)

    when (batchConfig) {
      null -> startProcessingWithoutKey(
          logger = this@KLogger,
          consumer = consumer,
          concurrency = concurrency,
          processor = taskRetryHandler::process,
      )

      else -> startBatchProcessingWithoutKey(
          logger = this@KLogger,
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
  context(KLogger)
  private fun CoroutineScope.startWorkflowExecutorEvent(
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
          logger = this@KLogger,
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
          logger = this@KLogger,
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
