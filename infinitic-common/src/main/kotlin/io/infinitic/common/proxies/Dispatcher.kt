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

import io.infinitic.exceptions.clients.SuspendMethodNotSupportedException
import java.lang.reflect.Method
import java.util.concurrent.CompletableFuture
import kotlin.reflect.jvm.kotlinFunction

interface Dispatcher {
    fun <S> dispatchAndWait(handler: TaskProxyHandler<*>): S

    fun <S> dispatchAndWait(handler: WorkflowProxyHandler<*>): S

    fun dispatchAndWait(handler: SendChannelProxyHandler<*>): CompletableFuture<Unit>

    fun checkMethodIsNotSuspend(method: Method) {
        if (method.kotlinFunction?.isSuspend == true) throw SuspendMethodNotSupportedException(method.declaringClass.name, method.name)
    }
}
