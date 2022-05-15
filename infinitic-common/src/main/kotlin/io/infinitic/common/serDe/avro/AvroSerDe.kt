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

package io.infinitic.common.serDe.avro

import com.github.avrokotlin.avro4k.Avro
import com.github.avrokotlin.avro4k.io.AvroEncodeFormat
import io.infinitic.common.serDe.kserializer.kserializer
import kotlinx.serialization.KSerializer
import org.apache.avro.file.SeekableByteArrayInput
import org.apache.avro.generic.GenericDatumReader
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.DecoderFactory
import java.io.ByteArrayOutputStream
import kotlin.reflect.KClass

/**
 * This object provides methods to serialize/deserialize an Avro-generated class
 */
object AvroSerDe {
    fun <T : Any> schema(klass: KClass<T>) = Avro.default.schema(kserializer(klass))

    fun <T : Any> writeBinary(t: T, serializer: KSerializer<T>): ByteArray {
        val out = ByteArrayOutputStream()
        Avro.default.openOutputStream(serializer) {
            encodeFormat = AvroEncodeFormat.Binary
            schema = Avro.default.schema(serializer)
        }.to(out).write(t).close()

        return out.toByteArray()
    }

    fun <T> readBinary(bytes: ByteArray, serializer: KSerializer<T>): T {
        val datumReader = GenericDatumReader<GenericRecord>(Avro.default.schema(serializer))
        val decoder = DecoderFactory.get().binaryDecoder(SeekableByteArrayInput(bytes), null)

        return Avro.default.fromRecord(serializer, datumReader.read(null, decoder))
    }
}
