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

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${project.extra["kotlinx_coroutines_version"]}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${project.extra["kotlinx_coroutines_version"]}")
    implementation("com.sksamuel.hoplite:hoplite-core:${project.extra["hoplite_version"]}")
    implementation("com.sksamuel.hoplite:hoplite-yaml:${project.extra["hoplite_version"]}")
    implementation("org.apache.pulsar:pulsar-client:${project.extra["pulsar_version"]}")

    api(project(":infinitic-common"))
    api(project(":infinitic-monitoring-engines"))
    api(project(":infinitic-task-engine"))
    api(project(":infinitic-workflow-engine"))
    api(project(":infinitic-pulsar"))
    api(project(":infinitic-storage"))
    api(project(":infinitic-pulsar"))
    api(project(":infinitic-task-executor"))
}
