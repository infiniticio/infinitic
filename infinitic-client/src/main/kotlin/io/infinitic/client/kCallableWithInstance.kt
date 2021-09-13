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

package io.infinitic.client

import io.infinitic.exceptions.thisShouldNotHappen
import kotlin.reflect.KCallable
import kotlin.reflect.KParameter
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

internal class KCallableWithInstance<out R>(private val func: KCallable<R>, private val instance: Any) : KCallable<R> by func {
    private val instanceParam = func.instanceParameter
        ?: func.extensionReceiverParameter
        ?: throw thisShouldNotHappen("Given function must not have a instance already bound")

    init {
        val instanceParamType = instanceParam.type.jvmErasure
        if (!instance::class.isSubclassOf(instanceParamType))
            throw thisShouldNotHappen(
                "Provided instance (${instance::class.qualifiedName}) isn't an subclass of " +
                    "instance param's value's class (${instanceParamType::class.qualifiedName})"
            )
    }

    override fun call(vararg args: Any?): R = func.call(instance, *args)

    override fun callBy(args: Map<KParameter, Any?>): R = func.callBy(args + (instanceParam to instance))

    override val parameters = func.parameters.filter { it != instanceParam }
}

internal fun <R> KCallable<R>.withInstance(instance: Any): KCallable<R> = KCallableWithInstance(this, instance)
