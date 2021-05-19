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

package io.infinitic.common.proxies

import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowOptions
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.exceptions.clients.ChannelUsedOnNewWorkflowException
import io.infinitic.exceptions.clients.MultipleMethodCallsException
import io.infinitic.exceptions.clients.NoMethodCallException
import io.infinitic.workflows.SendChannel
import java.lang.reflect.Method
import kotlin.reflect.full.isSubclassOf

class WorkflowProxyHandler<T : Any>(
    override val klass: Class<T>,
    val workflowTags: Set<WorkflowTag>? = null,
    val workflowOptions: WorkflowOptions? = null,
    val workflowMeta: WorkflowMeta? = null,
    var perWorkflowId: WorkflowId? = null,
    var perTag: WorkflowTag? = null,
    private val dispatcherFn: () -> Dispatcher
) : MethodProxyHandler<T>(klass) {

    val workflowName = WorkflowName(className)

    override val method: Method
        get() {
            if (methods.isEmpty()) {
                throw NoMethodCallException(klass.name)
            }

            val method = methods.last()

            val isChannel = method.returnType.kotlin.isSubclassOf(SendChannel::class)

            if (isChannel && isNew()) {
                throw ChannelUsedOnNewWorkflowException("$workflowName")
            }

            if (!isChannel && methods.size > 1) {
                throw MultipleMethodCallsException(klass.name, methods.first().name, methods.last().name)
            }

            return method
        }

    init {
        require(perWorkflowId == null || perTag == null)
    }

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        val any = super.invoke(proxy, method, args)

        if (method.name == "toString") return any

        return when (isSync) {
            true -> dispatcherFn().dispatchAndWait(this)
            false -> any
        }
    }

    fun isNew() = perWorkflowId == null && perTag == null
}
