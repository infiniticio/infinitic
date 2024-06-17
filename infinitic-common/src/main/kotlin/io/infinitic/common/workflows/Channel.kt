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
package io.infinitic.common.workflows

import com.jayway.jsonpath.Criteria
import io.infinitic.exceptions.workflows.ChannelWithoutGetterException
import io.infinitic.workflows.Deferred
import io.infinitic.workflows.SendChannel

class Channel<T : Any>(private val dispatcherFn: () -> WorkflowDispatcher) : SendChannel<T> {
  private lateinit var _name: String

  internal fun setName(name: String) {
    _name = name
  }

  internal fun hasName() = ::_name.isInitialized

  val name by lazy {
    when (hasName()) {
      true -> _name
      else -> throw ChannelWithoutGetterException
    }
  }

  override fun send(signal: T) = dispatcherFn().send(this, signal)

  @JvmOverloads
  fun receive(
    limit: Int? = null,
    jsonPath: String? = null,
    criteria: Criteria? = null
  ): Deferred<T> = dispatcherFn().receive(this, null, limit, jsonPath, criteria)

  @JvmOverloads
  fun <S : T> receive(
    klass: Class<S>,
    limit: Int? = null,
    jsonPath: String? = null,
    criteria: Criteria? = null
  ): Deferred<S> = dispatcherFn().receive(this, klass, limit, jsonPath, criteria)
}
