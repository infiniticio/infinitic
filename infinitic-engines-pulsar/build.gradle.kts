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

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.kotlin.dsl.support.serviceOf
import java.io.BufferedReader
import java.io.InputStreamReader

plugins {
    application
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${project.extra["kotlinx_coroutines_version"]}")
    implementation("org.apache.pulsar:pulsar-client:${project.extra["pulsar_version"]}")
    implementation("org.apache.pulsar:pulsar-functions-api:${project.extra["pulsar_version"]}")
    implementation("com.xenomachina:kotlin-argparser:2.0.+")

    implementation(project(":infinitic-common"))
    implementation(project(":infinitic-engines"))
    implementation(project(":infinitic-task-engine"))
    implementation(project(":infinitic-workflow-engine"))
    implementation(project(":infinitic-messaging-pulsar"))
    implementation(project(":infinitic-storage"))
}

application {
    mainClassName = "io.infinitic.engine.pulsar.main.MainKt"
}

tasks {
    named<ShadowJar>("shadowJar") {
        mergeServiceFiles()
    }
}

tasks.register("setRetention") {
    group = "Infinitic"
    description = "Set retention for default tenant/namespace to 1G"
    doLast {
        println("Set Pulsar retention to 1G size for public/default")
        val cmd = "$pulsarAdmin namespaces set-retention public/default --size 1G --time -1"
        exec(cmd)
    }
}

tasks.register("setSchemas") {
    group = "Infinitic"
    description = "Upload Infinitic schemas into Pulsar"
    dependsOn("assemble")
    doLast {
        createSchemaFiles()
//        uploadSchemaToTopic(
//            name = "AvroEnvelopeForWorkflowEngine",
//            topic = Topic.WORKFLOW_ENGINE.get()
//        )
        uploadSchemaToTopic(
            name = "TaskEngine",
            topic = Topic.TASK_ENGINE.get()
        )
        uploadSchemaToTopic(
            name = "MonitoringPerName",
            topic = Topic.MONITORING_PER_NAME.get()
        )
        uploadSchemaToTopic(
            name = "MonitoringGlobal",
            topic = Topic.MONITORING_GLOBAL.get()
        )
    }
}

tasks.register("install") {
    group = "Infinitic"
    description = "Install Infinitic into Pulsar"
    dependsOn("setRetention")
    dependsOn("setSchemas")
    doLast {
        setInfiniticFunction(
            name = "infinitic-workflows-engine",
            className = "WorkflowEnginePulsarFunction",
            classNamespace = "io.infinitic.engine.pulsar.workflowManager.functions",
            topicsIn = setOf(Topic.WORKFLOW_ENGINE.get()),
            action = "create"
        )
        setInfiniticFunction(
            name = "infinitic-tasks-engine",
            className = "TaskEnginePulsarFunction",
            classNamespace = "io.infinitic.engine.pulsar.taskManager.functions",
            topicsIn = setOf(Topic.TASK_ENGINE.get()),
            action = "create"
        )
        setInfiniticFunction(
            name = "infinitic-tasks-monitoring-global",
            className = "MonitoringGlobalPulsarFunction",
            classNamespace = "io.infinitic.engine.pulsar.taskManager.functions",
            topicsIn = setOf(Topic.MONITORING_GLOBAL.get()),
            action = "create"
        )
        setInfiniticFunction(
            name = "infinitic-tasks-monitoring-per-name",
            className = "MonitoringPerNamePulsarFunction",
            classNamespace = "io.infinitic.engine.pulsar.taskManager.functions",
            topicsIn = setOf(Topic.MONITORING_PER_NAME.get()),
            action = "create"
        )
    }
}

