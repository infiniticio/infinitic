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

import io.infinitic.common.transport.config.BatchConfig
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

@Suppress("unused")
sealed class WorkflowExecutorConfig {

  abstract val workflowName: String

  /**
   * The factories are functions that return a new instance of the workflow.
   * This allows creating a new instance each time the workflow is executed.
   */
  abstract val factories: WorkflowFactories

  /**
   * The number of concurrent workflow executions.
   * If not provided, it will default to 1.
   */
  abstract val concurrency: Int

  /**
   * The timeout in seconds for the workflow execution.
   * If not provided, it will default to UNSET_RETRY.
   */
  abstract val withRetry: WithRetry?

  /**
   * The timeout in seconds for the workflow execution.
   * If not provided, it will default to UNSET_TIMEOUT.
   */
  abstract val withTimeout: WithTimeout?

  /**
   * The check mode for the workflow executors.
   * If not provided, it will default to null.
   */
  abstract val checkMode: WorkflowCheckMode?

  /**
   * The batch configuration for the workflow execution.
   * If not provided, it will not use batching.
   */
  abstract val batch: BatchConfig?

  /**
   * The number of concurrent workflow executor event handlers.
   * If not provided, it will default to the same value as concurrency.
   */
  abstract val eventHandlerConcurrency: Int

  /**
   * The number of concurrent workflow executor retry handlers.
   * If not provided, it will default to the same value as concurrency.
   */
  abstract val retryHandlerConcurrency: Int

  companion object {
    @JvmStatic
    fun builder() = WorkflowExecutorConfigBuilder()

    /**
     * Create WorkflowExecutorConfig from files in the file system
     */
    @JvmStatic
    fun fromYamlFile(vararg files: String): WorkflowExecutorConfig =
        loadFromYamlFile<LoadedWorkflowExecutorConfig>(*files)

    /**
     * Create WorkflowExecutorConfig from files in the resources directory
     */
    @JvmStatic
    fun fromYamlResource(vararg resources: String): WorkflowExecutorConfig =
        loadFromYamlResource<LoadedWorkflowExecutorConfig>(*resources)

    /**
     * Create WorkflowExecutorConfig from YAML strings
     */
    @JvmStatic
    fun fromYamlString(vararg yamls: String): WorkflowExecutorConfig =
        loadFromYamlString<LoadedWorkflowExecutorConfig>(*yamls)
  }

  internal fun check() {
    concurrency.checkConcurrency(::concurrency.name)
    eventHandlerConcurrency.checkConcurrency(::eventHandlerConcurrency.name)
    retryHandlerConcurrency.checkConcurrency(::retryHandlerConcurrency.name)
    if (factories.isNotEmpty()) {
      require(concurrency > 0) { "${::concurrency.name} must be greater than 0 when ${::factories.name} is defined" }
    }
    if (concurrency > 0) {
      require(factories.isNotEmpty()) { "At least one factory must be defined when ${::concurrency.name} is greater than 0" }
      factories.checkVersionUniqueness()
      factories.checkInstanceUniqueness()
      withTimeout?.getTimeoutSeconds()?.checkTimeout()
    }
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
    private var batch: BatchConfig? = null
    private var eventHandlerConcurrency: Int = UNSET_CONCURRENCY
    private var retryHandlerConcurrency: Int = UNSET_CONCURRENCY

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

    fun setBatch(maxMessages: Int, maxSeconds: Double) =
        apply { this.batch = BatchConfig(maxMessages, maxSeconds) }

    fun setEventHandlerConcurrency(eventHandlerConcurrency: Int) =
        apply { this.eventHandlerConcurrency = eventHandlerConcurrency }

    fun setRetryHandlerConcurrency(retryHandlerConcurrency: Int) =
        apply { this.retryHandlerConcurrency = retryHandlerConcurrency }

    fun build(): WorkflowExecutorConfig {
      workflowName.checkWorkflowName()

      // Set workflow context if needed
      if (concurrency > 0) {
        Workflow.setContext(emptyWorkflowContext)
      }

      return BuiltWorkflowExecutorConfig(
          workflowName = workflowName!!,
          factories = factories,
          concurrency = concurrency,
          withTimeout = timeoutSeconds.withTimeout,
          withRetry = withRetry,
          checkMode = checkMode,
          batch = batch,
          eventHandlerConcurrency = eventHandlerConcurrency
              .takeIf { it != UNSET_CONCURRENCY } ?: concurrency,
          retryHandlerConcurrency = retryHandlerConcurrency
              .takeIf { it != UNSET_CONCURRENCY } ?: concurrency,
      ).also { it.check() }
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
  override var withTimeout: WithTimeout?,
  override var withRetry: WithRetry?,
  override var checkMode: WorkflowCheckMode?,
  override val batch: BatchConfig?,
  override val eventHandlerConcurrency: Int = concurrency,
  override val retryHandlerConcurrency: Int = concurrency,
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
  override val batch: BatchConfig? = null,
  override val eventHandlerConcurrency: Int = concurrency,
  override val retryHandlerConcurrency: Int = concurrency,
) : WorkflowExecutorConfig(), WithMutableWorkflowName {
  private val allInstances = mutableListOf<Workflow>()

  override val withTimeout = timeoutSeconds.withTimeout

  override val withRetry: WithRetry? = retry.withRetry

  override val factories: WorkflowFactories by lazy {
    allInstances.map { { it::class.java.getInstance().getOrThrow() } }
  }

  init {

    if (concurrency > 0) {
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

      retry?.check()
    }
    this.check()
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
