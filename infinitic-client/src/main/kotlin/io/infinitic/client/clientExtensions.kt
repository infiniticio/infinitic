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

@file:Suppress("unused")

package io.infinitic.client

import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.common.workflows.data.workflows.WorkflowOptions
import java.util.UUID

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
 * (kotlin) Cancel task per id
 */
inline fun <reified T : Any> InfiniticClient.cancelTask(
    id: UUID
) = cancelTask(T::class.java, id)

/**
 * (kotlin) Cancel task per tag
 */
inline fun <reified T : Any> InfiniticClient.cancelTask(
    tag: String
) = cancelTask(T::class.java, tag)

/**
 * (kotlin) Cancel workflow per id
 */
inline fun <reified T : Any> InfiniticClient.cancelWorkflow(
    id: UUID
) = cancelWorkflow(T::class.java, id)

/**
 * (kotlin) Cancel workflow per tag
 */
inline fun <reified T : Any> InfiniticClient.cancelWorkflow(
    tag: String
) = cancelWorkflow(T::class.java, tag)

/**
 * (kotlin) Complete task per id
 */
inline fun <reified T : Any> InfiniticClient.completeTask(
    id: UUID,
    value: Any
) = completeTask(T::class.java, id, value)

/**
 * (kotlin) Complete task per tag
 */
inline fun <reified T : Any> InfiniticClient.completeTask(
    tag: String,
    value: Any
) = completeTask(T::class.java, tag, value)

/**
 * (kotlin) Complete workflow per id
 */
inline fun <reified T : Any> InfiniticClient.completeWorkflow(
    id: UUID,
    value: Any
) = completeWorkflow(T::class.java, id, value)

/**
 * (kotlin) Complete workflow per tag
 */
inline fun <reified T : Any> InfiniticClient.completeWorkflow(
    tag: String,
    value: Any
) = completeWorkflow(T::class.java, tag, value)

/**
 * (kotlin) Retry task per id
 */
inline fun <reified T : Any> InfiniticClient.retryTask(
    id: UUID
) = retryTask(T::class.java, id)

/**
 * (kotlin) Retry task per tag
 */
inline fun <reified T : Any> InfiniticClient.retryTask(
    tag: String
) = retryTask(T::class.java, tag)

/**
 * (kotlin) Retry workflow per id
 */
inline fun <reified T : Any> InfiniticClient.retryWorkflow(
    id: UUID
) = retryWorkflow(T::class.java, id)

/**
 * (kotlin) Retry workflow per tag
 */
inline fun <reified T : Any> InfiniticClient.retryWorkflow(
    tag: String
) = retryWorkflow(T::class.java, tag)

/**
 * (kotlin) Await task per id
 */
inline fun <reified T : Any> InfiniticClient.awaitTask(
    id: UUID
) = awaitTask(T::class.java, id)

/**
 * (kotlin) Await workflow per id
 */
inline fun <reified T : Any> InfiniticClient.awaitWorkflow(
    id: UUID
) = awaitWorkflow(T::class.java, id)

/**
 * (kotlin) Get ids of running tasks per tag and name
 */
inline fun <reified T : Any> InfiniticClient.getTaskIds(
    tag: String
) = getTaskIds(T::class.java, tag)

/**
 * (kotlin) Get ids of running workflows per tag and name
 */
inline fun <reified T : Any> InfiniticClient.getWorkflowIds(
    tag: String
) = getWorkflowIds(T::class.java, tag)
