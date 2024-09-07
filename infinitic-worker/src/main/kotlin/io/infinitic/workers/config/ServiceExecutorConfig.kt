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
import io.infinitic.config.loadFromYamlFile
import io.infinitic.config.loadFromYamlResource
import io.infinitic.config.loadFromYamlString
import io.infinitic.tasks.WithRetry
import io.infinitic.tasks.WithTimeout

private typealias ServiceFactory = () -> Any

internal const val UNSET_TIMEOUT = Double.MAX_VALUE

@Suppress("unused")
sealed class ServiceExecutorConfig {

  abstract val factory: ServiceFactory
  abstract val concurrency: Int

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

  companion object {
    @JvmStatic
    fun builder() = ServiceExecutorConfigBuilder()

    /**
     * Create ServiceExecutorConfig from files in file system
     */
    @JvmStatic
    fun fromYamlFile(vararg files: String): ServiceExecutorConfig =
        loadFromYamlFile<LoadedServiceExecutorConfig>(*files)

    /**
     * Create ServiceExecutorConfig from files in resources directory
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
    private var factory: ServiceFactory? = null
    private var concurrency: Int = 1
    private var timeoutSeconds: Double? = UNSET_TIMEOUT
    private var withRetry: WithRetry? = WithRetry.UNSET

    fun setFactory(factory: () -> Any) =
        apply { this.factory = factory }

    fun setConcurrency(concurrency: Int) =
        apply { this.concurrency = concurrency }

    fun setTimeoutSeconds(timeoutSeconds: Double) =
        apply { this.timeoutSeconds = timeoutSeconds }

    fun withRetry(retry: WithRetry) =
        apply { this.withRetry = retry }

    fun build(): ServiceExecutorConfig {
      require(factory != null) { "factory must not be null" }
      concurrency.checkConcurrency()
      timeoutSeconds?.checkTimeout()

      return BuiltServiceExecutorConfig(
          factory!!,
          concurrency,
          withRetry,
          timeoutSeconds.withTimeout,
      )
    }
  }
}

/**
 * ServiceExecutorConfig built from builder
 */
data class BuiltServiceExecutorConfig(
  override val factory: ServiceFactory,
  override val concurrency: Int,
  override val withRetry: WithRetry?,
  override val withTimeout: WithTimeout?,
) : ServiceExecutorConfig()

/**
 * ServiceExecutorConfig loaded from YAML
 */
data class LoadedServiceExecutorConfig(
  val `class`: String,
  override val concurrency: Int = 1,
  val timeoutSeconds: Double? = UNSET_TIMEOUT,
  val retry: RetryPolicy? = UNSET_RETRY_POLICY,
) : ServiceExecutorConfig() {

  override val factory: () -> Any = { `class`.getInstance().getOrThrow() }

  override val withTimeout = timeoutSeconds.withTimeout

  override val withRetry: WithRetry? = retry.withRetry

  init {
    `class`.checkClass()
    concurrency.checkConcurrency()
    timeoutSeconds?.checkTimeout()
    retry?.check()
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

internal fun Int?.checkConcurrency() {
  require(this != null) { "concurrency must not be null" }
  require(this > 0) { "concurrency must be > 0, but was $this" }
}

internal fun Double.checkTimeout() {
  require(this > 0) { "timeoutSeconds must be > 0, but was $this" }
}
