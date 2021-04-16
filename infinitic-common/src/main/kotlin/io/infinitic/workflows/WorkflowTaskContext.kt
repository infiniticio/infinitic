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

package io.infinitic.workflows

import com.jayway.jsonpath.Criteria
import io.infinitic.common.proxies.Dispatcher
import io.infinitic.common.proxies.TaskProxyHandler
import io.infinitic.common.proxies.WorkflowProxyHandler
import io.infinitic.common.workflows.data.channels.ChannelImpl
import java.time.Duration
import java.time.Instant

interface WorkflowTaskContext : Dispatcher {

    fun <T : Any, S> async(proxy: T, method: T.() -> S): Deferred<S>

    fun <T> async(branch: () -> T): Deferred<T>

    fun <T> inline(task: () -> T): T

    fun <T> await(deferred: Deferred<T>): T

    fun <T> status(deferred: Deferred<T>): DeferredStatus

    fun <T> dispatchTask(handler: TaskProxyHandler<*>): Deferred<T>

    fun <T> dispatchWorkflow(handler: WorkflowProxyHandler<*>): Deferred<T>

    fun timer(duration: Duration): Deferred<Instant>

    fun timer(instant: Instant): Deferred<Instant>

    fun <T : Any> receiveFromChannel(channel: ChannelImpl<T>, jsonPath: String?, criteria: Criteria?): Deferred<T>

    fun <S : T, T : Any> receiveFromChannel(channel: ChannelImpl<T>, klass: Class<S>, jsonPath: String?, criteria: Criteria?): Deferred<S>

    fun <T : Any> sendToChannel(channel: ChannelImpl<T>, event: T)
}
