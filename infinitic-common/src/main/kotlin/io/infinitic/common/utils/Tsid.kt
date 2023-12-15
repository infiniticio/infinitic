package io.infinitic.common.utils

import io.hypersistence.tsid.TSID

object Tsid {
  private val TSID_FACTORY = TSID.Factory.builder()
      .withRandomFunction(TSID.Factory.THREAD_LOCAL_RANDOM_FUNCTION)
      .withNodeBits(8)
      .build()

  fun random(): String = TSID_FACTORY.generate().toString()
}

