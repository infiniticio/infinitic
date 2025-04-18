/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, "Sell" means practicing any or all of the rights granted to you
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
dependencies {
  // Cache
  implementation(project(":infinitic-cache"))
  implementation(project(":infinitic-utils"))

  implementation(Libs.Hoplite.core)
  implementation(Libs.Coroutines.core)
  implementation("com.zaxxer:HikariCP:5.0.1")

  // Compressor
  implementation(Libs.Compress.commons)

  // Redis
  implementation("redis.clients:jedis:5.2.0")

  // MySql
  implementation("com.mysql:mysql-connector-j:9.2.0")

  //Postgres
  implementation("org.postgresql:postgresql:42.7.5")

  // Tests
  testImplementation(Libs.TestContainers.testcontainers)
  testImplementation(Libs.TestContainers.mysql)
  testImplementation(Libs.TestContainers.postgresql)
}

apply("../publish.gradle.kts")
