import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.kotlin.dsl.support.serviceOf
import java.io.BufferedReader
import java.io.InputStreamReader

plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow") version "5.2.0"
    id("org.jlleitschuh.gradle.ktlint") version "9.2.1"
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.apache.pulsar:pulsar-client:2.5.+")
    implementation("org.apache.pulsar:pulsar-functions-api:2.5.+")
    implementation("org.apache.avro:avro:1.9.+")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.11.+")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.+")
    implementation("org.slf4j:slf4j-api:1.7.+")

    implementation(project(":zenaton-common"))
    implementation(project(":zenaton-jobManager"))

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

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks {
    named<ShadowJar>("shadowJar") {
        mergeServiceFiles()
    }
}

tasks.register("setSchemas") {
    group = "Zenaton"
    description = "Upload Zenaton schemas into Pulsar"
    dependsOn("assemble")
    doLast {
        createSchemaFiles()
        setPrefix()
        uploadSchemaToTopic(
            name = "AvroEnvelopeForJobEngine",
            topic = Topic.ENGINE.get()
        )
        uploadSchemaToTopic(
            name = "AvroEnvelopeForMonitoringPerName",
            topic = Topic.MONITORING_PER_NAME.get()
        )
        uploadSchemaToTopic(
            name = "AvroEnvelopeForMonitoringGlobal",
            topic = Topic.MONITORING_GLOBAL.get()
        )
    }
}

tasks.register("install") {
    group = "Zenaton"
    description = "Install Zenaton into Pulsar"
    dependsOn("setSchemas")
    doLast {
        setZenatonFunction(
            className = "JobEnginePulsarFunction",
            topicsIn = setOf(Topic.ENGINE.get()),
            action = "create"
        )
        setZenatonFunction(
            className = "MonitoringGlobalPulsarFunction",
            topicsIn = setOf(Topic.MONITORING_GLOBAL.get()),
            action = "create"
        )
        setZenatonFunction(
            className = "MonitoringPerNamePulsarFunction",
            topicsIn = setOf(Topic.MONITORING_PER_NAME.get()),
            action = "create"
        )
    }
}

tasks.register("update") {
    group = "Zenaton"
    description = "Update Zenaton into Pulsar"
    dependsOn("setSchemas")
    doLast {
        setZenatonFunction(
            className = "JobEnginePulsarFunction",
            topicsIn = setOf(Topic.ENGINE.get()),
            action = "update"
        )
        setZenatonFunction(
            className = "MonitoringGlobalPulsarFunction",
            classNamespace = "com.zenaton.jobManager.pulsar.functions",
            topicsIn = setOf(Topic.MONITORING_GLOBAL.get()),
            action = "update"
        )
        setZenatonFunction(
            className = "MonitoringPerNamePulsarFunction",
            topicsIn = setOf(Topic.MONITORING_PER_NAME.get()),
            action = "update"
        )
    }
}

tasks.register("delete") {
    group = "Zenaton"
    description = "Delete Zenaton from Pulsar"
    doLast {
        setPrefix()
        deleteZenatonFunction("JobEnginePulsarFunction")
        deleteZenatonFunction("MonitoringGlobalPulsarFunction")
        deleteZenatonFunction("MonitoringPerNamePulsarFunction")
        forceDeleteTopic(Topic.ENGINE.get())
        forceDeleteTopic(Topic.MONITORING_PER_NAME.get())
        forceDeleteTopic(Topic.MONITORING_GLOBAL.get())
        forceDeleteTopic(Topic.LOGS.get())
    }
}

tasks.register("prefix") {
    group = "Zenaton"
    description = "Delete Zenaton from Pulsar"
    doLast {
        setPrefix()
        println("Current prefix: ${Topic.prefix}")
    }
}

val pulsarAdmin = "docker-compose -f ../pulsar/docker-compose.yml exec -T pulsar bin/pulsar-admin"
val jar = "zenaton-jobManager-pulsar-1.0-SNAPSHOT-all.jar"

fun setPrefix() {
    if (gradle.rootProject.hasProperty("refix")) {
        Topic.prefix = gradle.rootProject.property("refix").toString()
    }
}

