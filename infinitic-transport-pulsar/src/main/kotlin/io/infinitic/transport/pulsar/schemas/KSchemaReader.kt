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

import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.messages.Envelope
import io.infinitic.common.messages.fromByteArray
import org.apache.avro.Schema
import org.apache.pulsar.client.api.schema.SchemaInfoProvider
import org.apache.pulsar.client.api.schema.SchemaReader
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class KSchemaReader<T : Envelope<*>>(private val klass: KClass<T>) : SchemaReader<T> {
    companion object {
        private val schemasCache = ConcurrentHashMap<String, Schema>()
    }

    private lateinit var schemaInfoProvider: SchemaInfoProvider

    // For our convenience, Pulsar will inject here an instance of
    // org.apache.pulsar.client.impl.schema.generic.MultiVersionSchemaInfoProvider
    override fun setSchemaInfoProvider(schemaInfoProvider: SchemaInfoProvider) {
        this.schemaInfoProvider = schemaInfoProvider
    }

    override fun read(bytes: ByteArray, offset: Int, length: Int) =
        thisShouldNotHappen("KSchemaReader should not be without schemaVersion")

    override fun read(inputStream: InputStream) =
        thisShouldNotHappen("KSchemaReader should not be without schemaVersion")

    override fun read(bytes: ByteArray, schemaVersion: ByteArray): T =
        klass.fromByteArray(bytes, getSchemaByVersion(schemaVersion))

    override fun read(inputStream: InputStream, schemaVersion: ByteArray): T {
        return read(inputStream.readBytes(), schemaVersion)
    }

    // Retrieve the Avro Schema from schemaVersion
    private fun getSchemaByVersion(schemaVersion: ByteArray): Schema {
        // retrieve cached Pulsar schemaDefinition from the schemaVersion
        val schemaDefinition = schemaInfoProvider.getSchemaByVersion(schemaVersion).get().schemaDefinition

        // parse Avro Schema from the String schemaDefinition
        // we use a new parser instance each time, as the same schema name can have different versions
        return schemasCache.getOrPut(schemaDefinition) {
            Schema.Parser().apply { validateDefaults = false }.parse(schemaDefinition)
        }
    }
}
