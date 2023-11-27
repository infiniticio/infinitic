package io.infinitic.tests.errors

import io.infinitic.tasks.WithTimeout

class After500MilliSeconds : WithTimeout {
  override fun getTimeoutInSeconds() = 0.5
}

