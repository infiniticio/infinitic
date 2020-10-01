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

package io.infinitic.messaging.api.utils

import io.infinitic.common.data.SerializedData
import io.infinitic.common.tasks.data.MethodInput
import io.infinitic.avro.taskManager.data.AvroSerializedData
import io.infinitic.avro.taskManager.data.AvroSerializedDataType
import io.kotest.properties.nextPrintableString
import java.nio.ByteBuffer
import kotlin.random.Random
import kotlin.reflect.KClass
import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters
import org.jeasy.random.FieldPredicates
import org.jeasy.random.api.Randomizer

object TestFactory {
    private var seed = 0L

    fun seed(seed: Long): TestFactory {
        TestFactory.seed = seed

        return this
    }

    fun <T : Any> random(klass: KClass<T>, values: Map<String, Any?>? = null): T {
        // if not updated, 2 subsequents calls to this method would provide the same values
        seed++

        val parameters = EasyRandomParameters()
            .seed(seed)
            .scanClasspathForConcreteTypes(true)
            .overrideDefaultInitialization(true)
            .randomize(ByteBuffer::class.java) { ByteBuffer.wrap(Random(seed).nextBytes(10)) }
            .randomize(ByteArray::class.java) { Random(seed).nextBytes(10) }
            .randomize(SerializedData::class.java) { SerializedData.from(Random(seed).nextPrintableString(10)) }
            .randomize(AvroSerializedData::class.java) {
                val data = random(SerializedData::class)
                AvroSerializedData.newBuilder()
                    .setBytes(ByteBuffer.wrap(data.bytes))
                    .setType(AvroSerializedDataType.JSON)
                    .setMeta(data.meta.mapValues { ByteBuffer.wrap(it.value) })
                    .build()
            }
            .randomize(MethodInput::class.java) {
                MethodInput(
                    Random(seed).nextBytes(10),
                    Random(seed).nextPrintableString(10)
                )
            }

        values?.forEach {
            parameters.randomize(FieldPredicates.named(it.key), Randomizer { it.value })
        }

        return EasyRandom(parameters).nextObject(klass.java)
    }
}
