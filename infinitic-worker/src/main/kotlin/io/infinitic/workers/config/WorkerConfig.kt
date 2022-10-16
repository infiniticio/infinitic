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

package io.infinitic.workers.config

import io.infinitic.cache.config.Cache
import io.infinitic.cache.config.CacheConfig
import io.infinitic.common.config.loadConfigFromFile
import io.infinitic.common.config.loadConfigFromResource
import io.infinitic.storage.config.Storage
import io.infinitic.storage.config.StorageConfig
import io.infinitic.transport.config.Transport
import io.infinitic.transport.config.TransportConfig
import io.infinitic.transport.pulsar.config.Pulsar

data class WorkerConfig(
    /**
     * Worker name
     */
    val name: String? = null,

    /**
     * Transport configuration
     */
    override val transport: Transport = Transport.pulsar,

    /**
     * Pulsar configuration
     */
    override val pulsar: Pulsar?,

    /**
     * Default storage
     */
    override val storage: Storage = Storage(),

    /**
     * Default cache
     */
    override var cache: Cache = Cache(),

    /**
     * Workflows configuration
     */
    val workflows: List<Workflow> = listOf(),

    /**
     * Services configuration
     */
    val services: List<Service> = listOf(),

    /**
     * Default service configuration
     */
    val service: ServiceDefault = ServiceDefault(),

    /**
     * Default workflow configuration
     */
    val workflow: WorkflowDefault = WorkflowDefault()

) : CacheConfig, StorageConfig, TransportConfig {

    companion object {
        /**
         * Create WorkerConfig from file in file system
         */
        @JvmStatic
        fun fromFile(vararg files: String): WorkerConfig = loadConfigFromFile(files.toList())

        /**
         * Create WorkerConfig from file in resources directory
         */
        @JvmStatic
        fun fromResource(vararg resources: String): WorkerConfig = loadConfigFromResource(resources.toList())
    }

    init {
        if (transport == Transport.pulsar) {
            require(pulsar != null) { "No `pulsar` configuration provided" }
        }

        // check default service retry Policy
        service.retry?.check()

        // apply default, if not set
        services.map { it ->
            it.concurrency = it.concurrency ?: service.concurrency
            it.retry = it.retry?.also { retry -> retry.check() } ?: service.retry
            it.timeoutInSeconds = it.timeoutInSeconds ?: service.timeoutInSeconds

            it.tagEngine?.let {
                it.storage = it.storage ?: storage
                it.cache = it.cache ?: cache
                if (it.isDefault) it.concurrency = it.concurrency
            }
        }

        // check default service retry Policy
        workflow.retry?.check()

        // apply default, if not set
        workflows.map { it ->
            it.concurrency = it.concurrency ?: workflow.concurrency
            it.retry = it.retry?.also { retry -> retry.check() } ?: workflow.retry
            it.timeoutInSeconds = it.timeoutInSeconds ?: workflow.timeoutInSeconds
            it.checkMode = it.checkMode ?: workflow.checkMode

            it.tagEngine?.let {
                it.storage = it.storage ?: storage
                it.cache = it.cache ?: cache
                if (it.isDefault) it.concurrency = it.concurrency
            }
            it.workflowEngine?.let {
                it.storage = it.storage ?: storage
                it.cache = it.cache ?: cache
                if (it.isDefault) it.concurrency = it.concurrency
            }
        }
    }
}
