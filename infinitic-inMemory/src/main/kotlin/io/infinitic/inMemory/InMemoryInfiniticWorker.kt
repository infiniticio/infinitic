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

package io.infinitic.inMemory

import io.infinitic.transport.inMemory.InMemoryStarter
import io.infinitic.workers.InfiniticWorker
import io.infinitic.workers.config.WorkerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

@Suppress("MemberVisibilityCanBePrivate")
class InMemoryInfiniticWorker(
    workerConfig: WorkerConfig
) : InfiniticWorker(workerConfig) {

    lateinit var client: InMemoryInfiniticClient

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    public override val workerStarter by lazy {
        InMemoryStarter(scope, name)
    }

    override val clientFactory = { client }

    override val name = workerConfig.name ?: "inMemory"

    override fun start(): Unit = startAsync().join()

    override fun startAsync(): CompletableFuture<Unit> =
        if (this::client.isInitialized) {
            scope.future { startWorker().join() }
        } else {
            logger.info { "Should not start ${this::class} outside of an in-memory client: closing" }
            CompletableFuture.completedFuture(null)
        }

    override fun close() {
        scope.cancel()
    }
}
