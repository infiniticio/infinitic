object Libs {
    const val kotlinVersion = "1.4.21"
    const val serializationVersion = "1.4.10"
    const val ktlintVersion = "9.4.1"
    const val dokkaVersion = "1.4.20"
    const val shadowVersion = "6.1.0"

    const val org = "io.infinitic"

    object Kotlin {
        const val reflect = "org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion"
        const val stdlib = "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion"
    }

    object Coroutines {
        private const val version = "1.4.2"
        const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$version"
        const val jdk8 = "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$version"
    }

    object Serialization {
        const val json = "org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1"
    }

    object Jackson {
        private const val version = "2.12.0"
        const val core = "com.fasterxml.jackson.core:jackson-core:$version"
        const val databind = "com.fasterxml.jackson.core:jackson-databind:$version"
        const val module = "com.fasterxml.jackson.module:jackson-module-kotlin:$version"
        const val jsr310 = "com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$version"
    }

    object Kotest {
        private const val version = "4.3.2"
        const val assertions = "io.kotest:kotest-assertions-core-jvm:$version"
        const val property = "io.kotest:kotest-property-jvm:$version"
        const val junit5 = "io.kotest:kotest-runner-junit5-jvm:$version"
    }

    object Mockk {
        const val mockk = "io.mockk:mockk:1.10.2"
    }

    object Ktlint {
        private const val version = "1.0.1"
    }

    object Avro4k {
        const val core = "com.github.avro-kotlin.avro4k:avro4k-core:1.0.0"
    }

    object Hoplite {
        private const val version = "1.3.11"
        const val core = "com.sksamuel.hoplite:hoplite-core:$version"
        const val yaml = "com.sksamuel.hoplite:hoplite-yaml:$version"
    }

    object Slf4j {
        const val api = "org.slf4j:slf4j-api:1.7.30"
    }

    object Pulsar {
        private const val version = "2.7.0"
        const val client = "org.apache.pulsar:pulsar-client:$version"
        const val clientAdmin = "org.apache.pulsar:pulsar-client-admin:$version"
        const val functions = "org.apache.pulsar:pulsar-functions-api:$version"
    }

    object Redis {
        const val clients = "redis.clients:jedis:3.3.0"
    }

    object EasyRandom {
        const val core = "org.jeasy:easy-random-core:4.2.0"
    }
}