tasks.register("update") {
    group = "Infinitic"
    description = "Update Infinitic into Pulsar"
    dependsOn("setSchemas")
    doLast {
        setInfiniticFunction(
            name = "infinitic-workflows-engine",
            className = "WorkflowEnginePulsarFunction",
            classNamespace = "io.infinitic.engine.pulsar.workflowManager.functions",
            topicsIn = setOf(Topic.WORKFLOW_ENGINE.get()),
            action = "update"
        )
        setInfiniticFunction(
            name = "infinitic-tasks-engine",
            className = "TaskEnginePulsarFunction",
            classNamespace = "io.infinitic.engine.pulsar.taskManager.functions",
            topicsIn = setOf(Topic.TASK_ENGINE.get()),
            action = "update"
        )
        setInfiniticFunction(
            name = "infinitic-tasks-monitoring-global",
            className = "MonitoringGlobalPulsarFunction",
            classNamespace = "io.infinitic.engine.pulsar.taskManager.functions",
            topicsIn = setOf(Topic.MONITORING_GLOBAL.get()),
            action = "update"
        )
        setInfiniticFunction(
            name = "infinitic-tasks-monitoring-per-name",
            className = "MonitoringPerNamePulsarFunction",
            classNamespace = "io.infinitic.engine.pulsar.taskManager.functions",
            topicsIn = setOf(Topic.MONITORING_PER_NAME.get()),
            action = "update"
        )
    }
}

tasks.register("delete") {
    group = "Infinitic"
    description = "Delete Infinitic from Pulsar"
    doLast {
        deleteInfiniticFunction("infinitic-workflows-engine")
        deleteInfiniticFunction("infinitic-tasks-engine")
        deleteInfiniticFunction("infinitic-tasks-monitoring-global")
        deleteInfiniticFunction("infinitic-tasks-monitoring-per-name")
        forceDeleteTopic(Topic.WORKFLOW_ENGINE.get())
        forceDeleteTopic(Topic.TASK_ENGINE.get())
        forceDeleteTopic(Topic.MONITORING_PER_NAME.get())
        forceDeleteTopic(Topic.MONITORING_GLOBAL.get())
        forceDeleteTopic(Topic.LOGS.get())
    }
}

val pulsarAdmin = "docker-compose -f ../pulsar/docker-compose.yml exec -T pulsar bin/pulsar-admin"
val jar = "infinitic-engine-pulsar-1.0.0-SNAPSHOT-all.jar"

enum class Topic {
    WORKFLOW_ENGINE {
        override fun get(name: String?) = "workflows-engine"
    },
    TASK_ENGINE {
        override fun get(name: String?) = "tasks-engine"
    },
    WORKERS {
        override fun get(name: String?) = "tasks-workers-$name"
    },
    MONITORING_PER_INSTANCE {
        override fun get(name: String?) = "tasks-monitoring-per-instance"
    },
    MONITORING_PER_NAME {
        override fun get(name: String?) = "tasks-monitoring-per-name"
    },
    MONITORING_GLOBAL {
        override fun get(name: String?) = "tasks-monitoring-global"
    },
    LOGS {
        override fun get(name: String?) = "tasks-logs"
    };

    abstract fun get(name: String? = ""): String
}

fun createSchemaFiles() {
    // create schema files
    println("Creating schemas files...")
    exec("java -cp ./build/libs/$jar io.infinitic.engine.pulsar.schemas.GenerateSchemaFilesKt")
}

fun uploadSchemaToTopic(
    name: String,
    topic: String,
    tenant: String = "public",
    namespace: String = "default"
) {
    println("Uploading $name schema to $topic topic...")
    val cmd = "$pulsarAdmin schemas upload \"persistent://$tenant/$namespace/$topic\"" +
        " --filename \"/infinitic/schemas/$name.schema\" "
    return exec(cmd)
}

fun setInfiniticFunction(
    name: String,
    className: String,
    classNamespace: String,
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
    var cmd = "$pulsarAdmin functions $action --jar /infinitic/libs/$jar" +
        " --classname \"$classNamespace.$className\" --inputs $inputs " +
        " --name \"$name\" --log-topic \"persistent://$tenant/$namespace/$logs\""
    if (topicOut != null) {
        cmd += " --output \"persistent://$tenant/$namespace/$topicOut\""
    }

    return exec(cmd)
}

fun deleteInfiniticFunction(name: String, tenant: String = "public", namespace: String = "default") {
    println("Deleting $name function from $tenant/$namespace...")
    val cmd = "$pulsarAdmin functions delete --tenant \"$tenant\" --namespace \"$namespace\" --name \"$name\""

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
    if (0 != p.waitFor()) throw GradleException("The following command failed to execute properly: $cmd")
}
