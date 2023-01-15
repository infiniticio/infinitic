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
package io.infinitic.workflows.workflowTask

import io.infinitic.common.workflows.data.properties.PropertyName
import io.infinitic.common.workflows.data.properties.PropertyValue
import io.infinitic.workflows.workflowTask.workflows.WorkflowAImpl
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

internal class PropertiesTests :
    StringSpec({
      "Make sure to ignore channels, tasks, workflows, logger and property with @Ignore annotation" {
        // given
        val w = WorkflowAImpl()
        // when
        val p: Map<PropertyName, PropertyValue> = w.getProperties()
        // then
        p.size shouldBe 1
        p.keys.first() shouldBe PropertyName("key2")
        p.values.first() shouldBe PropertyValue.from(42)
      }
    })
