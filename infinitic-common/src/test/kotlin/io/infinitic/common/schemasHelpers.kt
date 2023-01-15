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
package io.infinitic.common

import com.github.avrokotlin.avro4k.Avro
import io.infinitic.common.serDe.avro.AvroSerDe.SCHEMAS_FOLDER
import io.infinitic.common.serDe.avro.AvroSerDe.getAllSchemas
import io.infinitic.common.serDe.avro.AvroSerDe.getSchemaFilePrefix
import io.infinitic.version
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import java.io.File
import java.nio.file.Paths
import kotlinx.serialization.KSerializer
import org.apache.avro.SchemaValidatorBuilder

internal inline fun <reified T : Any> createSchemaFileIfAbsent(serializer: KSerializer<T>) {
  val schemaFile = getCurrentSchemaFile<T>()

  if (!schemaFile.exists()) {
    // write schema to schemaFile in resources
    schemaFile.writeText(Avro.default.schema(serializer).toString(true))
  }
}

internal inline fun <reified T : Any> checkCurrentFileIsUpToDate(serializer: KSerializer<T>) {
  getCurrentSchemaFile<T>().readText() shouldBe Avro.default.schema(serializer).toString(true)
}

internal inline fun <reified T : Any> checkBackwardCompatibility(serializer: KSerializer<T>) {
  val schemaList = getAllSchemas<T>().values

  // checking that we did not silently break something
  schemaList.size shouldBeGreaterThan 1

  // checking that we can read T from any previous version
  val validator = SchemaValidatorBuilder().canReadStrategy().validateAll()
  val newSchema = Avro.default.schema(serializer)
  shouldNotThrowAny { validator.validate(newSchema, schemaList) }
}

internal inline fun <reified T : Any> getCurrentSchemaFile() =
    File(
        "${Paths.get("").toAbsolutePath()}/src/main/resources/$SCHEMAS_FOLDER/",
        "${getSchemaFilePrefix<T>()}-$version.avsc")
