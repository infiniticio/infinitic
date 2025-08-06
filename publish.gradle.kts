/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */
import okhttp3.RequestBody.Companion.toRequestBody

// https://proandroiddev.com/publishing-a-maven-artifact-3-3-step-by-step-instructions-to-mavencentral-publishing-bd661081645d

// To publish a new version:
// * check the new version number CI.BASE
// * run: RELEASE=true ./gradlew publish --rerun-tasks
// * login to https://central.sonatype.com/publishing/deployments
// * once the new version is uploaded then publish it
//
// curl -u ossSonatypeOrgUsername:ossSonatypeOrgPassword -X POST
// https://ossrh-staging-api.central.sonatype.com/manual/upload/defaultRepository/io.infinitic
//
// You must have a gradle.properties file with
// ossSonatypeOrgUsername=
// ossSonatypeOrgPassword=
// signing.keyId=
// signing.password=
// signing.secretKeyRingFile=/Users/you/.gnupg/secring.gpg
//
// To deploy a snapshot, run: ./gradlew publish --rerun-tasks
// and add:
// Kotlin: maven(url = "https://central.sonatype.com/repository/maven-snapshots/")
// Java: maven { url = 'https://central.sonatype.com/repository/maven-snapshots/' }
// in the repositories section of the gradle.build file

apply(plugin = "java")

apply(plugin = "java-library")

apply(plugin = "maven-publish")

apply(plugin = "signing")

buildscript {
  repositories {
    mavenCentral()
    maven(url = uri("https://central.sonatype.com/repository/maven-snapshots/"))
    maven(url = uri("https://plugins.gradle.org/m2/"))
  }
  dependencies { classpath("com.squareup.okhttp3:okhttp:4.12.0") }
}

repositories { mavenCentral() }

fun Project.publishing(action: PublishingExtension.() -> Unit) = configure(action)

fun Project.signing(configure: SigningExtension.() -> Unit): Unit = configure(configure)

fun Project.java(configure: JavaPluginExtension.() -> Unit): Unit = configure(configure)

val publications = (extensions.getByName("publishing") as PublishingExtension).publications

signing {
  if (Ci.isRelease) {
    sign(publications)
  }
}

java {
  withJavadocJar()
  withSourcesJar()
}

val ossSonatypeOrgUsername: String? by project
val ossSonatypeOrgPassword: String? by project

publishing {
  repositories {
    val releasesRepoUrl =
        uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
    val snapshotsRepoUrl = uri("https://central.sonatype.com/repository/maven-snapshots/")
    maven {
      name = "ossrh-staging-api"
      url = if (Ci.isRelease) releasesRepoUrl else snapshotsRepoUrl
      credentials {
        username = System.getenv("OSSRH_USERNAME") ?: ossSonatypeOrgUsername
        password = System.getenv("OSSRH_PASSWORD") ?: ossSonatypeOrgPassword
      }
    }
  }

  publications {
    register("mavenJava", MavenPublication::class) {
      from(components["java"])
      pom {
        name.set("Infinitic")
        description.set("Infinitic Orchestration Framework")
        url.set("https://infinitic.io")

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
            email.set("gilles@infinitic.io")
          }
        }
      }
    }
  }
}

// === Notify Central Portal ===
val notifyCentralPortal =
    if (rootProject.tasks.findByName("notifyCentralPortal") == null) {
      rootProject.tasks.register("notifyCentralPortal") {
        group = "publishing"
        description = "Notify Central Portal after Maven publish"

        doLast {
          val groupId = "io.infinitic"
          val username = System.getenv("OSSRH_USERNAME") ?: ossSonatypeOrgUsername
          val password = System.getenv("OSSRH_PASSWORD") ?: ossSonatypeOrgPassword

          if (username == null || password == null)
              throw GradleException("Missing OSSRH credentials.")

          val auth = okhttp3.Credentials.basic(username, password)
          val client = okhttp3.OkHttpClient()

          val request =
              okhttp3.Request.Builder()
                  .url(
                      "https://ossrh-staging-api.central.sonatype.com/manual/upload/defaultRepository/$groupId")
                  .post(ByteArray(0).toRequestBody(null))
                  .header("Authorization", auth)
                  .build()

          val response = client.newCall(request).execute()

          if (!response.isSuccessful) {
            throw GradleException(
                "Failed to notify Central Portal: ${response.code} - ${response.body?.string()}")
          } else {
            println("✅ Successfully notified Central Portal for groupId: $groupId")
          }
        }
      }
    } else {
      rootProject.tasks.named("notifyCentralPortal")
    }

// === Hook into publishing (runs only once after all publishing is done) ===

gradle.taskGraph.whenReady {
  if (allTasks.any { it.name == "publish" || it.name == "publishToSonatype" }) {
    notifyCentralPortal.get().mustRunAfter(allTasks.filter { it.name == "publish" })
    gradle.taskGraph.allTasks.lastOrNull()?.finalizedBy(notifyCentralPortal)
  }
}
