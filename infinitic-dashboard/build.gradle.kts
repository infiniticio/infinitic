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

repositories {
    maven("https://jitpack.io")
}

plugins {
    application
    id(Plugins.Shadow.id).version(Plugins.Shadow.version)
}

application {
    mainClass.set("io.infinitic.dashboard.MainKt")
}

dependencies {
    implementation(Libs.Kweb.core)

    implementation(project(":infinitic-pulsar"))
}

tasks.withType<ShadowJar> {
    manifest {
        attributes("Main-Class" to "io.infinitic.dashboard.MainKt")
    }
    // https://stackoverflow.com/questions/48636944/how-to-avoid-a-java-lang-exceptionininitializererror-when-trying-to-run-a-ktor-a
    mergeServiceFiles()
}

apply("../publish.gradle.kts")
