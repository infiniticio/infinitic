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
import io.infinitic.storage.config.StorageConfig
import io.infinitic.tasks.WithRetry
import io.infinitic.tasks.WithTimeout
import io.infinitic.tasks.tag.storage.BinaryTaskTagStorage
import io.infinitic.workers.InfiniticWorker
import io.infinitic.workers.config.WorkerConfigInterface
import io.infinitic.workers.register.config.DEFAULT_CONCURRENCY
import io.infinitic.workers.register.config.LogsConfig
import io.infinitic.workers.register.config.ServiceConfigDefault
import io.infinitic.workers.register.config.UNDEFINED_EVENT_LISTENER
import io.infinitic.workers.register.config.UNDEFINED_WITH_RETRY
import io.infinitic.workers.register.config.UNDEFINED_WITH_TIMEOUT
import io.infinitic.workers.register.config.WorkflowConfigDefault
import io.infinitic.workflows.WorkflowCheckMode
import io.infinitic.workflows.engine.storage.BinaryWorkflowStateStorage
import io.infinitic.workflows.tag.storage.BinaryWorkflowTagStorage
import java.security.InvalidParameterException
import java.util.concurrent.ConcurrentHashMap

class InfiniticRegisterImpl(override var logsConfig: LogsConfig) : InfiniticRegister {

  // thread-safe set of all storage instances used
  private val storages = ConcurrentHashMap.newKeySet<StorageConfig>()

  override val registry = WorkerRegistry()

  override var defaultStorage: StorageConfig = StorageConfig()
  override var defaultEventListener: EventListenerConfig? = null
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

