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

plugins {
    `java-library`
}

dependencies {
    implementation(Libs.Coroutines.core)
    implementation(Libs.Coroutines.jdk8)
    implementation(Libs.Pulsar.functions)
    implementation(Libs.Avro4k.core)
    implementation(Libs.Hoplite.core)
    implementation(Libs.Hoplite.yaml)

    // do not change the order od these 2 depencies
    // to keep avro shaded and avoid https://github.com/apache/pulsar/issues/9045
    api(Libs.Pulsar.client)
    api(Libs.Pulsar.clientAdmin)
    api(project(":infinitic-storage"))

    implementation(project(":infinitic-common"))
    implementation(project(":infinitic-client"))
    implementation(project(":infinitic-monitoring-engines"))
    implementation(project(":infinitic-task-engine"))
    implementation(project(":infinitic-task-executor"))
    implementation(project(":infinitic-workflow-engine"))
}

apply("../publish.gradle.kts")
