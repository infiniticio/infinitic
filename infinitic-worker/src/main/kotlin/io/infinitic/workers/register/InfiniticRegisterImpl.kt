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
package io.infinitic.workers.register

import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.cloudEvents.CloudEventListener
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.utils.merge
import io.infinitic.common.workers.registry.RegisteredEventListener
import io.infinitic.common.workers.registry.RegisteredEventLogger
import io.infinitic.common.workers.registry.RegisteredServiceExecutor
import io.infinitic.common.workers.registry.RegisteredServiceTagEngine
import io.infinitic.common.workers.registry.RegisteredWorkflowExecutor
import io.infinitic.common.workers.registry.RegisteredWorkflowStateEngine
import io.infinitic.common.workers.registry.RegisteredWorkflowTagEngine
import io.infinitic.common.workers.registry.ServiceFactory
import io.infinitic.common.workers.registry.WorkerRegistry
import io.infinitic.common.workers.registry.WorkflowFactories
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.events.config.EventListenerConfig
import io.infinitic.events.config.EventLoggerConfig
import io.infinitic.logs.LogLevel
import io.infinitic.storage.config.StorageConfig
import io.infinitic.tasks.WithRetry
import io.infinitic.tasks.WithTimeout
import io.infinitic.tasks.tag.storage.BinaryTaskTagStorage
import io.infinitic.workers.config.WorkerConfigInterface
import io.infinitic.workers.register.config.DEFAULT_CONCURRENCY
import io.infinitic.workers.register.config.ServiceConfigDefault
import io.infinitic.workers.register.config.UNDEFINED_TIMEOUT
import io.infinitic.workers.register.config.UNDEFINED_WITH_RETRY
import io.infinitic.workers.register.config.UNDEFINED_WITH_TIMEOUT
import io.infinitic.workers.register.config.WorkflowConfigDefault
import io.infinitic.workflows.WorkflowCheckMode
import io.infinitic.workflows.engine.storage.BinaryWorkflowStateStorage
import io.infinitic.workflows.tag.config.WorkflowTagEngineConfig
import io.infinitic.workflows.tag.storage.BinaryWorkflowTagStorage
import java.security.InvalidParameterException
import java.util.concurrent.ConcurrentHashMap

class InfiniticRegisterImpl : InfiniticRegister {

  var logName: String? = null

  private val logger by lazy { KotlinLogging.logger(logName ?: this::class.java.name) }

  // thread-safe set of all storage instances used
  private val storages = ConcurrentHashMap.newKeySet<StorageConfig>()

  override val registry = WorkerRegistry()

  override var defaultStorage: StorageConfig = StorageConfig()
  override var defaultEventListener: EventListenerConfig? = null
  override var defaultEventLogger: EventLoggerConfig? = null
  override var serviceDefault: ServiceConfigDefault = ServiceConfigDefault()
  override var workflowDefault: WorkflowConfigDefault = WorkflowConfigDefault()

  override fun close() {
    storages.forEach {
      try {
        logger.info { "Closing KeyValueStorage $it" }
        it.keyValue.close()
      } catch (e: Exception) {
        logger.warn(e) { "Unable to close KeyValueStorage $it" }
      }
      try {
        logger.info { "Closing KeySetStorage $it" }
        it.keySet.close()
      } catch (e: Exception) {
        logger.warn(e) { "Unable to close KeySetStorage $it" }
      }
    }
  }

  /** Register task */
  override fun registerServiceExecutor(
    serviceName: String,
    serviceFactory: ServiceFactory,
    concurrency: Int?,
    withTimeout: WithTimeout?,
    withRetry: WithRetry?,
  ) {
    val withT = when (withTimeout) {
      null -> null
      else -> (if (withTimeout == UNDEFINED_WITH_TIMEOUT) null else withTimeout)
        ?: serviceDefault.timeoutInSeconds?.let { WithTimeout { it } }
    }

    val withR = when (withRetry) {
      null -> null
      else -> (if (withRetry == UNDEFINED_WITH_RETRY) null else withRetry)
        ?: serviceDefault.retry
    }

    registry.serviceExecutors[ServiceName(serviceName)] = RegisteredServiceExecutor(
        concurrency ?: serviceDefault.concurrency ?: DEFAULT_CONCURRENCY,
        serviceFactory,
        withT,
        withR,
    ).also {
      logger.info {
        "* service executor".padEnd(25) +
            ": (concurrency: ${it.concurrency}, class: ${it.factory()::class.java.simpleName})"
      }
    }
  }

