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
package io.infinitic.workers.config

import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.transport.config.BatchConfig
import io.infinitic.config.loadFromYamlFile
import io.infinitic.config.loadFromYamlResource
import io.infinitic.config.loadFromYamlString
import io.infinitic.storage.config.StorageConfig
import io.infinitic.workflows.engine.storage.BinaryWorkflowStateStorage

@Suppress("unused")
data class WorkflowStateEngineConfig(

  override var workflowName: String = "",
  /**
   * The number of concurrent workflow state engine instances.
   * If not provided, it will default to 1.
   */
  val concurrency: Int = 1,
  /**
   * Storage configuration for the workflow state engine.
   * If not provided, it will use the default storage configuration.
   */
  override var storage: StorageConfig? = null,
  /**
   * Batch configuration for the workflow state engine.
   * If not provided, it will not use batching.
   */
  val batch: BatchConfig? = null,
  /**
   * The number of concurrent workflow state timer handlers.
   * If not provided, it will default to the same value as concurrency.
   */
  val timerHandlerConcurrency: Int = concurrency,
  /**
   * The number of concurrent workflow state command handlers.
   * If not provided, it will default to the same value as concurrency.
   */
  val commandHandlerConcurrency: Int = concurrency,
  /**
   * The number of concurrent workflow state event handlers.
   * If not provided, it will default to the same value as concurrency.
   */
  val eventHandlerConcurrency: Int = concurrency,

  /**
   * The maximum number of seconds a message received by timer handler can be past due before being considered expired.
   * Default is 3 days (60 * 60 * 24 * 3 seconds).
   */
  val timerHandlerPastDueSeconds: Long = 60 * 60 * 24 * 3,

  ) : WithMutableWorkflowName, WithMutableStorage {

  init {
    require(concurrency >= 0) { "${::concurrency.name} must be positive" }

    require(timerHandlerConcurrency >= 0) { "${::timerHandlerConcurrency.name} must be positive" }

    require(commandHandlerConcurrency >= 0) { "${::commandHandlerConcurrency.name} must be positive" }

    require(eventHandlerConcurrency >= 0) { "${::eventHandlerConcurrency.name} must be positive" }

    require(timerHandlerPastDueSeconds >= 60 * 60) { "${::timerHandlerPastDueSeconds.name} must be at least 1 hour" }
  }

  val workflowStateStorage by lazy {
    BinaryWorkflowStateStorage((storage ?: thisShouldNotHappen()).keyValue)
  }

  companion object {
    @JvmStatic
    fun builder() = WorkflowStateEngineConfigBuilder()

    /**
     * Create WorkflowStateEngineConfig from files in the file system
     */
    @JvmStatic
    fun fromYamlFile(vararg files: String): WorkflowStateEngineConfig =
        loadFromYamlFile(*files)

    /**
     * Create WorkflowStateEngineConfig from files in the resources directory
     */
    @JvmStatic
    fun fromYamlResource(vararg resources: String): WorkflowStateEngineConfig =
        loadFromYamlResource(*resources)

    /**
     * Create WorkflowStateEngineConfig from YAML strings
     */
    @JvmStatic
    fun fromYamlString(vararg yamls: String): WorkflowStateEngineConfig =
        loadFromYamlString(*yamls)
  }

  /**
   * WorkflowStateEngineConfig builder (Useful for Java user)
   */
  class WorkflowStateEngineConfigBuilder {
    private val default = WorkflowStateEngineConfig()
    private var workflowName = default.workflowName
    private var concurrency = default.concurrency
    private var storage = default.storage
    private var batch = default.batch
    private var timerHandlerConcurrency = UNSET_CONCURRENCY
    private var commandHandlerConcurrency = UNSET_CONCURRENCY
    private var eventHandlerConcurrency = UNSET_CONCURRENCY
    private var timerHandlerPastDueSeconds = default.timerHandlerPastDueSeconds

    fun setWorkflowName(workflowName: String) =
        apply { this.workflowName = workflowName }

    fun setConcurrency(concurrency: Int) =
        apply { this.concurrency = concurrency }

    fun setStorage(storage: StorageConfig) =
        apply { this.storage = storage }

    fun setBatch(maxMessages: Int, maxSeconds: Double) =
        apply { this.batch = BatchConfig(maxMessages, maxSeconds) }

    fun setTimerHandlerConcurrency(timerHandlerConcurrency: Int) =
        apply { this.timerHandlerConcurrency = timerHandlerConcurrency }

    fun setCommandHandlerConcurrency(commandHandlerConcurrency: Int) =
        apply { this.commandHandlerConcurrency = commandHandlerConcurrency }

    fun setEventHandlerConcurrency(eventHandlerConcurrency: Int) =
        apply { this.eventHandlerConcurrency = eventHandlerConcurrency }

    fun setTimerHandlerPastDueSeconds(timerHandlerPastDueSeconds: Long) =
        apply { this.timerHandlerPastDueSeconds = timerHandlerPastDueSeconds }

    fun build(): WorkflowStateEngineConfig {
      workflowName.checkWorkflowName()

      return WorkflowStateEngineConfig(
          workflowName = workflowName,
          concurrency = concurrency,
          storage = storage,
          batch = batch,
          timerHandlerConcurrency = timerHandlerConcurrency
              .takeIf { it != UNSET_CONCURRENCY } ?: concurrency,
          commandHandlerConcurrency = commandHandlerConcurrency
              .takeIf { it != UNSET_CONCURRENCY } ?: concurrency,
          eventHandlerConcurrency = eventHandlerConcurrency
              .takeIf { it != UNSET_CONCURRENCY } ?: concurrency,
          timerHandlerPastDueSeconds = timerHandlerPastDueSeconds,
      )
    }
  }
}
