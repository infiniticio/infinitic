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

package io.infinitic.common.tasks.states

import com.sksamuel.avro4k.Avro
import io.infinitic.common.tasks.avro.AvroConverter
import io.infinitic.common.tasks.data.TaskAttemptId
import io.infinitic.common.tasks.data.TaskAttemptIndex
import io.infinitic.common.tasks.data.TaskAttemptRetry
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.MethodInput
import io.infinitic.common.tasks.data.MethodName
import io.infinitic.common.tasks.data.MethodParameterTypes
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.common.tasks.data.TaskStatus
import kotlinx.serialization.Serializable

sealed class State

@Serializable
data class TaskEngineState(
    val taskId: TaskId,
    val taskName: TaskName,
    val methodName: MethodName,
    val methodParameterTypes: MethodParameterTypes?,
    val methodInput: MethodInput,
    val taskStatus: TaskStatus,
    var taskAttemptId: TaskAttemptId,
    var taskAttemptIndex: TaskAttemptIndex = TaskAttemptIndex(0),
    var taskAttemptRetry: TaskAttemptRetry = TaskAttemptRetry(0),
    val taskOptions: TaskOptions,
    val taskMeta: TaskMeta
) : State() {
    fun deepCopy(): TaskEngineState {
        val serializer = TaskEngineState.serializer()
        val byteArray =  Avro.default.encodeToByteArray(serializer, this)

        return Avro.default.decodeFromByteArray(serializer, byteArray)
    }
}

@Serializable
data class MonitoringPerNameState(
    val taskName: TaskName,
    var runningOkCount: Long = 0,
    var runningWarningCount: Long = 0,
    var runningErrorCount: Long = 0,
    var terminatedCompletedCount: Long = 0,
    var terminatedCanceledCount: Long = 0
) : State() {
    fun deepCopy(): MonitoringPerNameState {
        val serializer = MonitoringPerNameState.serializer()
        val byteArray =  Avro.default.encodeToByteArray(serializer, this)

        return Avro.default.decodeFromByteArray(serializer, byteArray)
    }
}

@Serializable
data class MonitoringGlobalState(
    val taskNames: MutableSet<TaskName> = mutableSetOf()
) : State() {
    fun deepCopy() : MonitoringGlobalState {
        val serializer = MonitoringGlobalState.serializer()
        val byteArray =  Avro.default.encodeToByteArray(serializer, this)

        return Avro.default.decodeFromByteArray(serializer, byteArray)
    }
}
