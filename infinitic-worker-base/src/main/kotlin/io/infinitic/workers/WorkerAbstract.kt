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

package io.infinitic.workers

import io.infinitic.common.clients.ClientFactory
import io.infinitic.common.workers.WorkerStarter
import io.infinitic.workers.register.WorkerRegister
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import mu.KotlinLogging
import java.util.concurrent.CompletableFuture

@Suppress("MemberVisibilityCanBePrivate", "unused")
abstract class WorkerAbstract(
    val workerRegister: WorkerRegister
) : WorkerInterface, WorkerRegister by workerRegister {

    protected val logger = KotlinLogging.logger {}
    protected val workerRegistry = workerRegister.registry

    protected abstract val workerName: String
    protected abstract val workerStarter: WorkerStarter
    protected abstract val clientFactory: ClientFactory

    /**
     * Start worker synchronously
     */
    override fun start(): Unit = startAsync().join()

    /**
     * Start worker asynchronously
     */
    abstract override fun startAsync(): CompletableFuture<Unit>

    /**
     * Start worker asynchronously on provided scope
     */
    protected fun CoroutineScope.startWorker(): Job {
        with(workerStarter) {
            workerRegistry.workflows.forEach {
                startWorkflowTaskExecutor(it.key, it.value.concurrency, workerRegistry, clientFactory)
            }

            workerRegistry.workflowTags.forEach {
                startWorkflowTag(it.key, it.value.storage, it.value.concurrency)
            }

            workerRegistry.workflowEngines.forEach {
                startWorkflowEngine(it.key, it.value.storage, it.value.concurrency)
                startWorkflowDelay(it.key, it.value.concurrency)
            }

            workerRegistry.services.forEach {
                startTaskExecutor(it.key, it.value.concurrency, workerRegistry, clientFactory)
            }

            workerRegistry.serviceTags.forEach {
                startTaskTag(it.key, it.value.storage, it.value.concurrency)
            }
        }

        logger.info { "Worker \"$workerName\" ready" }

        return coroutineContext.job
    }
}
