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
package io.infinitic.common.fixtures

import io.infinitic.common.serDe.avro.AvroSerDe
import io.infinitic.common.serDe.avro.AvroSerDe.SCHEMAS_FOLDER
import io.infinitic.common.serDe.avro.AvroSerDe.getAllSchemas
import io.infinitic.common.serDe.avro.AvroSerDe.getSchemaFilePrefix
import io.infinitic.currentVersion
import io.infinitic.isReleaseVersion
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.serialization.KSerializer
import org.apache.avro.SchemaValidatorBuilder
import java.io.File
import java.io.FileWriter
import kotlin.reflect.KClass

fun <T : Any> checkOrCreateCurrentFile(klass: KClass<T>, serializer: KSerializer<T>) {
  // Non-release version are not saved to the sources
  if (isReleaseVersion) {
    val schemaFilePath = File(
        "/src/main/resources/$SCHEMAS_FOLDER/",
        "${getSchemaFilePrefix(klass)}-$currentVersion.avsc",
    )

    // Does the schema file already exist?
    when (schemaFilePath.exists()) {
      // create schema file for the current version
      false -> FileWriter(schemaFilePath).use {
        it.write(AvroSerDe.currentSchema(serializer).toString(true))
      }
      // check that schema file was not modified
      true -> AvroSerDe.currentSchema(serializer).toString(true) shouldBe schemaFilePath.readText()
    }
  }
}

fun <T : Any> checkBackwardCompatibility(klass: KClass<T>, serializer: KSerializer<T>) {
  val schemaList = getAllSchemas(klass).values

  // checking that we did not silently break something
  schemaList.size shouldBeGreaterThan 0

  // checking that we can read T from any previous version
  val validator = SchemaValidatorBuilder().canReadStrategy().validateAll()
  val newSchema = AvroSerDe.currentSchema(serializer)
  shouldNotThrowAny { validator.validate(newSchema, schemaList) }
}


