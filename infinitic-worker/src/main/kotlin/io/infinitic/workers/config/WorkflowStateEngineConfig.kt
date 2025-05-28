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
) : WithMutableWorkflowName, WithMutableStorage {

  init {
    require(concurrency >= 0) { "${::concurrency.name} must be positive" }

    require(timerHandlerConcurrency >= 0) { "${::timerHandlerConcurrency.name} must be positive" }

    require(commandHandlerConcurrency >= 0) { "${::commandHandlerConcurrency.name} must be positive" }

    require(eventHandlerConcurrency >= 0) { "${::eventHandlerConcurrency.name} must be positive" }
  }

  val workflowStateStorage by lazy {
    BinaryWorkflowStateStorage((storage ?: thisShouldNotHappen()).keyValue)
  }

  companion object {
    @JvmStatic
    fun builder() = WorkflowStateEngineConfigBuilder()

    /**
     * Create WorkflowStateEngineConfig from files in file system
     */
    @JvmStatic
    fun fromYamlFile(vararg files: String): WorkflowStateEngineConfig =
        loadFromYamlFile(*files)

    /**
     * Create WorkflowStateEngineConfig from files in resources directory
     */
    @JvmStatic
    fun fromYamlResource(vararg resources: String): WorkflowStateEngineConfig =
        loadFromYamlResource(*resources)

    /**
     * Create WorkflowStateEngineConfig from yaml strings
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
    private var timerHandlerConcurrency = default.timerHandlerConcurrency
    private var commandHandlerConcurrency = default.commandHandlerConcurrency
    private var eventHandlerConcurrency = default.eventHandlerConcurrency

    fun setWorkflowName(workflowName: String) =
        apply { this.workflowName = workflowName }

    fun setConcurrency(concurrency: Int) =
        apply { this.concurrency = concurrency }

    fun setStorage(storage: StorageConfig) =
        apply { this.storage = storage }

    fun setBatch(maxMessages: Int, maxSeconds: Double) =
        apply { this.batch = BatchConfig(maxMessages, maxSeconds) }

    fun setTimerHandlerConcurrency(concurrency: Int) =
        apply { this.timerHandlerConcurrency = concurrency }

    fun setCommandHandlerConcurrency(concurrency: Int) =
        apply { this.commandHandlerConcurrency = concurrency }

    fun setEventHandlerConcurrency(concurrency: Int) =
        apply { this.eventHandlerConcurrency = concurrency }

    fun build(): WorkflowStateEngineConfig {
      workflowName.checkWorkflowName()
      concurrency.checkConcurrency()

      return WorkflowStateEngineConfig(
          workflowName = workflowName,
          concurrency = concurrency,
          storage = storage,
          batch = batch,
          timerHandlerConcurrency = timerHandlerConcurrency,
          commandHandlerConcurrency = commandHandlerConcurrency,
          eventHandlerConcurrency = eventHandlerConcurrency,
      )
    }
  }
}
