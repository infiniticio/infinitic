/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

@file:Suppress("ClassName")

package io.infinitic.common.workers.config

import io.infinitic.workflows.Workflow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class WorkflowVersionTests : StringSpec({
    "Default version should be 0" {
        class MyWorkflow : Workflow()

        WorkflowVersion.from(MyWorkflow::class.java) shouldBe WorkflowVersion(0)
    }

    "Get version _" {
        class MyWorkflow_ : Workflow()

        WorkflowVersion.from(MyWorkflow_::class.java) shouldBe WorkflowVersion(0)
    }

    "Get version 1" {
        class MyWorkflow_1 : Workflow()

        WorkflowVersion.from(MyWorkflow_1::class.java) shouldBe WorkflowVersion(1)
    }

    "Get version 42" {
        class MyWorkflow_42 : Workflow()

        WorkflowVersion.from(MyWorkflow_42::class.java) shouldBe WorkflowVersion(42)
    }

    "Get version 0042" {
        class MyWorkflow_0042 : Workflow()

        WorkflowVersion.from(MyWorkflow_0042::class.java) shouldBe WorkflowVersion(42)
    }

    "Get version 01_42" {
        class MyWorkflow_01_42 : Workflow()

        WorkflowVersion.from(MyWorkflow_01_42::class.java) shouldBe WorkflowVersion(42)
    }

    "Get version 42_" {
        class MyWorkflow_42_ : Workflow()

        WorkflowVersion.from(MyWorkflow_42_::class.java) shouldBe WorkflowVersion(0)
    }

    "Get version empty" {
        class _42 : Workflow()

        WorkflowVersion.from(_42::class.java) shouldBe WorkflowVersion(0)
    }

    "Get version 42a" {
        class MyWorkflow_42a : Workflow()

        WorkflowVersion.from(MyWorkflow_42a::class.java) shouldBe WorkflowVersion(0)
    }
})
