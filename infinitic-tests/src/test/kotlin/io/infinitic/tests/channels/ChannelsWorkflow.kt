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
package io.infinitic.tests.channels

import com.jayway.jsonpath.Criteria.where
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.utils.Obj
import io.infinitic.utils.Obj1
import io.infinitic.utils.Obj2
import io.infinitic.utils.UtilService
import io.infinitic.workflows.Deferred
import io.infinitic.workflows.SendChannel
import io.infinitic.workflows.Workflow
import io.infinitic.workflows.or
import java.time.Duration
import java.time.Instant

interface ChannelsWorkflow {
  val channelObj: SendChannel<Obj>
  val channelStrA: SendChannel<String>
  val channelStrB: SendChannel<String>

  fun channel1(): String

  fun channel2(): String

  fun channel3(): String

  fun channel4(): Obj

  fun channel4bis(): Obj

  fun channel4ter(): Obj

  fun channel5(): Obj1

  fun channel5bis(): Obj1

  fun channel5ter(): Obj1

  fun channel6(): String

  fun channel6bis(): String

  fun channel6ter(): String

  fun channel7(count: Int, max: Int? = null): String

  fun channel8(): String

  fun channel9(): String
}

@Suppress("unused")
class ChannelsWorkflowImpl : Workflow(), ChannelsWorkflow {

  lateinit var deferred: Deferred<String>

  override val channelObj = channel<Obj>()
  override val channelStrA = channel<String>()
  override val channelStrB = channel<String>()

  private val utilService =
      newService(
          UtilService::class.java,
          tags = setOf("foo", "bar"),
          meta = mutableMapOf("foo" to "bar".toByteArray()),
      )
  private val workflowA = newWorkflow(ChannelsWorkflow::class.java)

  private var p1 = ""

  override fun channel1(): String {
    val deferred: Deferred<String> = channelStrA.receive()

    return deferred.await()
  }

  override fun channel2(): String {
    val deferredSignal = channelStrA.receive()

    val deferredInstant = timer(Duration.ofMillis(1000))

    return when (val r = (deferredSignal or deferredInstant).await()) {
      is String -> r
      is Instant -> "Instant"
      else -> thisShouldNotHappen()
    }
  }

  override fun channel3(): String {
    timer(Duration.ofMillis(1000)).await()
    val deferredSignal = channelStrA.receive()
    val deferredInstant = timer(Duration.ofMillis(100))

    return when (val r = (deferredSignal or deferredInstant).await()) {
      is String -> r
      is Instant -> "Instant"
      else -> thisShouldNotHappen()
    }
  }

  override fun channel4(): Obj {
    val deferred: Deferred<Obj> = channelObj.receive()

    return deferred.await()
  }

  override fun channel4bis(): Obj {
    val deferred: Deferred<Obj> = channelObj.receive(jsonPath = "[?(\$.foo == \"foo\")]")

    return deferred.await()
  }

  override fun channel4ter(): Obj {
    val deferred: Deferred<Obj> =
        channelObj.receive(jsonPath = "[?]", criteria = where("foo").eq("foo"))

    return deferred.await()
  }

  override fun channel5(): Obj1 {
    val deferred: Deferred<Obj1> = channelObj.receive(Obj1::class.java)

    return deferred.await()
  }

  override fun channel5bis(): Obj1 {
    val deferred: Deferred<Obj1> =
        channelObj.receive(Obj1::class.java, jsonPath = "[?(\$.foo == \"foo\")]")

    return deferred.await()
  }

  override fun channel5ter(): Obj1 {
    val deferred: Deferred<Obj1> =
        channelObj.receive(Obj1::class.java, jsonPath = "[?]", criteria = where("foo").eq("foo"))

    return deferred.await()
  }

  override fun channel6(): String {
    val deferred1: Deferred<Obj1> = channelObj.receive(Obj1::class.java)
    val deferred2: Deferred<Obj2> = channelObj.receive(Obj2::class.java)
    val obj1 = deferred1.await()
    val obj2 = deferred2.await()

    return obj1.foo + obj2.foo + obj1.bar * obj2.bar
  }

  override fun channel6bis(): String {
    val deferred1: Deferred<Obj1> =
        channelObj.receive(Obj1::class.java, jsonPath = "[?(\$.foo == \"foo\")]")
    val deferred2: Deferred<Obj2> =
        channelObj.receive(Obj2::class.java, jsonPath = "[?(\$.foo == \"foo\")]")
    val obj1 = deferred1.await()
    val obj2 = deferred2.await()

    return obj1.foo + obj2.foo + obj1.bar * obj2.bar
  }

  override fun channel6ter(): String {
    val deferred1: Deferred<Obj1> =
        channelObj.receive(Obj1::class.java, jsonPath = "[?]", criteria = where("foo").eq("foo"))
    val deferred2: Deferred<Obj2> =
        channelObj.receive(Obj2::class.java, jsonPath = "[?]", criteria = where("foo").eq("foo"))
    val obj1 = deferred1.await()
    val obj2 = deferred2.await()

    return obj1.foo + obj2.foo + obj1.bar * obj2.bar
  }

  override fun channel7(count: Int, max: Int?): String {
    val deferred: Deferred<String> = channelStrA.receive(max)

    var signal = ""

    repeat(count) {
      signal += deferred.await()

      utilService.await(50)
    }

    return signal
  }

  override fun channel8(): String {
    val deferred: Deferred<String> = channelStrA.receive()

    var out = deferred.isCompleted().toString()

    timer(Duration.ofMillis(300)).await()

    out += deferred.isCompleted().toString()

    deferred.await()

    out += deferred.isCompleted().toString()

    return out
  }

  override fun channel9(): String {
    val deferred: Deferred<String> = channelStrA.receive(1)

    repeat(2) {
      val timer = timer(Duration.ofMillis(10))

      or(deferred, timer).await()

      if (deferred.isCompleted()) {
        return "nok"
      }
    }
    return "ok"
  }
}
