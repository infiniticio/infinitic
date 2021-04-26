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

package io.infinitic.clients

import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.common.workflows.data.workflows.WorkflowOptions
import java.util.UUID

interface InfiniticClient {
    /**
     * Close client
     */
    fun close()

    /**
     * Create stub for a new task
     */
    fun <T : Any> newTask(
        klass: Class<out T>,
        tags: Set<String> = setOf(),
        options: TaskOptions? = null,
        meta: Map<String, ByteArray> = mapOf()
    ): T

    fun <T : Any> newTask(
        klass: Class<out T>,
        tags: Set<String> = setOf(),
        options: TaskOptions? = null,
    ): T = newTask(klass, tags, options, mapOf())

    fun <T : Any> newTask(
        klass: Class<out T>,
        tags: Set<String> = setOf()
    ): T = newTask(klass, tags, null, mapOf())

    fun <T : Any> newTask(
        klass: Class<out T>
    ): T = newTask(klass, setOf(), null, mapOf())

    /**
     * Create stub for a new workflow
     */
    fun <T : Any> newWorkflow(
        klass: Class<out T>,
        tags: Set<String> = setOf(),
        options: WorkflowOptions? = null,
        meta: Map<String, ByteArray> = mapOf()
    ): T

    fun <T : Any> newWorkflow(
        klass: Class<out T>,
        tags: Set<String> = setOf(),
        options: WorkflowOptions? = null
    ): T = newWorkflow(klass, tags, options, mapOf())

    fun <T : Any> newWorkflow(
        klass: Class<out T>,
        tags: Set<String> = setOf(),
    ): T = newWorkflow(klass, tags, null, mapOf())

    fun <T : Any> newWorkflow(
        klass: Class<out T>,
    ): T = newWorkflow(klass, setOf(), null, mapOf())

    /**
     * Create stub for an existing task targeted per id
     */
    fun <T : Any> getTask(
        klass: Class<out T>,
        id: UUID
    ): T

    /**
     * Create stub for an existing task targeted per tag
     */
    fun <T : Any> getTask(
        klass: Class<out T>,
        tag: String
    ): T

    /**
     * Create stub for an existing workflow targeted per id
     */
    fun <T : Any> getWorkflow(
        klass: Class<out T>,
        id: UUID
    ): T

    /**
     * Create stub for an existing workflow targeted per tag
     */
    fun <T : Any> getWorkflow(
        klass: Class<out T>,
        tag: String
    ): T

    /**
     *  Asynchronously process a task or a workflow
     */
    fun <T : Any, S> async(proxy: T, method: T.() -> S): Deferred<S>

    /**
     *  Cancel a task or a workflow from a stub
     */
    fun <T : Any> cancel(proxy: T)

    /**
     *  Cancel a task by id
     */
    fun <T : Any> cancelTask(
        klass: Class<out T>,
        id: UUID
    ) = cancel(getTask(klass, id))

    /**
     *  Cancel a task by tag
     */
    fun <T : Any> cancelTask(
        klass: Class<out T>,
        tag: String
    ) = cancel(getTask(klass, tag))

    /**
     *  Cancel a workflow by id
     */
    fun <T : Any> cancelWorkflow(
        klass: Class<out T>,
        id: UUID
    ) = cancel(getWorkflow(klass, id))

    /**
     *  Cancel a workflow by tag
     */
    fun <T : Any> cancelWorkflow(
        klass: Class<out T>,
        tag: String
    ) = cancel(getWorkflow(klass, tag))

    /**
     * Retry a task or a workflowTask from a stub
     */
    fun <T : Any> retry(proxy: T)

    /**
     * Retry a task by id
     */
    fun <T : Any> retryTask(
        klass: Class<out T>,
        id: UUID
    ) = retry(getTask(klass, id))

    /**
     * Retry a task by tag
     */
    fun <T : Any> retryTask(
        klass: Class<out T>,
        tag: String
    ) = retry(getTask(klass, tag))

    /**
     * Retry a workflow by id
     */
    fun <T : Any> retryWorkflow(
        klass: Class<out T>,
        id: UUID
    ) = retry(getWorkflow(klass, id))

    /**
     * Retry a workflow by tag
     */
    fun <T : Any> retryWorkflow(
        klass: Class<out T>,
        tag: String
    ) = retry(getWorkflow(klass, tag))
}

/**
 * (Kotlin) Create stub for a new task
 */
inline fun <reified T : Any> InfiniticClient.newTask(
    tags: Set<String> = setOf(),
    options: TaskOptions? = null,
    meta: Map<String, ByteArray> = mapOf()
): T = newTask(T::class.java, tags, options, meta)

/**
 * (Kotlin) Create stub for a new workflow
 */
inline fun <reified T : Any> InfiniticClient.newWorkflow(
    tags: Set<String> = setOf(),
    options: WorkflowOptions? = null,
    meta: Map<String, ByteArray> = mapOf()
): T = newWorkflow(T::class.java, tags, options, meta)

/**
 * (Kotlin) Create stub for an existing task targeted per id
 */
inline fun <reified T : Any> InfiniticClient.getTask(
    id: UUID
): T = getTask(T::class.java, id)

/**
 * (Kotlin) Create stub for an existing task targeted per tag
 */
inline fun <reified T : Any> InfiniticClient.getTask(
    tag: String
): T = getTask(T::class.java, tag)

/**
 * (kotlin) Create stub for an existing workflow per id
 */
inline fun <reified T : Any> InfiniticClient.getWorkflow(
    id: UUID
): T = getWorkflow(T::class.java, id)

/**
 * (kotlin) Create stub for an existing workflow per tag
 */
inline fun <reified T : Any> InfiniticClient.getWorkflow(
    tag: String
): T = getWorkflow(T::class.java, tag)

/**
 * (kotlin) Cancel task by id
 */
inline fun <reified T : Any> InfiniticClient.cancelTask(
    id: UUID
) = cancelTask(T::class.java, id)

/**
 * (kotlin) Cancel task by tag
 */
inline fun <reified T : Any> InfiniticClient.cancelTask(
    tag: String
) = cancelTask(T::class.java, tag)

/**
 * (kotlin) Cancel workflow by id
 */
inline fun <reified T : Any> InfiniticClient.cancelWorkflow(
    id: UUID
) = cancelWorkflow(T::class.java, id)

/**
 * (kotlin) Cancel workflow by tag
 */
inline fun <reified T : Any> InfiniticClient.cancelWorkflow(
    tag: String
) = cancelWorkflow(T::class.java, tag)

/**
 * (kotlin) Retry task by id
 */
inline fun <reified T : Any> InfiniticClient.retryTask(
    id: UUID
) = retryTask(T::class.java, id)

/**
 * (kotlin) Retry task by tag
 */
inline fun <reified T : Any> InfiniticClient.retryTask(
    tag: String
) = retryTask(T::class.java, tag)

/**
 * (kotlin) Retry workflow by id
 */
inline fun <reified T : Any> InfiniticClient.retryWorkflow(
    id: UUID
) = retryWorkflow(T::class.java, id)

/**
 * (kotlin) Retry workflow by tag
 */
inline fun <reified T : Any> InfiniticClient.retryWorkflow(
    tag: String
) = retryWorkflow(T::class.java, tag)
