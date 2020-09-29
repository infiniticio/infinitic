// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.engine.pulsar.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.Message

suspend fun <T> Consumer<T>.receiveSuspend(): Message<T> = suspendCancellableCoroutine { cont ->
    val future = receiveAsync()

    cont.invokeOnCancellation { future.cancel(true) }

    future.whenComplete { message, exception ->
        if (exception == null) {
            cont.resumeWith(Result.success(message))
        } else {
            cont.resumeWith(Result.failure(exception))
        }
    }
}

/**
 * Starts consuming message in a non blocking way and execute
 */
fun <T : Any?> CoroutineScope.startConsumer(consumer: Consumer<T>, block: suspend (message: Message<T>) -> Unit) = launch(Dispatchers.IO) {
    while (isActive) {
        val message = consumer.receiveSuspend()

        try {
            block(message)
            consumer.acknowledge(message)
        } catch (e: Exception) {
            consumer.negativeAcknowledge(message)
        }
    }
}
