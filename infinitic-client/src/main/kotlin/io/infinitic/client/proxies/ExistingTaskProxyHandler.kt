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

package io.infinitic.client.proxies

import io.infinitic.client.transport.ClientOutput
import io.infinitic.common.data.methods.MethodOutput
import io.infinitic.common.proxies.MethodProxyHandler
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.common.tasks.engine.messages.CancelTask
import io.infinitic.common.tasks.engine.messages.RetryTask
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future

internal class ExistingTaskProxyHandler<T : Any>(
    private val klass: Class<T>,
    private val id: String,
    private val taskOptions: TaskOptions?,
    private val taskMeta: TaskMeta?,
    private val clientOutput: ClientOutput
) : MethodProxyHandler<T>(klass) {

    /*
     * Cancel a task
     */
    fun cancelTask(output: Any?) {

        val msg = CancelTask(
            taskId = TaskId(id),
            taskOutput = MethodOutput.from(output)
        )
        GlobalScope.future { clientOutput.sendToTaskEngine(msg, 0F) }.join()

        // reset method to allow reuse of the stub
        reset()
    }

    /*
     * Retry a task
     */
    fun retryTask() {
        val msg = RetryTask(
            taskId = TaskId(id),
            taskName = TaskName(klass.name),
            methodName = null,
            methodParameterTypes = null,
            methodInput = null,
            taskOptions = taskOptions,
            taskMeta = taskMeta
        )
        GlobalScope.future { clientOutput.sendToTaskEngine(msg, 0F) }.join()

        // reset method to allow reuse of the stub
        reset()
    }
}