  /** Register Service Executor */
  override fun registerServiceExecutor(
    serviceName: String,
    serviceFactory: ServiceFactory,
    concurrency: Int,
    withTimeout: WithTimeout?,
    withRetry: WithRetry?,
  ) {
    val service = ServiceName(serviceName)

    val withT = when (withTimeout) {
      null -> null
      UNDEFINED_WITH_TIMEOUT -> serviceDefault.withTimeout
      else -> withTimeout
    }

    val withR = when (withRetry) {
      null -> null
      UNDEFINED_WITH_RETRY -> serviceDefault.retry
      else -> withRetry
    }

    registry.serviceExecutors[service] = RegisteredServiceExecutor(
        concurrency,
        serviceFactory,
        withT,
        withR,
    ).also {
      logger.info {
        "* Service Executor".padEnd(25) + ": (" +
            "concurrency: ${it.concurrency}, " +
            "class: ${it.factory()::class.java.name}, " +
            "timeout: ${
              it.withTimeout?.getTimeoutInSeconds()?.let { String.format("%.2f", it) }
            }s, " +
            "withRetry: ${it.withRetry})"
      }
    }
  }

  /** Register Service Tag Engine */
  override fun registerServiceTagEngine(
    serviceName: String,
    concurrency: Int,
    storageConfig: StorageConfig?
  ) {
    val service = ServiceName(serviceName)
    val storage = storageConfig ?: serviceDefault.tagEngine?.storage ?: defaultStorage
    storages.add(storage)

    registry.serviceTagEngines[service] = RegisteredServiceTagEngine(
        concurrency,
        BinaryTaskTagStorage(storage.keyValue, storage.keySet),
    ).also {
      logger.info {
        "* Service Tag Engine".padEnd(25) + ": (" +
            "concurrency: ${it.concurrency}, " +
            "storage: ${storage.type}, " +
            "cache: ${storage.cache?.type}, " +
            "compression: ${storage.compression})"
      }
    }
  }

  /** Register Service Event Listener */
  override fun registerServiceEventListener(
    serviceName: String,
    concurrency: Int,
    eventListener: CloudEventListener,
    subscriptionName: String?,
  ) {
    val subName = subscriptionName
      ?: serviceDefault.eventListener?.subscriptionName
      ?: defaultEventListener?.subscriptionName

    registry.serviceEventListeners[ServiceName(serviceName)] = RegisteredEventListener(
        eventListener,
        concurrency,
        subName,
    ).also {
      logger.info {
        "* Service Event Listener".padEnd(25) + ": (" +
            "concurrency: ${it.concurrency}, " +
            "class: ${it.eventListener::class.java.name}" +
            (it.subscriptionName?.let { ", subscription: $it" } ?: "") +
            ")"
      }
    }
  }

  /** Register Workflow Executor */
  override fun registerWorkflowExecutor(
    workflowName: String,
    factories: WorkflowFactories,
    concurrency: Int,
    withTimeout: WithTimeout?,
    withRetry: WithRetry?,
    checkMode: WorkflowCheckMode?,
  ) {
    val workflow = WorkflowName(workflowName)

    val withT = when (withTimeout) {
      null -> null
      UNDEFINED_WITH_TIMEOUT -> workflowDefault.withTimeout
      else -> withTimeout
    }

    val withR = when (withRetry) {
      null -> null
      UNDEFINED_WITH_RETRY -> workflowDefault.retry
      else -> withRetry
    }

    registry.workflowExecutors[workflow] = RegisteredWorkflowExecutor(
        workflow,
        factories,
        concurrency,
        withT,
        withR,
        checkMode ?: workflowDefault.checkMode,
    ).also {
      logger.info {
        "* Workflow Executor".padEnd(25) + ": (" +
            "concurrency: ${it.concurrency}, " +
            "classes: ${it.classes.joinToString { it.simpleName }}, " +
            "timeout: ${
              it.withTimeout?.getTimeoutInSeconds()?.let { String.format("%.2f", it) }
            }s, " +
            "withRetry: ${it.withRetry}" +
            (it.checkMode?.let { ", checkMode: $it" } ?: "") +
            ")"
      }
    }
  }

  /** Register Workflow State Engine */
  override fun registerWorkflowStateEngine(
    workflowName: String,
    concurrency: Int,
    storageConfig: StorageConfig?,
  ) {
    val workflow = WorkflowName(workflowName)

    val storage = storageConfig ?: workflowDefault.stateEngine?.storage ?: defaultStorage
    storages.add(storage)

    registry.workflowStateEngines[workflow] = RegisteredWorkflowStateEngine(
        concurrency,
        BinaryWorkflowStateStorage(storage.keyValue),
    ).also {
      logger.info {
        "* Workflow State Engine".padEnd(25) + ": (" +
            "concurrency: ${it.concurrency}, " +
            "storage: ${storage.type}, " +
            "cache: ${storage.cache?.type}, " +
            "compression: ${storage.compression})"
      }
    }
  }

  /** Register Workflow Tag Engine */
  override fun registerWorkflowTagEngine(
    workflowName: String,
    concurrency: Int,
    storageConfig: StorageConfig?,
  ) {
    val storage = storageConfig ?: workflowDefault.tagEngine?.storage ?: defaultStorage
    storages.add(storage)

    registry.workflowTagEngines[WorkflowName(workflowName)] = RegisteredWorkflowTagEngine(
        concurrency,
        BinaryWorkflowTagStorage(storage.keyValue, storage.keySet),
    ).also {
      logger.info {
        "* Workflow Tag Engine".padEnd(25) + ": (" +
            "concurrency: ${it.concurrency}, " +
            "storage: ${storage.type}, " +
            "cache: ${storage.cache?.type}, " +
            "compression: ${storage.compression})"
      }
    }
  }

  /** Register Workflow Event Listener */
  override fun registerWorkflowEventListener(
    workflowName: String,
    concurrency: Int,
    eventListener: CloudEventListener,
    subscriptionName: String?,
  ) {
    val workflow = WorkflowName(workflowName)
    val subName = subscriptionName
      ?: workflowDefault.eventListener?.subscriptionName
      ?: defaultEventListener?.subscriptionName

    registry.workflowEventListeners[workflow] = RegisteredEventListener(
        eventListener,
        concurrency,
        subName,
    ).also {
      logger.info {
        "* Workflow Event Listener".padEnd(25) + ": (" +
            "concurrency: ${it.concurrency}, " +
            "class: ${it.eventListener::class.java.name}" +
            (it.subscriptionName?.let { ", subscription: $it" } ?: "") +
            ")"
      }
    }
  }

  private fun getDefaultServiceConcurrency(name: String) =
      registry.serviceExecutors[ServiceName(name)]?.concurrency
        ?: serviceDefault.concurrency
        ?: DEFAULT_CONCURRENCY

  private fun getDefaultWorkflowConcurrency(name: String) =
      registry.workflowExecutors[WorkflowName(name)]?.concurrency
        ?: workflowDefault.concurrency
        ?: DEFAULT_CONCURRENCY

  companion object {
    private val logger by lazy { KotlinLogging.logger(InfiniticWorker::class.java.name) }

    /** Create [InfiniticRegisterImpl] from config */
    @JvmStatic
    fun fromConfig(workerConfig: WorkerConfigInterface): InfiniticRegisterImpl =
        InfiniticRegisterImpl(workerConfig.logs).apply {

          workerConfig.storage?.let { defaultStorage = it }
          workerConfig.serviceDefault?.let { serviceDefault = it }
          workerConfig.workflowDefault?.let { workflowDefault = it }
          workerConfig.eventListener?.let { defaultEventListener = it }

          for (workflowConfig in workerConfig.workflows) with(workflowConfig) {
            logger.info { "Workflow $name:" }

            // Workflow Executors are registered first, as it defines some default values for the others
            if (allClasses.isNotEmpty()) {
              registerWorkflowExecutor(
                  name,
                  allClasses.map { { it.getDeclaredConstructor().newInstance() } },
                  concurrency ?: getDefaultWorkflowConcurrency(name),
                  withTimeout,
                  withRetry,
                  checkMode,
              )
            }
            // Workflow Tag Engine
            tagEngine?.merge(workflowDefault.tagEngine)?.let {
              registerWorkflowTagEngine(
                  name,
                  it.concurrency ?: getDefaultWorkflowConcurrency(name),
                  it.storage,
              )
            }
            // Workflow State Engine
            stateEngine?.merge(workflowDefault.stateEngine)?.let {
              registerWorkflowStateEngine(
                  name,
                  it.concurrency ?: getDefaultWorkflowConcurrency(name),
                  it.storage,
              )
            }
            // Workflow Event Listener
            eventListener?.merge(workflowDefault.eventListener)?.merge(defaultEventListener)?.let {
              if (it != UNDEFINED_EVENT_LISTENER) when (val instance = it.instance) {
                null -> throw InvalidParameterException("Missing declaration of " + CloudEventListener::class.java.name)
                else -> registerWorkflowEventListener(
                    name,
                    it.concurrency ?: getDefaultWorkflowConcurrency(name),
                    instance,
                    it.subscriptionName,
                )
              }
            }
          }

          for (service in workerConfig.services) with(service) {
            logger.info { "Service $name:" }

            // Service Executors are registered first, as it defines some default values for the others
            `class`?.let {
              registerServiceExecutor(
                  name,
                  { getInstance() },
                  concurrency ?: getDefaultServiceConcurrency(name),
                  withTimeout,
                  withRetry,
              )
            }
            // Service Tag Engine
            tagEngine?.merge(serviceDefault.tagEngine)?.let {
              registerServiceTagEngine(
                  name,
                  it.concurrency ?: getDefaultServiceConcurrency(name),
                  it.storage,
              )
            }
            // Service Event Listener
            eventListener?.merge(serviceDefault.eventListener)?.merge(defaultEventListener)?.let {
              if (it != UNDEFINED_EVENT_LISTENER) when (val instance = it.instance) {
                null -> throw InvalidParameterException("Missing declaration of " + CloudEventListener::class.java.name)
                else -> registerServiceEventListener(
                    name,
                    it.concurrency ?: getDefaultServiceConcurrency(name),
                    instance,
                    it.subscriptionName,
                )
              }
            }
          }
        }
  }
}