  override fun registerServiceTagEngine(
    serviceName: String,
    concurrency: Int?,
    storageConfig: StorageConfig?
  ) {
    val service = ServiceName(serviceName)
    val s = storageConfig ?: serviceDefault.tagEngine?.storage ?: defaultStorage

    storages.add(s)

    registry.serviceTagEngines[service] = RegisteredServiceTagEngine(
        concurrency
          ?: serviceDefault.tagEngine?.concurrency
          ?: registry.serviceExecutors[service]?.concurrency
          ?: DEFAULT_CONCURRENCY,
        BinaryTaskTagStorage(s.keyValue, s.keySet),
    ).also {
      logger.info {
        "* service tag engine".padEnd(25) +
            ": (concurrency: ${it.concurrency}, storage: ${s.type}, cache: ${s.cache?.type}, compression: ${s.compression})"
      }
    }
  }

  override fun registerServiceEventListener(
    serviceName: String,
    concurrency: Int?,
    eventListener: CloudEventListener?,
    subscriptionName: String?,
  ) {
    val service = ServiceName(serviceName)
    val defaultConcurrency = registry.serviceExecutors[service]?.concurrency ?: DEFAULT_CONCURRENCY
    val config =
        EventListenerConfig(concurrency = concurrency, subscriptionName = subscriptionName) merge
            serviceDefault.eventListener merge
            defaultEventListener merge
            EventListenerConfig(concurrency = defaultConcurrency)

    val instance = eventListener ?: config.instance ?: throw InvalidParameterException(
        "Missing ${CloudEventListener::class.simpleName} for service $serviceName",
    )

    registry.serviceEventListeners[service] = RegisteredEventListener(
        instance,
        config.concurrency!!,
        config.subscriptionName,
    ).also {
      logger.info {
        "* service event listener ".padEnd(25) +
            ": (class: {${it.eventListener::class.java}}, concurrency: ${it.concurrency}), subscription: ${it.subscriptionName}"
      }
    }
  }

  override fun registerServiceLogger(
    serviceName: String,
    logLevel: LogLevel?,
    loggerName: String?,
    beautify: Boolean?,
    concurrency: Int?,
    subscriptionName: String?,
  ) {
    val service = ServiceName(serviceName)
    val defaultConcurrency = registry.serviceExecutors[service]?.concurrency ?: DEFAULT_CONCURRENCY
    val config =
        EventLoggerConfig(logLevel, loggerName, beautify, concurrency, subscriptionName) merge
            serviceDefault.eventLogger merge
            defaultEventLogger merge
            getDefaultEventLoggerConfig(defaultConcurrency)

    registry.serviceEventLoggers[service] = RegisteredEventLogger(
        config.logLevel!!,
        config.loggerName!!,
        config.beautify!!,
        config.concurrency!!,
        config.subscriptionName,
    ).also {
      logger.info {
        "* service logger ".padEnd(25) +
            ": (level: ${it.logLevel}, beautify: ${it.beautify}, concurrency: ${it.concurrency}, subscriptionName: ${it.subscriptionName})"
      }
    }
  }


