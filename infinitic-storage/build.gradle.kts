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
dependencies {
  implementation(Libs.Hoplite.core)

  // Compressor
  implementation(Libs.Compress.commons)

  // Redis
  implementation("redis.clients:jedis:5.0.2")
  //   For integration tests
  testImplementation("org.testcontainers:testcontainers:1.19.1")

  // MySql
  //   For connection pooling
  implementation("com.zaxxer:HikariCP:5.0.1")
  implementation("mysql:mysql-connector-java:8.0.33")
  //   For integration tests
  testImplementation("org.testcontainers:mysql:1.18.3")

  testImplementation(Libs.Hoplite.yaml)
}

apply("../publish.gradle.kts")
