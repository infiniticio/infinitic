/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.common.workflows.data.channels

import com.jayway.jsonpath.Criteria
import io.infinitic.exceptions.workflows.NameNotInitializedInChannelException
import io.infinitic.workflows.Channel
import io.infinitic.workflows.Deferred
import io.infinitic.workflows.WorkflowDispatcher

class ChannelImpl<T : Any>(
    private val dispatcherFn: () -> WorkflowDispatcher
) : Channel<T> {
    lateinit var name: String

    fun isNameInitialized() = ::name.isInitialized

    fun getNameOrThrow() = when (isNameInitialized()) {
        true -> name
        else -> throw NameNotInitializedInChannelException
    }

    override fun send(signal: T) =
        dispatcherFn().sendSignal(this, signal)

    override fun receive(jsonPath: String?, criteria: Criteria?): Deferred<T> =
        dispatcherFn().receiveSignal(this, jsonPath, criteria)

    override fun <S : T> receive(klass: Class<S>, jsonPath: String?, criteria: Criteria?): Deferred<S> =
        dispatcherFn().receiveSignal(this, klass, jsonPath, criteria)
}
