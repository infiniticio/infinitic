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
package io.infinitic.cloudEvents

@Suppress("unused")
data class SelectionConfig(
  val only: List<String>? = null,
  val exclude: List<String> = listOf()
) {
  var isDefined = true

  init {
    only?.forEach {
      require(it.isNotEmpty()) { error("'${::only.name}' must not contain empty element") }
    }
    exclude.forEach {
      require(it.isNotEmpty()) { error("'${::exclude.name}' must not contain empty element") }
    }
  }

  companion object {
    @JvmStatic
    fun builder() = SelectionConfigBuilder()
  }

  fun isIncluded(name: String) =
      (only == null || only.contains(name)) && !exclude.contains(name)
  
  /**
   * EventListenerConfig builder (Useful for Java user)
   */
  class SelectionConfigBuilder {
    private val default = SelectionConfig()
    private var only = default.only
    private var exclude = default.exclude

    fun only(only: List<String>) =
        apply { this.only = only }

    fun exclude(exclude: List<String>) =
        apply { this.exclude = exclude }

    fun build() = SelectionConfig(
        only,
        exclude,
    )
  }

  private fun error(txt: String) = "eventListener: $txt"
}
