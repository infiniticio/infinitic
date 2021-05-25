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

object Libs {
    // Plugins version
    const val kotlinVersion = "1.5.10"
    const val ktlintVersion = "10.0.0"

    const val org = "io.infinitic"

    object Kotlin {
        const val reflect = "org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion"
    }

    object Coroutines {
        private const val version = "1.5.0"
        const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$version"
        const val jdk8 = "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$version"
    }

    object Serialization {
        const val json = "org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.1"
    }

    object JsonPath {
        const val jayway = "com.jayway.jsonpath:json-path:2.5.0"
    }

    object Jackson {
        private const val version = "2.12.3"
        const val core = "com.fasterxml.jackson.core:jackson-core:$version"
        const val databind = "com.fasterxml.jackson.core:jackson-databind:$version"
        const val kotlin = "com.fasterxml.jackson.module:jackson-module-kotlin:$version"
        const val jsr310 = "com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$version"
    }

    object Kotest {
        private const val version = "4.3.2"
        const val property = "io.kotest:kotest-property-jvm:$version"
        const val junit5 = "io.kotest:kotest-runner-junit5-jvm:$version"
    }

    object Mockk {
        const val mockk = "io.mockk:mockk:1.10.6"
    }

    object Avro4k {
        const val core = "com.github.avro-kotlin.avro4k:avro4k-core:1.2.0"
    }

    object Hoplite {
        private const val version = "1.4.0"
        const val core = "com.sksamuel.hoplite:hoplite-core:$version"
        const val yaml = "com.sksamuel.hoplite:hoplite-yaml:$version"
    }

    object Pulsar {
        private const val version = "2.7.2"
        const val client = "org.apache.pulsar:pulsar-client:$version"
        const val clientAdmin = "org.apache.pulsar:pulsar-client-admin:$version"
        const val functions = "org.apache.pulsar:pulsar-functions-api:$version"
    }

    object EasyRandom {
        const val core = "org.jeasy:easy-random-core:4.3.0"
    }

    object Slf4j {
        private const val version = "1.7.30"
        const val simple = "org.slf4j:slf4j-simple:$version"
        const val api = "org.slf4j:slf4j-api:$version"
    }

    object Logging {
        const val api = "io.github.microutils:kotlin-logging:1.12.5"
    }
}
