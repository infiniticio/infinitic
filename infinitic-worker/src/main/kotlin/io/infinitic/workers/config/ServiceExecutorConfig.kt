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
import io.infinitic.common.utils.initBatchProcessorMethods
import io.infinitic.common.workers.config.RetryPolicy
import io.infinitic.common.workers.config.UNSET_RETRY_POLICY
import io.infinitic.config.loadFromYamlFile
import io.infinitic.config.loadFromYamlResource
import io.infinitic.config.loadFromYamlString
import io.infinitic.tasks.WithRetry
import io.infinitic.tasks.WithTimeout

private typealias ServiceFactory = () -> Any

internal const val UNSET_TIMEOUT = Double.MAX_VALUE

suspend fun ServiceExecutorConfig.initBatchProcessorMethods() {
  factory()::class.java.initBatchProcessorMethods()
}

@Suppress("unused")
sealed class ServiceExecutorConfig {

  abstract val serviceName: String

  /**
   * The factory is a function that returns a new instance of the service.
   * This allows creating a new instance each time the service is executed.
   */
  abstract val factory: ServiceFactory

  /**
   * The number of concurrent service executions.
   * If not provided, it will default to 1.
   */
  abstract val concurrency: Int

  /**
   * Configuration settings for batch message
   */
  abstract val batch: BatchConfig?

  /**
   * WithRetry instance for this executor
   * Set to WithRetry.UNSET if not defined
   * (it can still be defined by annotation or interface within the TaskExecutor)
   */
  abstract val withRetry: WithRetry?

  /**
   * WithTimout instance for this executor
   * Set to WithTimeout.UNSET if not defined
   * (it can still be defined by annotation or interface within the TaskExecutor)
   */
  abstract val withTimeout: WithTimeout?

  /**
   * The number of concurrent Service executor event handlers.
   * If not provided, it will default to the same value as concurrency.
   */
  abstract val eventHandlerConcurrency: Int

  /**
   * The number of concurrent Service executor retry handlers.
   * If not provided, it will default to the same value as concurrency.
   */
  abstract val retryHandlerConcurrency: Int

  companion object {
    @JvmStatic
    fun builder() = ServiceExecutorConfigBuilder()

    /**
     * Create ServiceExecutorConfig from files in the file system
     */
    @JvmStatic
    fun fromYamlFile(vararg files: String): ServiceExecutorConfig =
        loadFromYamlFile<LoadedServiceExecutorConfig>(*files)

    /**
     * Create ServiceExecutorConfig from files in the resources directory
     */
    @JvmStatic
    fun fromYamlResource(vararg resources: String): ServiceExecutorConfig =
        loadFromYamlResource<LoadedServiceExecutorConfig>(*resources)

    /**
     * Create ServiceExecutorConfig from yaml strings
     */
    @JvmStatic
    fun fromYamlString(vararg yamls: String): ServiceExecutorConfig =
        loadFromYamlString<LoadedServiceExecutorConfig>(*yamls)
  }

  /**
   * ServiceConfig builder
   */
  class ServiceExecutorConfigBuilder {
    private var serviceName: String? = null
    private var factory: ServiceFactory? = null
    private var concurrency: Int = 1
    private var timeoutSeconds: Double? = UNSET_TIMEOUT
    private var withRetry: WithRetry? = WithRetry.UNSET
    private var batchConfig: BatchConfig? = null
    private var eventHandlerConcurrency: Int = concurrency
    private var retryHandlerConcurrency: Int = concurrency

    fun setServiceName(name: String) =
        apply { this.serviceName = name }

    fun setFactory(factory: () -> Any) =
        apply { this.factory = factory }

    fun setConcurrency(concurrency: Int) =
        apply { this.concurrency = concurrency }

    fun setTimeoutSeconds(timeoutSeconds: Double) =
        apply { this.timeoutSeconds = timeoutSeconds }

    fun withRetry(retry: WithRetry) =
        apply { this.withRetry = retry }

    fun setBatch(maxMessages: Int, maxSeconds: Double) {
      apply { this.batchConfig = BatchConfig(maxMessages, maxSeconds) }
    }

    fun setEventHandlerConcurrency(eventHandlerConcurrency: Int) =
        apply { this.eventHandlerConcurrency = eventHandlerConcurrency }

    fun setRetryHandlerConcurrency(retryHandlerConcurrency: Int) =
        apply { this.retryHandlerConcurrency = retryHandlerConcurrency }

    fun build(): ServiceExecutorConfig {
      serviceName.checkServiceName()
      require(factory != null) { "${::factory.name} must not be null" }
      concurrency.checkConcurrency(::concurrency.name)
      timeoutSeconds?.checkTimeout()

      return BuiltServiceExecutorConfig(
          serviceName = serviceName!!,
          factory = factory!!,
          concurrency = concurrency,
          withRetry = withRetry,
          withTimeout = timeoutSeconds.withTimeout,
          batch = batchConfig,
          eventHandlerConcurrency = eventHandlerConcurrency,
          retryHandlerConcurrency = retryHandlerConcurrency,
      )
    }
  }
}

/**
 * ServiceExecutorConfig built by builder
 */
data class BuiltServiceExecutorConfig(
  override val serviceName: String,
  override val factory: ServiceFactory,
  override val concurrency: Int,
  override val withRetry: WithRetry?,
  override val withTimeout: WithTimeout?,
  override val batch: BatchConfig?,
  override val eventHandlerConcurrency: Int = concurrency,
  override val retryHandlerConcurrency: Int = concurrency
) : ServiceExecutorConfig()

/**
 * ServiceExecutorConfig loaded from YAML
 */
data class LoadedServiceExecutorConfig(
  override var serviceName: String = "",
  val `class`: String,
  override val concurrency: Int = 1,
  val timeoutSeconds: Double? = UNSET_TIMEOUT,
  val retry: RetryPolicy? = UNSET_RETRY_POLICY,
  override val batch: BatchConfig? = null,
  override val eventHandlerConcurrency: Int = concurrency,
  override val retryHandlerConcurrency: Int = concurrency
) : ServiceExecutorConfig(), WithMutableServiceName {

  override val factory: () -> Any = { `class`.getInstance().getOrThrow() }

  override val withTimeout = timeoutSeconds.withTimeout

  override val withRetry: WithRetry? = retry.withRetry

  init {
    `class`.checkClass()
    concurrency.checkConcurrency(::concurrency.name)
    timeoutSeconds?.checkTimeout()
    retry?.check()
  }

  @JvmName("replaceServiceName")
  internal fun setServiceName(name: String) {
    if (serviceName == name) return
    if (serviceName.isNotBlank()) {
      throw IllegalStateException("serviceName is already set to '$serviceName'")
    }
    serviceName = name
  }
}

internal val Double?.withTimeout
  get() = when (this) {
    UNSET_TIMEOUT -> WithTimeout.UNSET
    else -> this?.let { WithTimeout { it } }
  }

internal val WithRetry?.withRetry
  get() = when (this) {
    is UNSET_RETRY_POLICY -> WithRetry.UNSET
    else -> this
  }

internal fun String.checkClass() {
  require(isNotEmpty()) { "class can not be empty" }

  getInstance().getOrThrow()
}

internal fun String?.checkServiceName() {
  require(this != null) { "serviceName must not be null" }
  require(this.isNotBlank()) { "serviceName must not be blank" }
}

internal fun Int?.checkConcurrency(name: String) {
  require(this != null) { "$name must not be null" }
  require(this >= 0) { "$name must be >= 0, but was $this" }
}

internal fun Double.checkTimeout() {
  require(this > 0) { "timeoutSeconds must be > 0, but was $this" }
}
