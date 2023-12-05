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

// https://proandroiddev.com/publishing-a-maven-artifact-3-3-step-by-step-instructions-to-mavencentral-publishing-bd661081645d

// To publish a new version:
// * check the new version number CI.BASE
// * run: RELEASE=true ./gradlew publish --rerun-tasks
// * login to https://s01.oss.sonatype.org#stagingRepositories
// * once the new version is uploaded in staging repositories, close it, then release it

apply(plugin = "java")

apply(plugin = "java-library")

apply(plugin = "maven-publish")

apply(plugin = "signing")

buildscript {
  repositories {
    mavenCentral()
    maven(url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
    maven(url = uri("https://plugins.gradle.org/m2/"))
  }
}

repositories { mavenCentral() }

fun Project.publishing(action: PublishingExtension.() -> Unit) = configure(action)

fun Project.signing(configure: SigningExtension.() -> Unit): Unit = configure(configure)

fun Project.java(configure: JavaPluginExtension.() -> Unit): Unit = configure(configure)

val publications: PublicationContainer =
    (extensions.getByName("publishing") as PublishingExtension).publications

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
    val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
    val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    maven {
      name = "deploy"
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
