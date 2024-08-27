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
import io.infinitic.cloudEvents.SelectionConfig
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.utils.merge
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
import io.infinitic.workers.register.config.UNDEFINED_WITH_RETRY
import io.infinitic.workers.register.config.UNDEFINED_WITH_TIMEOUT
import io.infinitic.workers.register.config.WorkflowConfigDefault
import io.infinitic.workers.registrable.EventListener
import io.infinitic.workers.registrable.RegisteredServiceExecutor
import io.infinitic.workers.registrable.Registrable
import io.infinitic.workers.registrable.ServiceFactory
import io.infinitic.workers.registrable.ServiceTagEngine
import io.infinitic.workers.registrable.WorkerRegistry
import io.infinitic.workers.registrable.WorkflowExecutor
import io.infinitic.workers.registrable.WorkflowFactories
import io.infinitic.workers.registrable.WorkflowStateEngine
import io.infinitic.workers.registrable.WorkflowTagEngine
import io.infinitic.workflows.WorkflowCheckMode
import io.infinitic.workflows.engine.storage.BinaryWorkflowStateStorage
import io.infinitic.workflows.tag.storage.BinaryWorkflowTagStorage
import java.util.concurrent.ConcurrentHashMap

class InfiniticRegister(var logsConfig: LogsConfig) : AutoCloseable {

  // thread-safe set of all storage instances used
  private val storages = ConcurrentHashMap.newKeySet<StorageConfig>()

  val registry = WorkerRegistry()

  var defaultStorage: StorageConfig = StorageConfig()
  var defaultEventListener: EventListenerConfig? = null
  var serviceDefault: ServiceConfigDefault = ServiceConfigDefault()
  var workflowDefault: WorkflowConfigDefault = WorkflowConfigDefault()

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

  /** Register Service Tag Engine */
  @JvmOverloads
  fun registerServiceTagEngine(
    serviceName: String,
    concurrency: Int,
    storageConfig: StorageConfig? = null
  ) {
    val service = ServiceName(serviceName)
    val storage = storageConfig ?: serviceDefault.tagEngine?.storage ?: defaultStorage
    storages.add(storage)

    registry.serviceTagEngines[service] = ServiceTagEngine(
        concurrency,
        BinaryTaskTagStorage(storage.keyValue, storage.keySet),
    ).also {
      logger.info {
        "* Service Tag Engine".padEnd(25) + ": (" +
            "concurrency: ${it.concurrency}, " +
            "storage: ${storage.type}, " +
            "cache: ${storage.cache?.type ?: NONE}, " +
            "compression: ${storage.compression ?: NONE})"
      }
    }
  }

  /** Register Service Executor */
  @JvmOverloads
  fun registerServiceExecutor(
    serviceName: String,
    serviceFactory: ServiceFactory,
    concurrency: Int,
    withTimeout: WithTimeout? = UNDEFINED_WITH_TIMEOUT,
    withRetry: WithRetry? = UNDEFINED_WITH_RETRY,
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
              it.withTimeout?.getTimeoutInSeconds()?.let { String.format("%.2fs", it) } ?: NONE
            }, " +
            "withRetry: ${it.withRetry ?: NONE})"
      }
    }
  }

  /** Register Workflow Tag Engine */
  @JvmOverloads
  fun registerWorkflowTagEngine(
    workflowName: String,
    concurrency: Int,
    storageConfig: StorageConfig? = null,
  ) {
    val storage = storageConfig ?: workflowDefault.tagEngine?.storage ?: defaultStorage
    storages.add(storage)

    registry.workflowTagEngines[WorkflowName(workflowName)] = WorkflowTagEngine(
        concurrency,
        BinaryWorkflowTagStorage(storage.keyValue, storage.keySet),
    ).also {
      logger.info {
        "* Workflow Tag Engine".padEnd(25) + ": (" +
            "concurrency: ${it.concurrency}, " +
            "storage: ${storage.type}, " +
            "cache: ${storage.cache?.type ?: NONE}, " +
            "compression: ${storage.compression ?: NONE})"
      }
    }
  }

  /** Register Workflow Executor */
  @JvmOverloads
  fun registerWorkflowExecutor(
    workflowName: String,
    factories: WorkflowFactories,
    concurrency: Int,
    withTimeout: WithTimeout? = UNDEFINED_WITH_TIMEOUT,
    withRetry: WithRetry? = UNDEFINED_WITH_RETRY,
    checkMode: WorkflowCheckMode? = null,
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

    registry.workflowExecutors[workflow] = WorkflowExecutor(
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
              it.withTimeout?.getTimeoutInSeconds()?.let { String.format("%.2fs", it) } ?: NONE
            }, " +
            "withRetry: ${it.withRetry ?: NONE}" +
            (it.checkMode?.let { ", checkMode: $it" } ?: "") +
            ")"
      }
    }
  }

  /** Register Workflow State Engine */
  @JvmOverloads
  fun registerWorkflowStateEngine(
    workflowName: String,
    concurrency: Int,
    storageConfig: StorageConfig? = null,
  ) {
    val workflow = WorkflowName(workflowName)

    val storage = storageConfig ?: workflowDefault.stateEngine?.storage ?: defaultStorage
    storages.add(storage)

    registry.workflowStateEngines[workflow] = WorkflowStateEngine(
        concurrency,
        BinaryWorkflowStateStorage(storage.keyValue),
    ).also {
      logger.info {
        "* Workflow State Engine".padEnd(25) + ": (" +
            "concurrency: ${it.concurrency}, " +
            "storage: ${storage.type}, " +
            "cache: ${storage.cache?.type ?: NONE}, " +
            "compression: ${storage.compression ?: NONE})"
      }
    }
  }

  @JvmOverloads
  fun registerEventListener(
    eventListener: CloudEventListener,
    concurrency: Int,
    subscriptionName: String? = null,
    services: SelectionConfig = SelectionConfig(),
    workflows: SelectionConfig = SelectionConfig()
  ) {
    registry.eventListener = EventListener(
        eventListener,
        concurrency,
        subscriptionName,
        services,
        workflows,
    ).also {
      logger.info {
        "* Event Listener".padEnd(25) + ": (" +
            "class: ${it.eventListener::class.java.name}" +
            "concurrency: ${it.concurrency}, " +
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
    private val logger = KotlinLogging.logger(InfiniticWorker::class.java.name)

    private const val NONE = "none"

    /** Create [InfiniticRegister] from config */
    @JvmStatic
    fun fromConfig(workerConfig: WorkerConfigInterface): InfiniticRegister =
        InfiniticRegister(workerConfig.logs).apply {

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
          }

          workerConfig.eventListener?.let {
            logger.info { "Event Listener:" }
            with(it) {
              registerEventListener(
                  eventListener = instance,
                  concurrency = concurrency ?: DEFAULT_CONCURRENCY,
                  subscriptionName = subscriptionName,
                  services = services,
                  workflows = workflows,
              )
            }
          }
        }
  }
}
