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
import kotlin.io.path.readText

internal fun getSchemasResourcesPath(): Path {
    val projectDirAbsolutePath = Paths.get("").toAbsolutePath().toString()

    return Paths.get(projectDirAbsolutePath, "/src/test/resources/schemas/")
}

internal inline fun <reified T : Any> createShemaFileIfAbsent(serializer: KSerializer<T>) {
    val schemaFile = getCurrentFile<T>()

    if (!File(getSchemasResourcesPath().toString(), schemaFile).exists()) {
        // write WorkflowState schema to schemaFile in resources
        val schema = Avro.default.schema(serializer)
        val file = File(getSchemasResourcesPath().toString(), schemaFile)
        file.writeText(schema.toString(true))
    }
}

internal inline fun <reified T : Any> checkCurrentFileIsUpToDate(serializer: KSerializer<T>) {
    val schemaFile = getCurrentFile<T>()

    val savedSchema = File(getSchemasResourcesPath().toString(), schemaFile).readText()

    savedSchema shouldBe Avro.default.schema(serializer).toString(true)
}

internal inline fun <reified T : Any> checkBackwardCompatibility(serializer: KSerializer<T>) {
    val regex = ".*/${getFilePrefix<T>()}-.*\\.avsc$".toRegex()
    val schemaList = mutableListOf<Schema>()
    Files.walk(getSchemasResourcesPath())
        .filter { Files.isRegularFile(it) }
        .filter { regex.matches(it.toString()) }
        .forEach { schemaList.add(Schema.Parser().parse(it.readText())) }

    // checking that we did not silently break something
    schemaList.size shouldBeGreaterThan 0

    // checking that we can read T from any previous version
    val validator = SchemaValidatorBuilder().canReadStrategy().validateAll()
    val newSchema = Avro.default.schema(serializer)
    shouldNotThrowAny { validator.validate(newSchema, schemaList) }
}

internal inline fun <reified T : Any> getFilePrefix() = T::class.simpleName!!.replaceFirstChar { it.lowercase() }

internal inline fun <reified T : Any> getCurrentFile() = "${getFilePrefix<T>()}-${Ci.version}.avsc"
