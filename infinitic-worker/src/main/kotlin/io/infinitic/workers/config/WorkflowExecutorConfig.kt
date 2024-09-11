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

import io.infinitic.common.utils.getInstance
import io.infinitic.common.workers.config.RetryPolicy
import io.infinitic.common.workers.config.UNSET_RETRY_POLICY
import io.infinitic.common.workers.config.WorkflowVersion
import io.infinitic.common.workflows.emptyWorkflowContext
import io.infinitic.common.workflows.executors.getProperties
import io.infinitic.config.loadFromYamlFile
import io.infinitic.config.loadFromYamlResource
import io.infinitic.config.loadFromYamlString
import io.infinitic.tasks.WithRetry
import io.infinitic.tasks.WithTimeout
import io.infinitic.workflows.Workflow
import io.infinitic.workflows.WorkflowCheckMode

internal typealias WorkflowFactory = () -> Workflow
internal typealias WorkflowFactories = List<WorkflowFactory>

sealed class WorkflowExecutorConfig {

  abstract val workflowName: String
  abstract val factories: WorkflowFactories
  abstract val concurrency: Int
  abstract val withRetry: WithRetry?
  abstract val withTimeout: WithTimeout?
  abstract val checkMode: WorkflowCheckMode?

  companion object {
    @JvmStatic
    fun builder() = WorkflowExecutorConfigBuilder()

    /**
     * Create WorkflowExecutorConfig from files in file system
     */
    @JvmStatic
    fun fromYamlFile(vararg files: String): WorkflowExecutorConfig =
        loadFromYamlFile<LoadedWorkflowExecutorConfig>(*files)

    /**
     * Create WorkflowExecutorConfig from files in resources directory
     */
    @JvmStatic
    fun fromYamlResource(vararg resources: String): WorkflowExecutorConfig =
        loadFromYamlResource<LoadedWorkflowExecutorConfig>(*resources)

    /**
     * Create WorkflowExecutorConfig from yaml strings
     */
    @JvmStatic
    fun fromYamlString(vararg yamls: String): WorkflowExecutorConfig =
        loadFromYamlString<LoadedWorkflowExecutorConfig>(*yamls)
  }

  /**
   * ServiceConfig builder
   */
  class WorkflowExecutorConfigBuilder {
    private var workflowName: String? = null
    private var factories: MutableList<() -> Workflow> = mutableListOf()
    private var concurrency: Int = 1
    private var timeoutSeconds: Double? = UNSET_TIMEOUT
    private var withRetry: WithRetry? = WithRetry.UNSET
    private var checkMode: WorkflowCheckMode? = null

    fun setWorkflowName(workflowName: String) =
        apply { this.workflowName = workflowName }

    fun addFactory(factory: () -> Workflow) =
        apply { this.factories.add(factory) }

    fun setConcurrency(concurrency: Int) =
        apply { this.concurrency = concurrency }

    fun setTimeoutSeconds(timeoutSeconds: Double) =
        apply { this.timeoutSeconds = timeoutSeconds }

    fun withRetry(retry: WithRetry) =
        apply { this.withRetry = retry }

    fun setCheckMode(checkMode: WorkflowCheckMode) =
        apply { this.checkMode = checkMode }

    fun build(): WorkflowExecutorConfig {
      workflowName.checkWorkflowName()

      // Needed if the workflow context is referenced within the properties of the workflow
      Workflow.setContext(emptyWorkflowContext)

      require(factories.isNotEmpty()) { "At least one factory must be defined" }
      factories.checkVersionUniqueness()
      factories.checkInstanceUniqueness()

      concurrency.checkConcurrency()
      timeoutSeconds?.checkTimeout()

      return BuiltWorkflowExecutorConfig(
          workflowName!!,
          factories,
          concurrency,
          timeoutSeconds.withTimeout,
          withRetry,
          checkMode,
      )
    }
  }
}

/**
 * WorkflowExecutorConfig built from builders
 */
