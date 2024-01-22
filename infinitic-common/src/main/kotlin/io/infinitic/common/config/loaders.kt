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
package io.infinitic.common.config

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.yaml.YamlPropertySource
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File

val logger = KotlinLogging.logger("io.infinitic.common.config.loaders")

/**
 * Loads a configuration object of type [T] from the given list of resource names.
 *
 * @param resources The list of resource names to be loaded.
 * @return The loaded configuration object.
 */
inline fun <reified T : Any> loadConfigFromResource(vararg resources: String): T {
  val builder = ConfigLoaderBuilder.default()
  resources.map { builder.addSource(PropertySource.resource(it, false)) }
  val config = builder.build().loadConfigOrThrow<T>()
  logger.info { "Config loaded from resource: $config" }

  return config
}

/**
 * Loads a configuration of type [T] from the specified list of files.
 *
 * @param files The list of file paths to load the configuration from.
 * @return The loaded configuration of type [T].
 */
inline fun <reified T : Any> loadConfigFromFile(vararg files: String): T {
  val builder = ConfigLoaderBuilder.default()
  files.map { builder.addSource(PropertySource.file(File(it), false)) }
  val config = builder.build().loadConfigOrThrow<T>()
  logger.info { "Config loaded from file: $config" }

  return config
}

/**
 * Loads a configuration of type [T]  from a YAML string.
 *
 * @param yamls The YAML strings to load the configuration from.
 * @return The loaded configuration object of type T.
 */
inline fun <reified T : Any> loadConfigFromYaml(vararg yamls: String): T {
  val builder = ConfigLoaderBuilder.default()
  yamls.map { builder.addSource(YamlPropertySource(it)) }
  val config = builder.build().loadConfigOrThrow<T>()
  logger.info { "Config loaded from yaml: $config" }

  return config
}
