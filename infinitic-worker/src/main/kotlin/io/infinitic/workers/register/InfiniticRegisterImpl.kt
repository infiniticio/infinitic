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
import io.infinitic.cache.config.Cache
import io.infinitic.cloudEvents.CloudEventListener
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.workers.registry.RegisteredEventListener
import io.infinitic.common.workers.registry.RegisteredServiceExecutor
import io.infinitic.common.workers.registry.RegisteredServiceTag
import io.infinitic.common.workers.registry.RegisteredWorkflowEngine
import io.infinitic.common.workers.registry.RegisteredWorkflowExecutor
import io.infinitic.common.workers.registry.RegisteredWorkflowTag
import io.infinitic.common.workers.registry.ServiceFactory
import io.infinitic.common.workers.registry.WorkerRegistry
import io.infinitic.common.workers.registry.WorkflowFactories
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.events.config.EventListener
import io.infinitic.storage.Storage
import io.infinitic.tasks.WithRetry
import io.infinitic.tasks.WithTimeout
import io.infinitic.tasks.tag.storage.BinaryTaskTagStorage
import io.infinitic.workers.config.WorkerConfigInterface
import io.infinitic.workers.register.config.DEFAULT_CONCURRENCY
import io.infinitic.workers.register.config.ServiceDefault
import io.infinitic.workers.register.config.UNDEFINED_TIMEOUT
import io.infinitic.workers.register.config.UNDEFINED_WITH_RETRY
import io.infinitic.workers.register.config.UNDEFINED_WITH_TIMEOUT
import io.infinitic.workers.register.config.WorkflowDefault
import io.infinitic.workers.storage.CachedKeySetStorage
import io.infinitic.workers.storage.CachedKeyValueStorage
import io.infinitic.workflows.WorkflowCheckMode
import io.infinitic.workflows.engine.storage.BinaryWorkflowStateStorage
import io.infinitic.workflows.tag.storage.BinaryWorkflowTagStorage
import java.security.InvalidParameterException
import java.util.concurrent.ConcurrentHashMap

class InfiniticRegisterImpl : InfiniticRegister {

  var logName: String? = null

  private val logger by lazy { KotlinLogging.logger(logName ?: this::class.java.name) }

  // thread-safe set of all storage instances used
  private val storages = ConcurrentHashMap.newKeySet<Storage>()

  override val registry = WorkerRegistry()

  override var defaultStorage: Storage = Storage()
  override var defaultCache: Cache = Cache()
  override var defaultEventListener: EventListener? = null

