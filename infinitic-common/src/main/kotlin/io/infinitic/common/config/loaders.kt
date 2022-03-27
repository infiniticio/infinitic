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
package io.infinitic.common.config

import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.PropertySource
import mu.KotlinLogging
import java.io.File

val logger = KotlinLogging.logger("io.infinitic.common.config.loaders")

inline fun <reified T : Any> loadConfigFromResource(resources: List<String>): T {
    val config = ConfigLoader
        .Builder()
        .also { builder -> resources.map { builder.addSource(PropertySource.resource(it, false)) } }
        .build()
        .loadConfigOrThrow<T>()
    logger.info { "Config loaded from resource: $config" }

    return config
}

inline fun <reified T : Any> loadConfigFromFile(files: List<String>): T {
    val config = ConfigLoader
        .Builder()
        .also { builder -> files.map { builder.addSource(PropertySource.file(File(it), false)) } }
        .build()
        .loadConfigOrThrow<T>()
    logger.info { "Config loaded from file: $config" }

    return config
}
