
plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint") version "9.2.1"
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.apache.avro:avro:1.10.+")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.11.+")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.+")
    implementation("org.slf4j:slf4j-api:1.7.+")

    testImplementation(project(":infinitic-avro"))
    testImplementation(project(":infinitic-messaging-api"))
    testImplementation(project(":infinitic-taskManager-common"))
    testImplementation(project(":infinitic-taskManager-engine"))
    testImplementation(project(":infinitic-taskManager-client"))
    testImplementation(project(":infinitic-taskManager-worker"))
    testImplementation(project(":infinitic-workflowManager-common"))
    testImplementation(project(":infinitic-workflowManager-engine"))
    testImplementation(project(":infinitic-workflowManager-client"))
    testImplementation(project(":infinitic-workflowManager-worker"))

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.8")
    testImplementation("org.jeasy:easy-random-core:4.2.+")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:4.1.+")
    testImplementation("io.kotest:kotest-property-jvm:4.1.+")
    testImplementation("io.kotest:kotest-core-jvm:4.1.+")
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
