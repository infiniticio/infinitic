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

package io.infinitic.common.proxies

import io.infinitic.common.proxies.data.Signal
import io.infinitic.common.workflows.data.channels.ChannelName
import io.infinitic.common.workflows.data.channels.ChannelSignal
import io.infinitic.common.workflows.data.channels.ChannelSignalId
import io.infinitic.common.workflows.data.channels.ChannelSignalType
import io.infinitic.workflows.SendChannel

@Suppress("UNCHECKED_CAST")
class ChannelSelectionProxyHandler<K : SendChannel<*>>(
    val handler: WorkflowSelectionProxyHandler<*>,
) : ProxyHandler<K>(
    handler.method.returnType as Class<out K>,
    handler.dispatcherFn
),
    ChannelProxyHandler {

    override val workflowName = handler.workflowName
    override val channelName = ChannelName(handler.methodName)

    fun signal(): Signal {
        val signal = methodArgs[0]

        return Signal(
            workflowName,
            channelName,
            ChannelSignalId(),
            ChannelSignal.from(signal),
            ChannelSignalType.allFrom(signal::class.java),
            handler.perWorkflowId,
            handler.perWorkflowTag
        )
    }
}