  /** Register workflow */
  override fun registerWorkflowExecutor(
    workflowName: String,
    factories: WorkflowFactories,
    concurrency: Int?,
    withTimeout: WithTimeout?,
    withRetry: WithRetry?,
    checkMode: WorkflowCheckMode?,
  ) {
    val workflow = WorkflowName(workflowName)

    val withT = when (withTimeout) {
      null -> null
      else -> (if (withTimeout == UNDEFINED_WITH_TIMEOUT) null else withTimeout)
        ?: workflowDefault.timeoutInSeconds?.let { WithTimeout { it } }
    }

    val withR = when (withRetry) {
      null -> null
      else -> (if (withRetry == UNDEFINED_WITH_RETRY) null else withRetry)
        ?: workflowDefault.retry
    }

    registry.workflowExecutors[workflow] = RegisteredWorkflowExecutor(
        workflow,
        factories,
        concurrency ?: workflowDefault.concurrency ?: DEFAULT_CONCURRENCY,
        withT,
        withR,
        checkMode ?: workflowDefault.checkMode,
    ).also {
      logger.info {
        "* workflow executor".padEnd(25) +
            ": (concurrency: ${it.concurrency}, classes: ${it.classes.joinToString { it.simpleName }})"
      }
    }
  }

  override fun registerWorkflowStateEngine(
    workflowName: String,
    concurrency: Int?,
    storageConfig: StorageConfig?,
  ) {
    val workflow = WorkflowName(workflowName)
    val s = storageConfig ?: workflowDefault.stateEngine?.storage ?: defaultStorage

    storages.add(s)

    registry.workflowStateEngines[workflow] = RegisteredWorkflowStateEngine(
        concurrency
          ?: workflowDefault.stateEngine?.concurrency
          ?: registry.workflowExecutors[workflow]?.concurrency
          ?: DEFAULT_CONCURRENCY,
        BinaryWorkflowStateStorage(s.keyValue),
    ).also {
      logger.info {
        "* workflow state engine".padEnd(25) +
            ": (concurrency: ${it.concurrency}, storage: ${s.type}, cache: ${s.cache?.type}, compression: ${s.compression})"
      }
    }
  }

  override fun registerWorkflowTagEngine(
    workflowName: String,
    concurrency: Int?,
    storageConfig: StorageConfig?,
  ) {
    val workflow = WorkflowName(workflowName)
    val config = WorkflowTagEngineConfig(concurrency, storageConfig) merge
        workflowDefault.stateEngine

    val s = storageConfig ?: workflowDefault.stateEngine?.storage ?: defaultStorage

    storages.add(s)

    registry.workflowTagEngines[WorkflowName(workflowName)] = RegisteredWorkflowTagEngine(
        concurrency
          ?: workflowDefault.tagEngine?.concurrency
          ?: registry.workflowExecutors[workflow]?.concurrency
          ?: DEFAULT_CONCURRENCY,
        BinaryWorkflowTagStorage(s.keyValue, s.keySet),
    ).also {
      logger.info {
        "* workflow tag engine".padEnd(25) +
            ": (concurrency: ${it.concurrency}, storage: ${s.type}, cache: ${s.cache?.type}, compression: ${s.compression})"
      }
    }
  }

  override fun registerWorkflowEventListener(
    workflowName: String,
    concurrency: Int?,
    eventListener: CloudEventListener?,
    subscriptionName: String?,
  ) {
    val workflow = WorkflowName(workflowName)
    val defaultConcurrency =
        registry.workflowExecutors[workflow]?.concurrency ?: DEFAULT_CONCURRENCY
    val config =
        EventListenerConfig(concurrency = concurrency, subscriptionName = subscriptionName) merge
            workflowDefault.eventListener merge
            defaultEventListener merge
            EventListenerConfig(null, defaultConcurrency, null)
    val listener = eventListener ?: config.instance ?: throw InvalidParameterException(
        "Missing ${CloudEventListener::class.simpleName} for workflow $workflowName",
    )
    registry.workflowEventListeners[workflow] = RegisteredEventListener(
        listener,
        config.concurrency!!,
        config.subscriptionName,
    ).also {
      logger.info {
        "* workflow event listener ".padEnd(25) +
            ": (class: ${it.eventListener::class.java}, concurrency: ${it.concurrency}, subscription: ${it.subscriptionName})"
      }
    }
  }

