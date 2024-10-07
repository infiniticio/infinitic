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
package io.infinitic.common.transport.consumer

class Result<M : Any, out T> internal constructor(
  private val message: M,
  private val value: Any?
) {

  val isSuccess: Boolean get() = value !is Failure

  val isFailure: Boolean get() = value is Failure

  fun message(): M = message

  fun value(): T {
    require(isSuccess) { "This result is a failure" }
    @Suppress("UNCHECKED_CAST")
    return value as T
  }

  fun exception(): Exception {
    require(isFailure) { "This result is a success" }
    return (value as Failure).exception
  }

  suspend fun onFailure(action: suspend (exception: Exception) -> Unit): Result<M, T> {
    if (isFailure) action((value as Failure).exception)
    return this
  }

  suspend fun onSuccess(action: suspend (value: T) -> Unit): Result<M, T> {
    @Suppress("UNCHECKED_CAST")
    if (isSuccess) action(value as T)
    return this
  }

  override fun toString(): String =
      when (value) {
        is Failure -> "Failure($message, $value)"
        else -> "Success($message, $value)"
      }

  fun <S> success(value: S): Result<M, S> = Result(message, value)

  fun <S> failure(exception: Exception): Result<M, S> = Result(message, Failure(exception))

  fun <S> failure(): Result<M, S> = Result(message, Failure(exception()))

  companion object {
    /**
     * Returns an instance that encapsulates the given [value] as successful value.
     */
    fun <M : Any, T> success(message: M, value: T): Result<M, T> =
        Result(message, value)

    /**
     * Returns an instance that encapsulates the given [Exception] [exception] as failure.
     */
    fun <M : Any, T> failure(message: M, exception: Exception): Result<M, T> =
        Result(message, Failure(exception))
  }

  private class Failure(val exception: Exception) {
    override fun equals(other: Any?): Boolean = other is Failure && exception == other.exception
    override fun hashCode(): Int = exception.hashCode()
    override fun toString(): String = "$exception"
  }
}

