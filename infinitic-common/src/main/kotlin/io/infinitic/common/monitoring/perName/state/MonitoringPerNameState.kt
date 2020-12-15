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

package io.infinitic.common.monitoring.perName.state

import com.github.avrokotlin.avro4k.Avro
import io.infinitic.common.monitoring.global.state.MonitoringGlobalState
import io.infinitic.common.serDe.kotlin.readBinary
import io.infinitic.common.serDe.kotlin.writeBinary
import io.infinitic.common.tasks.data.TaskName
import kotlinx.serialization.Serializable
import java.nio.ByteBuffer

@Serializable
data class MonitoringPerNameState(
    val taskName: TaskName,
    var runningOkCount: Long = 0,
    var runningWarningCount: Long = 0,
    var runningErrorCount: Long = 0,
    var terminatedCompletedCount: Long = 0,
    var terminatedCanceledCount: Long = 0
) {
    companion object {
        fun fromByteArray(bytes: ByteArray) = readBinary(bytes, serializer())
        fun fromByteBuffer(bytes: ByteBuffer) = fromByteArray(bytes.array())
    }

    fun toByteArray() = writeBinary(this, serializer())
    fun toByteBuffer(): ByteBuffer = ByteBuffer.wrap(toByteArray())
    fun deepCopy() = fromByteArray(toByteArray())
}
