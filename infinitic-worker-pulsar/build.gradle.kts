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

plugins {
    `java-library`
    `maven-publish`
    id("com.github.johnrengelman.shadow").version(Libs.shadowVersion)
}

dependencies {
    api(Libs.Coroutines.core)
    api(Libs.Coroutines.jdk8)
    api(Libs.Pulsar.client)
    api(Libs.Pulsar.clientAdmin)
    api(Libs.Pulsar.functions)
    api(Libs.Avro4k.core)
    api(Libs.Hoplite.core)
    api(Libs.Hoplite.yaml)

    api(project(":infinitic-common"))
    api(project(":infinitic-client"))
    api(project(":infinitic-storage"))
    api(project(":infinitic-monitoring-engines"))
    api(project(":infinitic-task-engine"))
    api(project(":infinitic-task-executor"))
    api(project(":infinitic-workflow-engine"))
}

tasks {
    named<ShadowJar>("shadowJar") {
        mergeServiceFiles()
        println("user = ${System.getenv("SONATYPE_NEXUS_USERNAME")}")
        println("user = ${System.getenv("SONATYPE_NEXUS_USERNAME")}")
    }
}

// apply("../publish.gradle.kts")
apply(plugin = "java")
apply(plugin = "java-library")
apply(plugin = "maven-publish")
apply(plugin = "signing")

buildscript {
    repositories {
        jcenter()
        mavenCentral()
        maven(url = uri("https://oss.sonatype.org/content/repositories/snapshots/"))
        maven(url = uri("https://plugins.gradle.org/m2/"))
    }
}

repositories {
    mavenCentral()
}

val signingKey: String? by project
val signingPassword: String? by project

fun Project.publishing(action: PublishingExtension.() -> Unit) = configure(action)

fun Project.signing(configure: SigningExtension.() -> Unit): Unit = configure(configure)

fun Project.java(configure: JavaPluginExtension.() -> Unit): Unit = configure(configure)

val publications: PublicationContainer = (extensions.getByName("publishing") as PublishingExtension).publications

signing {
    useGpgCmd()
    if (signingKey != null && signingPassword != null) {
        @Suppress("UnstableApiUsage")
        useInMemoryPgpKeys(signingKey, signingPassword)
    }
    if (Ci.isRelease) {
        sign(publications)
    }
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    repositories {
        val releasesRepoUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
        val snapshotsRepoUrl = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        maven {
            name = "deploy"
            url = if (Ci.isRelease) releasesRepoUrl else snapshotsRepoUrl
            credentials {
                username = System.getenv("SONATYPE_NEXUS_USERNAME") ?: ""
                password = System.getenv("SONATYPE_NEXUS_PASSWORD") ?: ""
            }
        }
    }

    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
//            artifact(tasks["shadowJar"])
            pom {
                name.set("infinitic")
                description.set("Configuration for Kotlin")
                url.set("https://github.com/infiniticio/infinitic/")

                scm {
                    connection.set("scm:git:https://github.com/infiniticio/infinitic/")
                    developerConnection.set("scm:git:https://github.com/infiniticio/infinitic/")
                    url.set("https://github.com/infiniticio/infinitic/")
                }

                licenses {
                    license {
                        name.set("Commons Clause (MIT License)")
                        url.set("https://commonsclause.com")
                    }
                }

                developers {
                    developer {
                        id.set("geomagilles")
                        name.set("Gilles Barbier")
                        email.set("geomagilles@gmail.com")
                    }
                }
            }
        }
    }
}
