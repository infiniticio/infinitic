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
import io.infinitic.current
import io.infinitic.isReleaseVersion
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.serialization.KSerializer
import org.apache.avro.SchemaValidatorBuilder
import java.io.File
import java.nio.file.Paths

internal inline fun <reified T : Any> checkOrCreateCurrentFile(
  serializer: KSerializer<T>
) {
  // Non-release version are not saved to the sources
  if (isReleaseVersion()) {
    val schemaFilePath = getCurrentSchemaFilePath<T>()

    // Does the schema file already exist?
    when (schemaFilePath.parentFile.exists()) {
      true -> // check that schema file was not modified
        Avro.default.schema(serializer).toString(true) shouldBe schemaFilePath.readText()

      false ->  // create schema file for the current version
        schemaFilePath.writeText(Avro.default.schema(serializer).toString(true))
    }
  }
}

internal inline fun <reified T : Any> checkBackwardCompatibility(serializer: KSerializer<T>) {
  val schemaList = getAllSchemas<T>().values

  // checking that we did not silently break something
  schemaList.size shouldBeGreaterThan 20

  // checking that we can read T from any previous version
  val validator = SchemaValidatorBuilder().canReadStrategy().validateAll()
  val newSchema = Avro.default.schema(serializer)
  shouldNotThrowAny { validator.validate(newSchema, schemaList) }
}

internal inline fun <reified T : Any> getCurrentSchemaFilePath() = File(
    "${Paths.get("").toAbsolutePath()}/src/main/resources/$SCHEMAS_FOLDER/",
    "${getSchemaFilePrefix<T>()}-$current.avsc",
)

