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

package io.infinitic.common

import Ci
import com.github.avrokotlin.avro4k.Avro
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.serialization.KSerializer
import org.apache.avro.Schema
import org.apache.avro.SchemaValidatorBuilder
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.readText

internal fun getSchemasResourcesPath(): Path {
    val projectDirAbsolutePath = Paths.get("").toAbsolutePath().toString()

    return Paths.get(projectDirAbsolutePath, "/src/test/resources/schemas/")
}

internal inline fun <reified T : Any> createSchemaFileIfAbsent(serializer: KSerializer<T>) {
    val schemaFile = getCurrentSchemaFile<T>()

    if (!schemaFile.exists()) {
        val snapshotFile = getSnapshotSchemaFile<T>()
        if (Ci.isRelease && snapshotFile.exists()) {
            // if it's a release, we move the snapshot file to the release file
            Files.move(snapshotFile.toPath(), snapshotFile.resolveSibling(schemaFile).toPath())
        } else {
            // write WorkflowState schema to schemaFile in resources
            schemaFile.writeText(Avro.default.schema(serializer).toString(true))
        }
    }
}

internal inline fun <reified T : Any> checkCurrentFileIsUpToDate(serializer: KSerializer<T>) {
    getCurrentSchemaFile<T>().readText() shouldBe Avro.default.schema(serializer).toString(true)
}

@OptIn(ExperimentalPathApi::class)
internal inline fun <reified T : Any> checkBackwardCompatibility(serializer: KSerializer<T>) {
    val regex = ".*/${getFilePrefix<T>()}-.*\\.avsc$".toRegex()
    val schemaList = mutableListOf<Schema>()
    Files.walk(getSchemasResourcesPath())
        .filter { Files.isRegularFile(it) }
        .filter { regex.matches(it.toString()) }
        .forEach { schemaList.add(Schema.Parser().parse(it.readText())) }

    // checking that we did not silently break something
    schemaList.size shouldBeGreaterThan 1

    // checking that we can read T from any previous version
    val validator = SchemaValidatorBuilder().canReadStrategy().validateAll()
    val newSchema = Avro.default.schema(serializer)
    shouldNotThrowAny { validator.validate(newSchema, schemaList) }
}

@OptIn(ExperimentalStdlibApi::class)
internal inline fun <reified T : Any> getFilePrefix() = T::class.simpleName!!.replaceFirstChar { it.lowercase() }

internal inline fun <reified T : Any> getCurrentSchemaFile() = File(getSchemasResourcesPath().toString(), "${getFilePrefix<T>()}-${Ci.version}.avsc")

internal inline fun <reified T : Any> getSnapshotSchemaFile() = File(
    getSchemasResourcesPath().toString(),
    when (Ci.isRelease) {
        true -> "${getFilePrefix<T>()}-${Ci.version}-SNAPSHOT.avsc"
        false -> "${getFilePrefix<T>()}-${Ci.version}.avsc"
    }
)