data class BuiltWorkflowExecutorConfig(
  override val workflowName: String,
  override val factories: WorkflowFactories,
  override val concurrency: Int,
  override var withTimeout: WithTimeout? = null,
  override var withRetry: WithRetry? = null,
  override var checkMode: WorkflowCheckMode? = null,
) : WorkflowExecutorConfig()

/**
 * WorkflowExecutorConfig loaded from YAML
 */
data class LoadedWorkflowExecutorConfig(
  override var workflowName: String = "",
  val `class`: String? = null,
  val classes: List<String>? = null,
  override val concurrency: Int = 1,
  val timeoutSeconds: Double? = UNSET_TIMEOUT,
  var retry: RetryPolicy? = UNSET_RETRY_POLICY,
  override var checkMode: WorkflowCheckMode? = null,
) : WorkflowExecutorConfig(), WithMutableWorkflowName {
  private val allInstances = mutableListOf<Workflow>()

  override val withTimeout = timeoutSeconds.withTimeout

  override val withRetry: WithRetry? = retry.withRetry

  override val factories: WorkflowFactories by lazy {
    allInstances.map { { it::class.java.getInstance().getOrThrow() } }
  }

  init {
    // Needed if the workflow context is referenced within the properties of the workflow
    Workflow.setContext(emptyWorkflowContext)

    require((`class` != null) || (classes != null)) {
      "'${::`class`.name}' and '${::classes.name}' can not be both null"
    }

    `class`?.let {
      require(`class`.isNotEmpty()) { "'${::`class`.name}' can not be empty" }
      allInstances.add(getInstance(it))
    }

    classes?.forEachIndexed { index, s: String ->
      require(s.isNotEmpty()) { "'${::classes.name}[$index]' can not be empty" }
      allInstances.add(getInstance(s))
    }
    factories.checkVersionUniqueness()
    factories.checkInstanceUniqueness()

    concurrency.checkConcurrency()
    timeoutSeconds?.checkTimeout()
    retry?.check()
  }

  private fun getInstance(className: String): Workflow {
    val instance = className.getInstance().getOrThrow()

    require(instance is Workflow) {
      "Class '$className' must extend '${Workflow::class.java.name}'"
    }

    return instance
  }
}

internal fun String?.checkWorkflowName() {
  require(this != null) { "workflowName must not be null" }
  require(this.isNotBlank()) { "workflowName must not be blank" }
}

internal fun WorkflowFactories.checkInstanceUniqueness() {
  // check that each factory returns a different instance, if this instance has properties
  forEachIndexed { index, function ->
    val instance1 = function.invoke()
    val instance2 = function.invoke()

    require(
        (instance1 !== instance2) ||
            (instance1.getProperties().isEmpty() && instance2.getProperties().isEmpty()),
    ) {
      "The workflow factory $index returned the same object instance twice. " +
          "Because your workflow contains properties, this will create threading issues. " +
          "Please ensure a new workflow instance is returned each time."
    }
  }
}

@JvmName("checkFactories")
internal fun WorkflowFactories.checkVersionUniqueness() {
  // check that each factory is associated with a different version
  val instances = mapIndexed { index, factory ->
    try {
      factory()
    } catch (e: Exception) {
      throw IllegalArgumentException("Error when running factory #$index", e)
    }
  }
  instances.checkVersionUniqueness()
}

@JvmName("checkWorkflows")
internal fun List<Workflow>.checkVersionUniqueness() {
  // check that each class is associated with a different version
  val versions = map { WorkflowVersion.from(it::class.java) }

  val duplicatedVersions = getDuplicatedVersionsWithIndices(versions)

  duplicatedVersions.forEach { (version, indices) ->
    throw IllegalArgumentException("Version $version is duplicated at factory: $indices")
  }
}

private fun getDuplicatedVersionsWithIndices(versions: List<WorkflowVersion>): Map<WorkflowVersion, List<Int>> {
  val indexMap = mutableMapOf<WorkflowVersion, MutableList<Int>>()

  versions.forEachIndexed { index, version ->
    indexMap.computeIfAbsent(version) { mutableListOf() }.add(index)
  }

  return indexMap.filter { it.value.size > 1 }
}
