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

import io.infinitic.cloudEvents.CloudEventListener
import io.infinitic.cloudEvents.SelectionConfig
import io.infinitic.common.utils.annotatedName
import io.infinitic.common.utils.getInstance
import io.infinitic.config.loadFromYamlFile
import io.infinitic.config.loadFromYamlResource
import io.infinitic.config.loadFromYamlString

sealed class EventListenerConfig {
  abstract val listener: CloudEventListener
  abstract val concurrency: Int
  abstract val subscriptionName: String?
  abstract val allowedServices: List<String>?
  abstract val disallowedServices: List<String>
  abstract val allowedWorkflows: List<String>?
  abstract val disallowedWorkflows: List<String>

  fun isServiceAllowed(service: String): Boolean {
    return !disallowedServices.contains(service) && (allowedServices?.contains(service) != false)
  }

  fun isWorkflowAllowed(workflow: String): Boolean {
    return !disallowedWorkflows.contains(workflow) && (allowedWorkflows?.contains(workflow) != false)
  }

  companion object {
    @JvmStatic
    fun builder() = EventListenerConfigBuilder()

    /**
     * Create EventListenerConfig from files in file system
     */
    @JvmStatic
    fun fromYamlFile(vararg files: String): EventListenerConfig =
        loadFromYamlFile<LoadedEventListenerConfig>(*files)

    /**
     * Create EventListenerConfig from files in resources directory
     */
    @JvmStatic
    fun fromYamlResource(vararg resources: String): EventListenerConfig =
        loadFromYamlResource<LoadedEventListenerConfig>(*resources)

    /**
     * Create EventListenerConfig from yaml strings
     */
    @JvmStatic
    fun fromYamlString(vararg yamls: String): EventListenerConfig =
        loadFromYamlString<LoadedEventListenerConfig>(*yamls)
  }

  /**
   * EventListenerConfig builder
   */
  class EventListenerConfigBuilder {
    private var listener: CloudEventListener? = null
    private var concurrency: Int = 1
    private var subscriptionName: String? = null
    private var allowedServices: MutableList<String>? = null
    private val disallowedServices: MutableList<String> = mutableListOf()
    private var allowedWorkflows: MutableList<String>? = null
    private val disallowedWorkflows: MutableList<String> = mutableListOf()

    fun setListener(cloudEventListener: CloudEventListener) =
        apply { this.listener = cloudEventListener }

    fun setConcurrency(concurrency: Int) =
        apply { this.concurrency = concurrency }

    fun setSubscriptionName(subscriptionName: String) =
        apply { this.subscriptionName = subscriptionName }

    fun disallowServices(vararg services: String) =
        apply { services.forEach { disallowedServices.add(it) } }

    fun allowServices(vararg services: String) =
        apply {
          services.forEach { service ->
            allowedServices = (allowedServices ?: mutableListOf()).apply { add(service) }
          }
        }

    fun disallowWorkflows(vararg workflows: String) =
        apply { workflows.forEach { disallowedWorkflows.add(it) } }

    fun allowWorkflows(vararg workflows: String) =
        apply {
          workflows.forEach { workflow ->
            allowedWorkflows = (allowedWorkflows ?: mutableListOf()).apply { add(workflow) }
          }
        }

    fun disallowServices(vararg services: Class<*>) =
        apply { disallowServices(*(services.map { it.annotatedName }.toTypedArray())) }

    fun allowServices(vararg services: Class<*>) =
        apply { allowServices(*(services.map { it.annotatedName }.toTypedArray())) }

    fun disallowWorkflows(vararg workflows: Class<*>) =
        apply { disallowWorkflows(*(workflows.map { it.annotatedName }.toTypedArray())) }

    fun allowWorkflows(vararg workflows: Class<*>) =
        apply { allowWorkflows(*(workflows.map { it.annotatedName }.toTypedArray())) }

    fun build(): EventListenerConfig {
      require(listener != null) { "cloudEventListener must not be null" }

      return BuiltEventListenerConfig(
          listener!!,
          concurrency,
          subscriptionName,
          allowedServices,
          disallowedServices,
          allowedWorkflows,
          disallowedWorkflows,
      )
    }
  }
}

/**
 * EventListenerConfig built from builder
 */
data class BuiltEventListenerConfig(
  override val listener: CloudEventListener,
  override val concurrency: Int,
  override val subscriptionName: String?,
  override val allowedServices: MutableList<String>?,
  override val disallowedServices: MutableList<String>,
  override val allowedWorkflows: MutableList<String>?,
  override val disallowedWorkflows: MutableList<String>
) : EventListenerConfig()

/**
 * EventListenerConfig loaded from YAML
 */
data class LoadedEventListenerConfig(
  val `class`: String,
  override val concurrency: Int = 1,
  override val subscriptionName: String? = null,
  val services: SelectionConfig = SelectionConfig(),
  val workflows: SelectionConfig = SelectionConfig()
) : EventListenerConfig() {

  override val listener: CloudEventListener
  override var allowedServices = services.allow
  override val disallowedServices = services.disallow
  override var allowedWorkflows = workflows.allow
  override val disallowedWorkflows = workflows.disallow

  init {
    with(`class`) {
      require(isNotEmpty()) { error("'class' must not be empty") }
      val obj = getInstance().getOrThrow()
      require(obj is CloudEventListener) {
        error("Class '$this' must implement '${CloudEventListener::class.java.name}'")
      }
      listener = obj
    }

    require(concurrency > 0) { error("'${::concurrency.name}' must be > 0, but was $concurrency") }

    subscriptionName?.let {
      require(it.isNotEmpty()) { error("'when provided, ${::subscriptionName.name}' must not be empty") }
    }
  }

  private fun error(txt: String) = "eventListener: $txt"
}
