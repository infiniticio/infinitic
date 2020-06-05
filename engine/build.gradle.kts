import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.io.BufferedReader
import java.io.InputStreamReader

group = "com.zenaton.engine"
version = "1.0-SNAPSHOT"

plugins {
    application
    kotlin("jvm") version "1.3.72"
    id("com.github.johnrengelman.shadow") version "5.2.0"
    id("org.jlleitschuh.gradle.ktlint") version "9.2.1"
    id("com.commercehub.gradle.plugin.avro") version "0.19.1"
}

application {
    mainClassName = "com.zenaton.MainKt"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.apache.pulsar:pulsar-client:2.5.+")
    implementation("org.apache.pulsar:pulsar-functions-api:2.5.+")
    implementation("org.apache.avro:avro:1.9.+")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.11.+")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.+")
    implementation("org.slf4j:slf4j-api:1.7.+")

    testImplementation("org.jeasy:easy-random-core:4.2.+")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:4.0.+")
    testImplementation("io.kotest:kotest-property-jvm:4.0.+")
    testImplementation("io.kotest:kotest-core-jvm:4.0.+")
    testImplementation("io.mockk:mockk:1.9.+")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}

tasks {
    named<ShadowJar>("shadowJar") {
        mergeServiceFiles()
    }
}

tasks {
    build {
        dependsOn("ktlintFormat")
        dependsOn(shadowJar)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

avro {
    isCreateSetters.set(false)
    isCreateOptionalGetters.set(false)
    isGettersReturnOptional.set(false)
    fieldVisibility.set("PRIVATE")
    outputCharacterEncoding.set("UTF-8")
    stringType.set("String")
    templateDirectory.set(null as String?)
    isEnableDecimalLogicalType.set(true)
    dateTimeLogicalType.set("JSR310")
}

tasks.register("set schemas") {
    group = "Zenaton"
    description = "Upload Zenaton schemas into Pulsar"
    dependsOn("assemble")
    doLast {
        createSchemaFiles()
        uploadSchemaToTopic(
            name = "AvroForEngineMessage",
            topic = "engine"
        )
        uploadSchemaToTopic(
            name = "AvroForMonitoringPerNameMessage",
            topic = "monitoring-per-name"
        )
        uploadSchemaToTopic(
            name = "AvroForMonitoringGlobalMessage",
            topic = "monitoring-global"
        )
    }
}

tasks.register("install") {
    group = "Zenaton"
    description = "Install Zenaton into Pulsar"
    dependsOn("set schemas")
    doLast {
        setZenatonFunction(
            className = "com.zenaton.jobManager.pulsar.engine.EngineFunction",
            topicsIn = setOf("engine"),
            action = "create"
        )
        setZenatonFunction(
            className = "com.zenaton.jobManager.pulsar.monitoring.global.MonitoringGlobalFunction",
            topicsIn = setOf("monitoring-global"),
            action = "create"
        )
        setZenatonFunction(
            className = "com.zenaton.jobManager.pulsar.monitoring.perName.MonitoringPerNameFunction",
            topicsIn = setOf("monitoring-per-name"),
            action = "create"
        )
    }
}

tasks.register("update") {
    group = "Zenaton"
    description = "Update Zenaton into Pulsar"
    dependsOn("set schemas")
    doLast {
        setZenatonFunction(
            className = "com.zenaton.jobManager.pulsar.engine.EngineFunction",
            topicsIn = setOf("engine"),
            action = "update"
        )
        setZenatonFunction(
            className = "com.zenaton.jobManager.pulsar.monitoring.global.MonitoringGlobalFunction",
            topicsIn = setOf("monitoring-global"),
            action = "update"
        )
        setZenatonFunction(
            className = "com.zenaton.jobManager.pulsar.monitoring.perName.MonitoringPerNameFunction",
            topicsIn = setOf("monitoring-per-name"),
            action = "update"
        )
    }
}

tasks.register("delete") {
    group = "Zenaton"
    description = "Delete Zenaton from Pulsar"
    doLast {
        deleteZenatonFunction("EngineFunction")
        deleteZenatonFunction("MonitoringGlobalFunction")
        deleteZenatonFunction("MonitoringPerNameFunction")
        forceDeleteTopic("engine")
        forceDeleteTopic("monitoring-per-instance")
        forceDeleteTopic("monitoring-per-name")
        forceDeleteTopic("monitoring-global")
        forceDeleteTopic("logs")
    }
}

fun createSchemaFiles() {
    // create schema files
    println("Creating schemas files...")
    val cmd = arrayOf("java", "-cp", "build/libs/engine-1.0-SNAPSHOT-all.jar", "com.zenaton.commons.utils.avro.MainKt")
    exec(cmd)
}

fun uploadSchemaToTopic(
    name: String,
    topic: String,
    tenant: String = "public",
    namespace: String = "default"
) {
    println("Uploading $name schema to $topic topic...")
    val cmd = arrayOf("docker-compose", "exec", "-T", "pulsar", "bin/pulsar-admin",
        "schemas",
        "upload",
        "persistent://$tenant/$namespace/$topic",
        "--filename", "/zenaton/engine/schemas/$name.schema"
    )
    exec(cmd)
}

fun setZenatonFunction(
    className: String,
    topicsIn: Set<String>,
    action: String,
    topicOut: String? = null,
    tenant: String = "public",
    namespace: String = "default"
) {
    val inputs = topicsIn.joinToString(
        separator = ",",
        transform = { "persistent://$tenant/$namespace/$it" }
    )
    println("$action $className for $inputs...")
    val cmd = mutableListOf("docker-compose", "exec", "-T", "pulsar", "bin/pulsar-admin",
        "functions", action,
        "--jar", "/zenaton/engine/libs/engine-1.0-SNAPSHOT-all.jar",
        "--log-topic", "persistent://$tenant/$namespace/logs",
        "--classname", className,
        "--inputs", inputs
    )
    if (topicOut != null) {
        cmd.add("--output")
        cmd.add("persistent://$tenant/$namespace/$topicOut")
    }
    exec(cmd.toTypedArray())
}

fun deleteZenatonFunction(name: String, tenant: String = "public", namespace: String = "default") {
    println("Deleting $name function from $tenant/$namespace...")
    val cmd = arrayOf("docker-compose", "exec", "-T", "pulsar", "bin/pulsar-admin",
        "functions", "delete",
        "--tenant", tenant,
        "--namespace", namespace,
        "--name", name
    )
    exec(cmd)
}

fun forceDeleteTopic(topic: String, tenant: String = "public", namespace: String = "default") {
    println("Deleting $topic topic from $tenant/$namespace...")
    val cmd = arrayOf("docker-compose", "exec", "-T", "pulsar", "bin/pulsar-admin",
        "topics", "delete",
        "persistent://$tenant/$namespace/$topic",
        "--deleteSchema",
        "--force"
    )
    exec(cmd)
}

fun exec(cmd: Array<String>) {
    val p = Runtime.getRuntime().exec(cmd)
    val output = BufferedReader(InputStreamReader(p.inputStream))
    val error = BufferedReader(InputStreamReader(p.errorStream))
    var line: String? = ""
    while (output.readLine().also { line = it } != null) println(line)
    while (error.readLine().also { line = it } != null) println(line)
    p.waitFor()
}
