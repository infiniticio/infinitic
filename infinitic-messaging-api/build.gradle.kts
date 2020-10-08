// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint") version "9.2.1"

    // Apply the java-library plugin for API and implementation separation.
    `java-library`
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")

    implementation(project(":infinitic-avro"))
    api(project(":infinitic-common"))

    testImplementation("org.jeasy:easy-random-core:4.2.+")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:4.2.+")
    testImplementation("io.kotest:kotest-property-jvm:4.2.+")
    testImplementation("io.mockk:mockk:1.10.+")
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