enum class Topic {
    ENGINE {
        override fun get(name: String?) = "${Topic.prefix}-engine"
    },
    WORKERS {
        override fun get(name: String?) = "${Topic.prefix}-workers-$name"
    },
    MONITORING_PER_INSTANCE {
        override fun get(name: String?) = "${Topic.prefix}-monitoring-per-instance"
    },
    MONITORING_PER_NAME {
        override fun get(name: String?) = "${Topic.prefix}-monitoring-per-name"
    },
    MONITORING_GLOBAL {
        override fun get(name: String?) = "${Topic.prefix}-monitoring-global"
    },
    LOGS {
        override fun get(name: String?) = "${Topic.prefix}-logs"
    };

    companion object {
        var prefix = "jobs"
    }

    abstract fun get(name: String? = ""): String
}

fun createSchemaFiles() {
    // create schema files
    println("Creating schemas files...")
    val cmd = "java -cp ./build/libs/$jar com.zenaton.jobManager.pulsar.utils.MainKt"
    return exec(cmd)
}

fun uploadSchemaToTopic(
    name: String,
    topic: String,
    tenant: String = "public",
    namespace: String = "default"
) {
    println("Uploading $name schema to $topic topic...")
    val cmd = "$pulsarAdmin schemas upload \"persistent://$tenant/$namespace/$topic\"" +
        " --filename \"/zenaton/jobManager/schemas/$name.schema\" "
    return exec(cmd)
}

fun setZenatonFunction(
    className: String,
    classNamespace: String = "com.zenaton.jobManager.pulsar.functions",
    topicsIn: Set<String>,
    action: String,
    topicOut: String? = null,
    logs: String = Topic.LOGS.get(),
    tenant: String = "public",
    namespace: String = "default"
) {
    val inputs = topicsIn.joinToString(
        separator = ",",
        transform = { "persistent://$tenant/$namespace/$it" }
    )
    println("$action $className for $inputs...")
    var cmd = "$pulsarAdmin functions $action --jar /zenaton/jobManager/libs/$jar" +
        " --classname \"$classNamespace.$className\" --inputs $inputs " +
        " --name \"${Topic.prefix}-$className\" --log-topic \"persistent://$tenant/$namespace/$logs\""
    if (topicOut != null) {
        cmd += " --output \"persistent://$tenant/$namespace/$topicOut\""
    }
    if (gradle.rootProject.hasProperty("refix")) {
        cmd += " --user-config {\"topicPrefix\":\"${gradle.rootProject.property("refix")}\"}"
    }
    return exec(cmd)
}

fun deleteZenatonFunction(name: String, tenant: String = "public", namespace: String = "default") {
    println("Deleting $name function from $tenant/$namespace...")
    val cmd = "$pulsarAdmin functions delete --tenant \"$tenant\" --namespace \"$namespace\" --name \"${Topic.prefix}-$name\""

    return exec(cmd)
}

fun forceDeleteTopic(topic: String, tenant: String = "public", namespace: String = "default") {
    println("Deleting $topic topic from $tenant/$namespace...")
    val cmd = "$pulsarAdmin topics delete \"persistent://$tenant/$namespace/$topic\" --deleteSchema --force"

    return exec(cmd)
}

fun exec(cmd: String) {
    val out = project.serviceOf<org.gradle.internal.logging.text.StyledTextOutputFactory>().create("an-output")
    val infoStyle = org.gradle.internal.logging.text.StyledTextOutput.Style.Info
    val errorStyle = org.gradle.internal.logging.text.StyledTextOutput.Style.Error
    val normalStyle = org.gradle.internal.logging.text.StyledTextOutput.Style.Normal
    out.style(infoStyle).println(cmd)

    val p = Runtime.getRuntime().exec(cmd.split(" ").toTypedArray())
    val output = BufferedReader(InputStreamReader(p.inputStream))
    val error = BufferedReader(InputStreamReader(p.errorStream))
    var line: String? = ""
    while (output.readLine().also { line = it } != null) out.style(normalStyle).println(line)
    while (error.readLine().also { line = it } != null) out.style(errorStyle).println(line)
    if (0 != p.waitFor()) throw StopActionException()
}
