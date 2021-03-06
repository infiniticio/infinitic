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

buildscript {
    repositories {
        gradlePluginPortal()
        maven(url = "https://dl.bintray.com/gradle/gradle-plugins")
    }
}

plugins {
    kotlin("jvm").version(Libs.kotlinVersion) apply false
    kotlin("plugin.serialization").version(Libs.serializationVersion) apply false
    id("org.jlleitschuh.gradle.ktlint").version(Libs.ktlintVersion) apply false
}

subprojects {
    repositories {
        mavenCentral()
    }

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    group = Libs.org
    version = Ci.version

    dependencies {
        "implementation"(platform("org.jetbrains.kotlin:kotlin-bom"))
        "implementation"(Libs.Slf4j.api)

        "testImplementation"(Libs.Slf4j.simple)
        "testImplementation"(Libs.Kotest.junit5)
        "testImplementation"(Libs.Kotest.property)
        "testImplementation"(Libs.Mockk.mockk)

        if (name != "infinitic-common") {
            "testImplementation"(testFixtures(project(":infinitic-common")))
        }
    }

    if (name != "infinitic-rest-api") {
        tasks.withType<Test> {
            useJUnitPlatform()
        }

        // For Kotlin sources
        tasks.withType<KotlinCompile> {
            kotlinOptions {
                freeCompilerArgs += "-Xjvm-default=enable"
                jvmTarget = JavaVersion.VERSION_1_8.toString()
            }
        }
    }

    // For Java sources
    tasks.withType<JavaCompile> {
        sourceCompatibility = JavaVersion.VERSION_1_8.toString()
        targetCompatibility = JavaVersion.VERSION_1_8.toString()
    }
}