  override fun registerWorkflowEventLogger(
    workflowName: String,
    logLevel: LogLevel?,
    loggerName: String?,
    beautify: Boolean?,
    concurrency: Int?,
    subscriptionName: String?,
  ) {
    val workflow = WorkflowName(workflowName)
    val defaultConcurrency =
        registry.workflowExecutors[workflow]?.concurrency ?: DEFAULT_CONCURRENCY
    val config =
        EventLoggerConfig(logLevel, loggerName, beautify, concurrency, subscriptionName) merge
            workflowDefault.eventLogger merge
            defaultEventLogger merge
            getDefaultEventLoggerConfig(defaultConcurrency)

    registry.workflowEventLoggers[workflow] = RegisteredEventLogger(
        logLevel = config.logLevel!!,
        loggerName = config.loggerName!!,
        beautify = config.beautify!!,
        concurrency = config.concurrency!!,
        config.subscriptionName,
    ).also {
      logger.info {
        "* workflow event logger ".padEnd(25) +
            ": (level: ${it.logLevel}, beautify: ${it.beautify}, concurrency: ${it.concurrency}, subscription: ${it.subscriptionName})"
      }
    }
  }

  private fun getDefaultEventLoggerConfig(concurrency: Int) = EventLoggerConfig(
      LogLevel.INFO,
      "io.infinitic.workers.events",
      true,
      concurrency,
      null,
  )

  companion object {
    /** Create [InfiniticRegisterImpl] from config */
    @JvmStatic
    fun fromConfig(workerConfig: WorkerConfigInterface): InfiniticRegisterImpl =
        InfiniticRegisterImpl().apply {
          workerConfig.storage?.let { defaultStorage = it }
          workerConfig.serviceDefault?.let { serviceDefault = it }
          workerConfig.workflowDefault?.let { workflowDefault = it }
          workerConfig.eventListener?.let { defaultEventListener = it }

          for (w in workerConfig.workflows) {
            logger.info { "Workflow ${w.name}:" }

            // Workflow Executors are registered first, as it defines some default values for the others
            if (w.allClasses.isNotEmpty()) {
              registerWorkflowExecutor(
                  w.name,
                  w.allClasses.map { { it.getDeclaredConstructor().newInstance() } },
                  w.concurrency,
                  w.timeoutInSeconds?.let { if (it == UNDEFINED_TIMEOUT) UNDEFINED_WITH_TIMEOUT else WithTimeout { it } },
                  w.retry?.let { if (it.isDefined) it else UNDEFINED_WITH_RETRY },
                  w.checkMode,
              )
            }
            // Workflow Tag Engine
            w.tagEngine?.let {
              registerWorkflowTagEngine(
                  w.name,
                  it.concurrency,
                  it.storage,
              )
            }
            // Workflow State Engine
            w.stateEngine?.let {
              registerWorkflowStateEngine(
                  w.name,
                  it.concurrency,
                  it.storage,
              )
            }
            // Workflow Event Listener
            w.eventListener?.let {
              val listener = if (it.isDefined) it else null
              if (listener != null || defaultEventListener != null) {
                registerWorkflowEventListener(
                    w.name,
                    listener?.concurrency,
                    listener?.`class`?.let { listener.instance },
                    listener?.subscriptionName,
                )
              }
            }
          }

          for (s in workerConfig.services) {
            logger.info { "Service ${s.name}:" }

            // Service Executors are registered first, as it defines some default values for the others
            s.`class`?.let {
              registerServiceExecutor(
                  s.name,
                  { s.getInstance() },
                  s.concurrency,
                  s.timeoutInSeconds?.let { if (it == UNDEFINED_TIMEOUT) UNDEFINED_WITH_TIMEOUT else WithTimeout { it } },
                  s.retry?.let { if (it.isDefined) it else UNDEFINED_WITH_RETRY },
              )
            }
            // Service Tag Engine
            s.tagEngine?.let {
              registerServiceTagEngine(
                  s.name,
                  it.concurrency,
                  it.storage,
              )
            }
            // Service Event Listener
            s.eventListener?.let {
              val listener = if (it.isDefined) it else null
              if (listener != null || defaultEventListener != null) {
                registerServiceEventListener(
                    s.name,
                    listener?.concurrency,
                    listener?.`class`?.let { listener.instance },
                    listener?.subscriptionName,
                )
              }
            }
          }
        }
  }
}
