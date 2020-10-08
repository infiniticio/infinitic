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
    id("java-test-fixtures")
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.fasterxml.jackson.core:jackson-databind:2.11.+")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.+")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.11.+")

    implementation(project(":infinitic-avro"))

    testImplementation("org.jeasy:easy-random-core:${project.extra["easyrandom_version"]}")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:${project.extra["kotest_version"]}")
    testImplementation("io.kotest:kotest-property-jvm:${project.extra["kotest_version"]}")
    testImplementation("io.mockk:mockk:${project.extra["mockk_version"]}")

    testFixturesImplementation(project(":infinitic-avro"))
    testFixturesImplementation("org.jeasy:easy-random-core:${project.extra["easyrandom_version"]}")
    testFixturesImplementation("io.kotest:kotest-runner-junit5-jvm:${project.extra["kotest_version"]}")
    testFixturesImplementation("io.kotest:kotest-property-jvm:${project.extra["kotest_version"]}")
    testFixturesImplementation("io.mockk:mockk:${project.extra["mockk_version"]}")
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
    compileTestFixturesKotlin {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
