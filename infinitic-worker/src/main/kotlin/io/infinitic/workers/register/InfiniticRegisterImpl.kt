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
import io.infinitic.common.events.CloudEventListener
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.workers.registry.RegisteredEventListener
import io.infinitic.common.workers.registry.RegisteredServiceExecutor
import io.infinitic.common.workers.registry.RegisteredServiceTag
import io.infinitic.common.workers.registry.RegisteredWorkflowEngine
import io.infinitic.common.workers.registry.RegisteredWorkflowExecutor
import io.infinitic.common.workers.registry.RegisteredWorkflowTag
import io.infinitic.common.workers.registry.ServiceFactory
import io.infinitic.common.workers.registry.WorkerRegistry
import io.infinitic.common.workers.registry.WorkflowClassList
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.storage.config.Storage
import io.infinitic.tasks.WithRetry
import io.infinitic.tasks.WithTimeout
import io.infinitic.tasks.tag.storage.BinaryTaskTagStorage
import io.infinitic.workers.config.WorkerConfigInterface
import io.infinitic.workers.storage.CachedKeySetStorage
import io.infinitic.workers.storage.CachedKeyValueStorage
import io.infinitic.workflows.WorkflowCheckMode
import io.infinitic.workflows.engine.storage.BinaryWorkflowStateStorage
import io.infinitic.workflows.tag.storage.BinaryWorkflowTagStorage
import java.util.concurrent.ConcurrentHashMap

class InfiniticRegisterImpl : InfiniticRegister {

  var logName: String? = null

  private val logger by lazy { KotlinLogging.logger(logName ?: this::class.java.name) }

  // thread-safe set of all storage instances used
  private val storages = ConcurrentHashMap.newKeySet<Storage>()

  override val registry = WorkerRegistry()

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
    concurrency: Int,
    withTimeout: WithTimeout?,
    withRetry: WithRetry?,
  ) {
    logger.info {
      "* task executor".padEnd(25) +
          ": (concurrency: $concurrency, class: ${serviceFactory()::class.java.name})"
    }
    registry.serviceExecutors[ServiceName(serviceName)] =
        RegisteredServiceExecutor(concurrency, serviceFactory, withTimeout, withRetry)

  }

  override fun registerServiceTagEngine(
    serviceName: String,
    concurrency: Int,
    storage: Storage,
    cache: Cache
  ) {
    storages.add(storage)

    logger.info {
      "* task tag ".padEnd(25) + ": (concurrency: $concurrency, storage: ${storage.type}, cache: ${cache.type})"
    }

    registry.serviceTags[ServiceName(serviceName)] = RegisteredServiceTag(
        concurrency,
        BinaryTaskTagStorage(
            CachedKeyValueStorage(cache.keyValue, storage.keyValue),
            CachedKeySetStorage(cache.keySet, storage.keySet),
        ),
    )
  }

  override fun registerServiceEventListener(
    serviceName: String,
    concurrency: Int,
    eventListener: CloudEventListener,
    subscriptionName: String?,
  ) {
    logger.info { "* event listener ".padEnd(25) + ": (concurrency: $concurrency)" }

    registry.serviceListeners[ServiceName(serviceName)] = RegisteredEventListener(
        eventListener,
        concurrency,
        subscriptionName,
    )
  }


  /** Register workflow */
  override fun registerWorkflowExecutor(
    workflowName: String,
    classes: WorkflowClassList,
    concurrency: Int,
    withTimeout: WithTimeout?,
    withRetry: WithRetry?,
    checkMode: WorkflowCheckMode?,
  ) {
    logger.info {
      "* workflow executor".padEnd(25) + ": (concurrency: $concurrency, class: ${classes.joinToString { it.name }})"
    }

    val wfName = WorkflowName(workflowName)
    registry.workflowExecutors[wfName] = RegisteredWorkflowExecutor(
        wfName,
        classes.distinct(),
        concurrency,
        withTimeout,
        withRetry,
        checkMode,
    )
  }

  override fun registerWorkflowStateEngine(
    workflowName: String,
    concurrency: Int,
    storage: Storage,
    cache: Cache
  ) {
    storages.add(storage)

    logger.info {
      "* workflow engine".padEnd(25) +
          ": (concurrency: $concurrency, storage: ${storage.type}, cache: ${cache.type})"
    }

    registry.workflowEngines[WorkflowName(workflowName)] = RegisteredWorkflowEngine(
        concurrency,
        BinaryWorkflowStateStorage(
            CachedKeyValueStorage(cache.keyValue, storage.keyValue),
        ),
    )
  }

  override fun registerWorkflowTagEngine(
    workflowName: String,
    concurrency: Int,
    storage: Storage,
    cache: Cache
  ) {
    storages.add(storage)

    logger.info {
      "* workflow tag ".padEnd(25) +
          ": (concurrency: $concurrency, storage: ${storage.type}, cache: ${cache.type})"
    }

    registry.workflowTags[WorkflowName(workflowName)] = RegisteredWorkflowTag(
        concurrency,
        BinaryWorkflowTagStorage(
            CachedKeyValueStorage(cache.keyValue, storage.keyValue),
            CachedKeySetStorage(cache.keySet, storage.keySet),
        ),
    )
  }

  override fun registerWorkflowEventListener(
    workflowName: String,
    concurrency: Int,
    eventListener: CloudEventListener,
    subscriptionName: String?,
  ) {
    logger.info { "* event listener ".padEnd(25) + ": (concurrency: $concurrency)" }

    registry.workflowListeners[WorkflowName(workflowName)] = RegisteredEventListener(
        eventListener,
        concurrency,
        subscriptionName,
    )
  }

  companion object {
    /** Create [InfiniticRegisterImpl] from config */
    @JvmStatic
    fun fromConfig(workerConfig: WorkerConfigInterface): InfiniticRegisterImpl =
        InfiniticRegisterImpl().apply {
          for (w in workerConfig.workflows) {
            logger.info { "Workflow ${w.name}:" }

            w.tagEngine?.let {
              registerWorkflowTagEngine(
                  w.name,
                  it.concurrency!!,
                  it.storage!!,
                  it.cache!!,
              )
            }
            w.stateEngine?.let {
              registerWorkflowStateEngine(
                  w.name,
                  it.concurrency!!,
                  it.storage!!,
                  it.cache!!,
              )
            }
            w.eventListener?.let {
              registerWorkflowEventListener(
                  w.name,
                  it.concurrency!!,
                  it.instance,
                  it.subscriptionName,
              )
            }
            if (w.allClasses.isNotEmpty()) {
              registerWorkflowExecutor(
                  w.name,
                  w.allClasses,
                  w.concurrency!!,
                  w.timeoutInSeconds?.let { { it } },
                  w.retry,
                  w.checkMode,
              )
            }
          }

          for (s in workerConfig.services) {
            logger.info { "Service ${s.name}:" }

            s.tagEngine?.let {
              registerServiceTagEngine(
                  s.name,
                  it.concurrency!!,
                  it.storage!!,
                  it.cache!!,
              )
            }
            s.eventListener?.let {
              registerServiceEventListener(
                  s.name,
                  it.concurrency!!,
                  it.instance,
                  it.subscriptionName,
              )
            }
            s.`class`?.let {
              registerServiceExecutor(
                  s.name,
                  { s.getInstance() },
                  s.concurrency!!,
                  s.timeoutInSeconds?.let { { it } },
                  s.retry,
              )
            }
          }
        }
  }
}
