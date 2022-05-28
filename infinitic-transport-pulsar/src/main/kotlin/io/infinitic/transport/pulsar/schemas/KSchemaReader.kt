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

package io.infinitic.transport.pulsar.schemas

import io.infinitic.common.serDe.avro.AvroSerDe
import io.infinitic.common.serDe.kserializer.kserializer
import org.apache.avro.Schema
import org.apache.pulsar.client.api.schema.SchemaInfoProvider
import org.apache.pulsar.client.api.schema.SchemaReader
import org.apache.pulsar.common.schema.SchemaInfo
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class KSchemaReader<T : Any>(private val klass: KClass<T>) : SchemaReader<T> {
    companion object {
        private val schemasCache = ConcurrentHashMap<String, Schema>()
    }

    private val schemaParser = Schema.Parser().apply { validateDefaults = false }

    private lateinit var schemaInfoProvider: SchemaInfoProvider

    override fun read(bytes: ByteArray, offset: Int, length: Int) =
        read(bytes.inputStream(offset, length))

    override fun read(inputStream: InputStream) =
        AvroSerDe.readBinary(inputStream.readBytes(), kserializer(klass))

    override fun read(bytes: ByteArray, schemaVersion: ByteArray): T =
        AvroSerDe.readBinary(bytes, kserializer(klass), parseAvroSchema(getSchemaInfoByVersion(schemaVersion)))

    override fun read(inputStream: InputStream, schemaVersion: ByteArray): T {
        return read(inputStream.readBytes(), schemaVersion)
    }

    // Pulsar will inject here an instance of
    // org.apache.pulsar.client.impl.schema.generic.MultiVersionSchemaInfoProvider
    // for our convenience
    override fun setSchemaInfoProvider(schemaInfoProvider: SchemaInfoProvider) {
        this.schemaInfoProvider = schemaInfoProvider
    }

    // retrieve Pulsar SchemaInfo from the schemaVersion
    // luckily, the method getSchemaByVersion includes a cache
    private fun getSchemaInfoByVersion(schemaVersion: ByteArray): SchemaInfo? = schemaInfoProvider.getSchemaByVersion(schemaVersion).get()

    // retrieve the Avro Schema from our cache or parse it from the SchemaInfo schema definition
    private fun parseAvroSchema(schemaInfo: SchemaInfo?): Schema? = schemaInfo?.schemaDefinition!!.let {
        schemasCache.getOrPut(it) { schemaParser.parse(it) }
    }
}
