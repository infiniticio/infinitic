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

import io.github.oshai.kotlinlogging.KLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * Processes elements received in this channel either individually or in batches,
 * allowing for concurrent processing and returning the results in an output channel.
 *
 * @param concurrency The number of concurrent coroutines that will process the elements. Default is 1.
 * @param singleProcess Function to process a single element.
 * @param batchProcess Function to process a batch of elements.
 * @return A channel where the results of the processed elements will be sent.
 */
context(CoroutineScope, KLogger)
internal fun <M : Any, I, O> Channel<OneOrMany<Result<M, I>>>.batchProcess(
  concurrency: Int = 1,
  singleProcess: suspend (M, I) -> O,
  batchProcess: suspend (List<M>, List<I>) -> List<O>,
): Channel<Result<M, O>> {
  val callingScope: CoroutineScope = this@CoroutineScope
  val outputChannel: Channel<Result<M, O>> = Channel()

  suspend fun process(one: One<Result<M, I>>) {
    val result = one.datum
    if (result.isFailure) {
      outputChannel.send(result.failure())
    }
    if (result.isSuccess) {
      try {
        val o = singleProcess(result.message(), result.value())
        outputChannel.send(result.success(o))
      } catch (e: Exception) {
        outputChannel.send(result.failure(e))
      }
    }
  }

  suspend fun process(many: Many<Result<M, I>>) {
    val results = many.data
    val messages = results.map { it.message() }
    // At this point, all results should be a success
    val values = results.map { it.value() }
    try {
      val output = batchProcess(messages, values)
      messages.zip(output).forEach { (message, value) ->
        outputChannel.send(Result.success(message, value))
      }
    } catch (e: Exception) {
      warn(e) { "batchProcess: exception when batch processing messages: ${messages.map { it.string }}" }
      messages.forEach { message ->
        outputChannel.send(Result.failure(message, e))
      }
    }
  }

  launch {
    withContext(NonCancellable) {
      repeat(concurrency) {
        launch {
          debug { "batchProcess: adding producer $it to ${outputChannel.hashCode()}" }
          outputChannel.addProducer()
          trace { "batchProcess: added producer $it to ${outputChannel.hashCode()}" }
          while (true) {
            try {
              // the only way to quit this loop is to close the input channel
              // which is triggered by canceling the calling scope
              val oneOrMany = receiveIfNotClose().also { trace { "batchProcess: receiving $it" } }
                ?: break
              when (oneOrMany) {
                is One -> process(oneOrMany)
                is Many -> process(oneOrMany)
              }
            } catch (e: Exception) {
              warn(e) { "Exception while batch processing messages" }
              throw e
            } catch (e: Error) {
              warn(e) { "Error when batch processing messages, cancelling calling scope" }
              callingScope.cancel()
            }
          }
          debug { "batchProcess: removing producer $it to ${outputChannel.hashCode()}" }
          outputChannel.removeProducer()
          trace { "batchProcess: removed producer $it to ${outputChannel.hashCode()}" }
        }
      }
    }
  }

  return outputChannel
}

