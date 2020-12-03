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

package io.infinitic.worker.inMemory

import io.infinitic.common.monitoring.global.messages.MonitoringGlobalMessage
import io.infinitic.common.monitoring.global.transport.SendToMonitoringGlobal
import io.infinitic.common.monitoring.perName.messages.MonitoringPerNameEngineMessage
import io.infinitic.monitoring.perName.engine.MonitoringPerNameEngine
import io.infinitic.monitoring.perName.engine.storage.MonitoringPerNameStateStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

fun CoroutineScope.startMonitoringPerNameEngine(
    monitoringPerNameStateStorage: MonitoringPerNameStateStorage,
    monitoringPerNameChannel: ReceiveChannel<MonitoringPerNameEngineMessage>,
    monitoringGlobalChannel: SendChannel<MonitoringGlobalMessage>
) = launch {

    val sendToMonitoringGlobal: SendToMonitoringGlobal = { monitoringGlobalChannel.send(it) }

    val monitoringPerNameEngine = MonitoringPerNameEngine(
        monitoringPerNameStateStorage,
        sendToMonitoringGlobal
    )

    while (true) {
        select<Unit> {
            monitoringPerNameChannel.onReceive { monitoringPerNameEngine.handle(it) }
        }
    }
}
