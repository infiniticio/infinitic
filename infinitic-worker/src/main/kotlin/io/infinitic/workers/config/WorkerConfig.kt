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
package io.infinitic.workers.config

import io.infinitic.cache.config.CacheConfig
import io.infinitic.clients.config.ClientConfig
import io.infinitic.common.config.loadConfigFromFile
import io.infinitic.common.config.loadConfigFromResource
import io.infinitic.storage.config.StorageConfig
import io.infinitic.workers.InfiniticWorker
import io.infinitic.workers.register.InfiniticRegister
import io.infinitic.workers.register.InfiniticRegisterImpl
import io.infinitic.workers.register.config.RegisterConfig

interface WorkerConfig : RegisterConfig, CacheConfig, StorageConfig, ClientConfig {

  /** Infinitic Register */
  val register: InfiniticRegister
    get() = InfiniticRegisterImpl(this)

  /** Infinitic Worker */
  val worker: InfiniticWorker
    get() = InfiniticWorker(register, consumer, producerAsync, client)

  companion object {
    /** Create ClientConfig from file in file system */
    @JvmStatic
    fun fromFile(vararg files: String): WorkerConfig =
        loadConfigFromFile<WorkerConfigData>(files.toList())

    /** Create ClientConfig from file in resources directory */
    @JvmStatic
    fun fromResource(vararg resources: String): WorkerConfig =
        loadConfigFromResource<WorkerConfigData>(resources.toList())
  }
}
