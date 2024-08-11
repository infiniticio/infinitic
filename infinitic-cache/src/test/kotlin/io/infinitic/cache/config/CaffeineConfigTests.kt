package io.infinitic.cache.config

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CaffeineConfigTests : StringSpec(
    {
      "Can create CaffeineConfig through builder" {
        val caffeineConfig = CaffeineConfig
            .builder()
            .build()

        caffeineConfig shouldBe CaffeineConfig()
      }
    },
)
