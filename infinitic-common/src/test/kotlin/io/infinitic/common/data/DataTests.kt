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
package io.infinitic.common.data

import io.infinitic.common.fixtures.TestFactory
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

class DataTests :
  StringSpec(
      {
        "MessageId should be reversible with ByteArray" {
          val messageId = TestFactory.random<MessageId>()

          MessageId.fromByteArray(messageId.toByteArray()) shouldBe messageId
        }

        "Version should be comparable" {
          Version("1.2.3").shouldBeGreaterThan(Version("1.2.2"))
          Version("1.2.3").shouldBeGreaterThan(Version("1.1.4"))
          Version("1.2.3").shouldBeGreaterThan(Version("0.10.11"))
          Version("1.2.3").shouldBeEqualComparingTo(Version("1.2.3-SNAPSHOT"))
        }
      },
  )
