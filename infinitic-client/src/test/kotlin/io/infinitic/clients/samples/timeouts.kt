package io.infinitic.clients.samples

import io.infinitic.tasks.WithTimeout

class After100MilliSeconds : WithTimeout {
  override fun getTimeoutInSeconds() = 0.1
}

