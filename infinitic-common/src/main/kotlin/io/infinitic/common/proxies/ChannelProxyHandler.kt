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
package io.infinitic.common.proxies

import io.infinitic.common.workflows.data.channels.ChannelName
import io.infinitic.common.workflows.data.channels.ChannelType
import io.infinitic.common.workflows.data.channels.SignalData
import io.infinitic.exceptions.clients.InvalidChannelGetterException
import io.infinitic.workflows.SendChannel

@Suppress("UNCHECKED_CAST")
class ChannelProxyHandler<K : SendChannel<*>>(
    handler: ExistingWorkflowProxyHandler<*>,
) : ProxyHandler<K>(handler.method.returnType as Class<out K>, handler.dispatcherFn) {
  init {
    if (handler.method.returnType != SendChannel::class.java)
        throw InvalidChannelGetterException(handler.method.returnType.name)
  }

  val workflowName = handler.workflowName
  val channelName = ChannelName.from(handler.methodName)
  val workflowId = handler.workflowId
  val workflowTag = handler.workflowTag

  val channelTypes by lazy { ChannelType.allFrom(methodArgs.first()::class.java) }
  val signalData by lazy { SignalData.from(methodArgs.first()) }
}
