// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.common.workers.messages

import com.github.avrokotlin.avro4k.Avro
import io.infinitic.common.tasks.data.TaskName
import kotlinx.serialization.Serializable
import java.nio.ByteBuffer

@Serializable
data class WorkerEnvelope(
    val taskName: TaskName,
    val type: WorkerMessageType,
    val runTask: RunTask? = null
) {
    init {
        val noNull = listOfNotNull(
            runTask
        )

        require(noNull.size == 1)
        require(noNull.first() == message())
        require(noNull.first().taskName == taskName)
    }

    companion object {
        fun from(msg: WorkerMessage) = when (msg) {
            is RunTask -> WorkerEnvelope(
                msg.taskName,
                WorkerMessageType.RUN_TASK,
                runTask = msg
            )
        }

        fun fromByteArray(bytes: ByteArray) = Avro.default.decodeFromByteArray(serializer(), bytes)
        fun fromByteBuffer(bytes: ByteBuffer) = fromByteArray(bytes.array())
    }

    fun message(): WorkerMessage = when (type) {
        WorkerMessageType.RUN_TASK -> runTask!!
    }

    fun toByteArray() = Avro.default.encodeToByteArray(serializer(), this)
    fun toByteBuffer(): ByteBuffer = ByteBuffer.wrap(toByteArray())
}
