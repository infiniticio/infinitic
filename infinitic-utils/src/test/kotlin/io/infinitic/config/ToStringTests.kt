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
package io.infinitic.config

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

internal class ToStringTests : StringSpec(
    {
      "Can display non null values" {
        val config = FakeConfig(foo = "abc", bar = 3, qux = 1.0)

        config.notNullPropertiesToString() shouldBe "bar=3, foo=abc, qux=1.0"
      }

      "Can display non null values with interface" {
        val config = FakeConfig(foo = "abc", bar = 3, qux = 1.0)

        config.notNullPropertiesToString(FakeConfigService::class) shouldBe "foo=abc, qux=1.0"
      }
    },
)

private data class FakeConfig(
  override val foo: String? = null,
  val bar: Int? = null,
  val baz: Boolean? = null,
  override val qux: Double? = null,
) : FakeConfigService

private interface FakeConfigService {
  val foo: String?
  val qux: Double?
}
