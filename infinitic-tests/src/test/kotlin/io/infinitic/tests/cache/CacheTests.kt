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
package io.infinitic.tests.cache

import io.infinitic.Test
import io.infinitic.common.utils.maxCachesSize
import io.infinitic.exceptions.WorkflowUnknownException
import io.infinitic.tests.inline.InlineWorkflow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

internal class CacheTests :
  StringSpec(
      {
        val client = Test.client

        "Cache utilities should not grow while running instances" {

          val inlineWorkflow = client.newWorkflow(InlineWorkflow::class.java)

          // first instance
          inlineWorkflow.inline1(0)

          val size = maxCachesSize

          // running more instances
          (1..20).map { client.dispatch(inlineWorkflow::inline1, it) }.forEach {
            try {
              it.await()
            } catch (e: WorkflowUnknownException) {
              // ignore
            }
          }
          // checking that dispatching more instances does not make the cache grow
          maxCachesSize shouldBe size
        }
      },
  )