  override var serviceDefault: ServiceDefault = ServiceDefault()
  override var workflowDefault: WorkflowDefault = WorkflowDefault()

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
    storage: Storage?,
    cache: Cache?
  ) {
    val service = ServiceName(serviceName)
    val s = storage ?: serviceDefault.tagEngine?.storage ?: defaultStorage
    val c = cache ?: serviceDefault.tagEngine?.cache ?: defaultCache

    storages.add(s)

    registry.serviceTags[service] = RegisteredServiceTag(
        concurrency
          ?: serviceDefault.tagEngine?.concurrency
          ?: registry.serviceExecutors[service]?.concurrency
          ?: DEFAULT_CONCURRENCY,
        BinaryTaskTagStorage(
            CachedKeyValueStorage(c.keyValue, s.keyValue),
            CachedKeySetStorage(c.keySet, s.keySet),
        ),
    ).also {
      logger.info {
        "* service tag engine".padEnd(25) +
            ": (concurrency: ${it.concurrency}, storage: ${s.type}, cache: ${c.type})"
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

    registry.serviceListeners[service] = RegisteredEventListener(
        eventListener
          ?: serviceDefault.eventListener?.instance
          ?: defaultEventListener?.instance
          ?: throw InvalidParameterException("Missing ${CloudEventListener::class.simpleName} at registration for service $serviceName"),
        concurrency
          ?: serviceDefault.eventListener?.concurrency
          ?: defaultEventListener?.concurrency
          ?: registry.serviceExecutors[service]?.concurrency
          ?: DEFAULT_CONCURRENCY,
        subscriptionName
          ?: serviceDefault.eventListener?.subscriptionName
          ?: defaultEventListener?.subscriptionName,
    ).also {
      logger.info {
        "* service event listener ".padEnd(25) +
            ": (concurrency: ${it.concurrency})"
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
    storage: Storage?,
    cache: Cache?
  ) {
    val workflow = WorkflowName(workflowName)
    val s = storage ?: workflowDefault.stateEngine?.storage ?: defaultStorage
    val c = cache ?: workflowDefault.stateEngine?.cache ?: defaultCache

    storages.add(s)

    registry.workflowEngines[workflow] = RegisteredWorkflowEngine(
        concurrency
          ?: workflowDefault.stateEngine?.concurrency
          ?: registry.workflowExecutors[workflow]?.concurrency
          ?: DEFAULT_CONCURRENCY,
        BinaryWorkflowStateStorage(
            CachedKeyValueStorage(c.keyValue, s.keyValue),
        ),
    ).also {
      logger.info {
        "* workflow state engine".padEnd(25) +
            ": (concurrency: ${it.concurrency}, storage: ${s.type}, cache: ${c.type})"
      }
    }
  }

  override fun registerWorkflowTagEngine(
    workflowName: String,
    concurrency: Int?,
    storage: Storage?,
    cache: Cache?
  ) {
    val workflow = WorkflowName(workflowName)
    val s = storage ?: workflowDefault.stateEngine?.storage ?: defaultStorage
    val c = cache ?: workflowDefault.stateEngine?.cache ?: defaultCache

    storages.add(s)

    registry.workflowTags[WorkflowName(workflowName)] = RegisteredWorkflowTag(
        concurrency
          ?: workflowDefault.tagEngine?.concurrency
          ?: registry.workflowExecutors[workflow]?.concurrency
          ?: DEFAULT_CONCURRENCY,
        BinaryWorkflowTagStorage(
            CachedKeyValueStorage(c.keyValue, s.keyValue),
            CachedKeySetStorage(c.keySet, s.keySet),
        ),
    ).also {
      logger.info {
        "* workflow tag engine".padEnd(25) +
            ": (concurrency: ${it.concurrency}, storage: ${s.type}, cache: ${c.type})"
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

    registry.workflowListeners[workflow] = RegisteredEventListener(
        eventListener
          ?: workflowDefault.eventListener?.instance
          ?: defaultEventListener?.instance
          ?: throw InvalidParameterException("Missing ${CloudEventListener::class.simpleName} at registration for workflow $workflow"),
        concurrency
          ?: workflowDefault.eventListener?.concurrency
          ?: defaultEventListener?.concurrency
          ?: registry.workflowExecutors[workflow]?.concurrency
          ?: DEFAULT_CONCURRENCY,
        subscriptionName
          ?: workflowDefault.eventListener?.subscriptionName
          ?: defaultEventListener?.subscriptionName,
    ).also {
      logger.info {
        "* workflow event listener ".padEnd(25) +
            ": (concurrency: ${it.concurrency})"
      }
    }
  }

  companion object {
    /** Create [InfiniticRegisterImpl] from config */
    @JvmStatic
    fun fromConfig(workerConfig: WorkerConfigInterface): InfiniticRegisterImpl =
        InfiniticRegisterImpl().apply {
          workerConfig.storage?.let { defaultStorage = it }
          workerConfig.cache?.let { defaultCache = it }
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
                  it.cache,
              )
            }
            // Workflow State Engine
            w.stateEngine?.let {
              registerWorkflowStateEngine(
                  w.name,
                  it.concurrency,
                  it.storage,
                  it.cache,
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
                  it.cache,
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
