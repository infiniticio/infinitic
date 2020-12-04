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

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

allprojects {
    repositories {
        mavenCentral()
    }
}

plugins {
    kotlin("jvm") version "1.4.10" apply false
    kotlin("plugin.serialization") version "1.4.10" apply false
    id("org.jlleitschuh.gradle.ktlint") version "9.4.1" apply false
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    version = "1.0.0-SNAPSHOT"

    dependencies {
        "implementation"(platform("org.jetbrains.kotlin:kotlin-bom"))
        "testImplementation"("io.kotest:kotest-runner-junit5-jvm:4.3.+")
        "testImplementation"("io.kotest:kotest-property-jvm:4.3.+")
        "testImplementation"("io.mockk:mockk:1.10.2")

        if (name != "infinitic-common") {
            "testImplementation"(testFixtures(project(":infinitic-common")))
        }
    }

    extra["jackson_version"] = "2.12.+"
    extra["avro4k_version"] = "1.0.+"
    extra["hoplite_version"] = "1.3.+"
    extra["slf4j_version"] = "1.7.+"
    extra["kotlin_reflect_version"] = "1.4.10"
    extra["kotlinx_coroutines_version"] = "1.4.+"
    extra["kotlinx_serialization_version"] = "1.0.+"
    extra["pulsar_version"] = "2.7.+"
    extra["easyrandom_version"] = "4.2.+"
    extra["shadow_version"] = "6.1.+"

    tasks.withType<JavaCompile> {
        sourceCompatibility = JavaVersion.VERSION_1_8.toString()
        targetCompatibility = JavaVersion.VERSION_1_8.toString()
    }

    if (name != "infinitic-rest-api") {
        tasks.withType<Test> {
            useJUnitPlatform()
        }

        tasks.withType<KotlinCompile> {
            kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
        }
    }
}
