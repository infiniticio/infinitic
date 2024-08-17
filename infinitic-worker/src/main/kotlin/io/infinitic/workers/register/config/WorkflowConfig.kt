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
package io.infinitic.workers.register.config

import io.infinitic.common.utils.getInstance
import io.infinitic.common.utils.isImplementationOf
import io.infinitic.common.workers.config.RetryPolicy
import io.infinitic.common.workflows.emptyWorkflowContext
import io.infinitic.events.config.EventListenerConfig
import io.infinitic.events.config.EventLoggerConfig
import io.infinitic.workflows.Workflow
import io.infinitic.workflows.WorkflowCheckMode
import io.infinitic.workflows.engine.config.WorkflowStateEngineConfig
import io.infinitic.workflows.tag.config.WorkflowTagEngineConfig
import io.infinitic.workflows.Workflow as WorkflowBase

@Suppress("unused")
data class WorkflowConfig(
  val name: String,
  val `class`: String? = null,
  val classes: List<String>? = null,
  var concurrency: Int? = null,
  var timeoutInSeconds: Double? = UNDEFINED_TIMEOUT,
  var retry: RetryPolicy? = UNDEFINED_RETRY,
  var checkMode: WorkflowCheckMode? = null,
  var tagEngine: WorkflowTagEngineConfig? = DEFAULT_WORKFLOW_TAG_ENGINE,
  var stateEngine: WorkflowStateEngineConfig? = DEFAULT_WORKFLOW_STATE_ENGINE,
  var eventListener: EventListenerConfig? = UNDEFINED_EVENT_LISTENER,
  val eventLogger: EventLoggerConfig? = UNDEFINED_EVENT_LOGGER
) {
  val allClasses = mutableListOf<Class<out WorkflowBase>>()

  init {
    require(name.isNotEmpty()) { "name can not be empty" }

    when {
      (`class` == null) && (classes == null) -> require(tagEngine != null || stateEngine != null || eventListener != null) {
        error("'${::`class`.name}', '${::classes.name}', '${::tagEngine.name}', '${::stateEngine.name}' and '${::eventListener.name}' can not be all null")
      }

      else -> {
        `class`?.let {
          require(`class`.isNotEmpty()) { error("'${::`class`.name}' can not be empty") }
          allClasses.add(getWorkflowClass(it))
        }
        classes?.forEachIndexed { index, s: String ->
          require(s.isNotEmpty()) { error("'${::classes.name}[$index]' can not be empty") }
          allClasses.add(getWorkflowClass(s))
        }

        concurrency?.let {
          require(it >= 0) { error("'${::concurrency.name}' must be an integer >= 0") }
        }

        timeoutInSeconds?.let { timeout ->
          require(timeout > 0 || timeout == UNDEFINED_TIMEOUT) { error("'${::timeoutInSeconds.name}' must be an integer > 0") }
        }
      }
    }
  }

  companion object {
    @JvmStatic
    fun builder() = WorkflowConfigBuilder()
  }

  /**
   * WorkflowConfig builder (Useful for Java user)
   */
  class WorkflowConfigBuilder {
    private val default = WorkflowConfig(UNSET)
    private var name = default.name
    private var `class` = default.`class`
    private var classes = default.classes
    private var concurrency = default.concurrency
    private var timeoutInSeconds = default.timeoutInSeconds
    private var retry = default.retry
    private var checkMode = default.checkMode
    private var tagEngine = default.tagEngine
    private var stateEngine = default.stateEngine
    private var eventListener = default.eventListener
    private var eventLogger = default.eventLogger

    fun name(name: String) =
        apply { this.name = name }

    fun `class`(`class`: String) =
        apply { this.`class` = `class` }

    fun classes(classes: MutableList<String>) =
        apply { this.classes = classes }

    fun concurrency(concurrency: Int) =
        apply { this.concurrency = concurrency }

    fun timeoutInSeconds(timeoutInSeconds: Double) =
        apply { this.timeoutInSeconds = timeoutInSeconds }

    fun retry(retry: RetryPolicy) =
        apply { this.retry = retry }

    fun checkMode(checkMode: WorkflowCheckMode) =
        apply { this.checkMode = checkMode }

    fun tagEngine(tagEngine: WorkflowTagEngineConfig) =
        apply { this.tagEngine = tagEngine }

    fun stateEngine(stateEngine: WorkflowStateEngineConfig) =
        apply { this.stateEngine = stateEngine }

    fun eventListener(eventListener: EventListenerConfig) =
        apply { this.eventListener = eventListener }

    fun eventLogger(eventLogger: EventLoggerConfig) =
        apply { this.eventLogger = eventLogger }

    fun build() = WorkflowConfig(
        name.noUnset,
        `class`,
        classes,
        concurrency,
        timeoutInSeconds,
        retry,
        checkMode,
        tagEngine,
        stateEngine,
        eventListener,
        eventLogger,
    )
  }

  private fun getWorkflowClass(className: String): Class<out Workflow> {
    // make sure to have a context to be able to create the workflow instance
    Workflow.setContext(emptyWorkflowContext)

    val instance = className.getInstance().getOrThrow()
    val klass = instance::class.java

    require(klass.isImplementationOf(name)) {
      error("Class '${klass.name}' is not an implementation of '$name' - check your configuration")
    }

    require(instance is WorkflowBase) {
      error("Class '${klass.name}' must extend '${WorkflowBase::class.java.name}'")
    }

    @Suppress("UNCHECKED_CAST")
    return klass as Class<out WorkflowBase>
  }

  private fun error(txt: String) = "Workflow $name: $txt"
}

private const val UNSET = "INFINITIC_UNSET_STRING"
private val String.noUnset: String
  get() = when (this) {
    UNSET -> ""
    else -> this
  }
