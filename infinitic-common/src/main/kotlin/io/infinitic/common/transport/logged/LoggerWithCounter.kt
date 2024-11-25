package io.infinitic.common.transport.logged

import io.github.oshai.kotlinlogging.KLogger
import java.util.concurrent.atomic.AtomicLong

class LoggerWithCounter(logger: KLogger) : KLogger by logger {
  private val _received = AtomicLong(0)
  private val _processed = AtomicLong(0)

  fun incr(c: Int = 1) = _received.addAndGet(c.toLong())
  fun decr(c: Int = 1) = _processed.addAndGet(c.toLong())

  val received get() = _received.get()
  val processed get() = _processed.get()
  val remaining get() = received - processed
}
