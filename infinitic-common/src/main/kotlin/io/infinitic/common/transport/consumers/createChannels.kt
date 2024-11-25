/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */
package io.infinitic.common.transport.consumers

import io.infinitic.common.transport.interfaces.TransportMessage
import io.infinitic.common.transport.logged.LoggerWithCounter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Creates a new channel
 * that negatively acknowledge the `TransportMessage` object if non-distributed
 */
context(LoggerWithCounter)
fun <T : TransportMessage<M>, M> createChannel(): Channel<Result<T, T>> =
    Channel {
      runBlocking {
        it.message.tryNegativeAcknowledge()
      }
    }

/**
 * Creates a new channel
 * that negatively acknowledge the `TransportMessage` objects if non-distributed
 */
context(LoggerWithCounter)
fun <T : TransportMessage<M>, M> createBatchChannel(): Channel<Result<List<T>, List<T>>> =
    Channel {
      runBlocking {
        it.message.forEach {
          launch { it.tryNegativeAcknowledge() }
        }
      }
    }
