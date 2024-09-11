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
import io.infinitic.config.loadFromYamlFile
import io.infinitic.config.loadFromYamlResource
import io.infinitic.config.loadFromYamlString
import io.infinitic.storage.config.StorageConfig
import io.infinitic.workflows.tag.storage.BinaryWorkflowTagStorage

data class WorkflowTagEngineConfig(
  override var workflowName: String = "",
  var concurrency: Int = 1,
  override var storage: StorageConfig? = null,
) : WithMutableWorkflowName, WithMutableStorage {

  init {
    require(concurrency >= 0) { "concurrency must be positive" }
  }

  val workflowTagStorage by lazy {
    (storage ?: thisShouldNotHappen()).let {
      BinaryWorkflowTagStorage(it.keyValue, it.keySet)
    }
  }

  companion object {
    @JvmStatic
    fun builder() = WorkflowTagEngineConfigBuilder()

    /**
     * Create WorkflowTagEngineConfig from files in file system
     */
    @JvmStatic
    fun fromYamlFile(vararg files: String): WorkflowTagEngineConfig =
        loadFromYamlFile(*files)

    /**
     * Create WorkflowTagEngineConfig from files in resources directory
     */
    @JvmStatic
    fun fromYamlResource(vararg resources: String): WorkflowTagEngineConfig =
        loadFromYamlResource(*resources)

    /**
     * Create WorkflowTagEngineConfig from yaml strings
     */
    @JvmStatic
    fun fromYamlString(vararg yamls: String): WorkflowTagEngineConfig =
        loadFromYamlString(*yamls)
  }

  /**
   * WorkflowTagEngineConfig builder (Useful for Java user)
   */
  class WorkflowTagEngineConfigBuilder {
    private val default = WorkflowTagEngineConfig()
    private var workflowName = default.workflowName
    private var concurrency = default.concurrency
    private var storage = default.storage

    fun setWorkflowName(workflowName: String) =
        apply { this.workflowName = workflowName }

    fun setConcurrency(concurrency: Int) =
        apply { this.concurrency = concurrency }

    fun setStorage(storage: StorageConfig) =
        apply { this.storage = storage }

    fun build(): WorkflowTagEngineConfig {
      workflowName.checkWorkflowName()
      concurrency.checkConcurrency()

      return WorkflowTagEngineConfig(
          workflowName,
          concurrency,
          storage,
      )
    }
  }
}
