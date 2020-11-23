// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.common.workflows.data.steps

import io.infinitic.common.workflows.data.commands.CommandId
import io.infinitic.common.workflows.data.commands.CommandOutput
import io.infinitic.common.workflows.data.commands.CommandStatusCompleted
import io.infinitic.common.workflows.data.commands.CommandStatusOngoing
import io.infinitic.common.workflows.data.steps.Step.And
import io.infinitic.common.workflows.data.steps.Step.Or
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskIndex
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

fun getStepId() = Step.Id(CommandId(), CommandStatusOngoing)

fun getCompletedStatus(output: Any? = null, index: Int = 0) = CommandStatusCompleted(
    completionResult = CommandOutput.from(output),
    completionWorkflowTaskIndex = WorkflowTaskIndex(index)
)

class StepTests : StringSpec({
    "Step should not be terminated by default" {
        val step = getStepId()

        step.isTerminated() shouldBe false
    }

    "Complete (OR A)" {
        val stepA = getStepId()
        val step = Or(listOf(stepA))

        step.isTerminated() shouldBe false
        step.update(stepA.commandId, getCompletedStatus())
        step.isTerminated() shouldBe true
    }

    "Complete (AND A)" {
        val stepA = getStepId()
        val step = And(listOf(stepA))

        step.isTerminated() shouldBe false
        step.update(stepA.commandId, getCompletedStatus())
        step.isTerminated() shouldBe true
    }

    "Complete (A AND B)" {
        val stepA = getStepId()
        val stepB = getStepId()
        val step = And(listOf(stepA, stepB))

        step.isTerminated() shouldBe false
        step.update(stepA.commandId, getCompletedStatus())
        step.isTerminated() shouldBe false
        step.update(stepB.commandId, getCompletedStatus())
        step.isTerminated() shouldBe true
        step shouldBe And(listOf(stepA, stepB))
    }

    "Complete (A OR B)" {
        val stepA = getStepId()
        val stepB = getStepId()
        val step = Or(listOf(stepA, stepB))

        step.isTerminated() shouldBe false
        step.update(stepA.commandId, getCompletedStatus())
        step shouldBe Or(listOf(stepA))
    }

    "Complete (A OR (B OR C))" {
        val stepA = getStepId()
        val stepB = getStepId()
        val stepC = getStepId()
        val step = Or(listOf(stepA, Or(listOf(stepB, stepC))))

        step.isTerminated() shouldBe false
        step.update(stepB.commandId, getCompletedStatus())
        step shouldBe Or(listOf(stepB))
    }

    "Complete (A AND (B OR C))" {
        val stepA = getStepId()
        val stepB = getStepId()
        val stepC = getStepId()
        val step = And(listOf(stepA, Or(listOf(stepB, stepC))))

        step.isTerminated() shouldBe false
        step.update(stepA.commandId, getCompletedStatus())
        step.isTerminated() shouldBe false
        step.update(stepB.commandId, getCompletedStatus())
        step shouldBe And(listOf(stepA, stepB))
    }

    "Complete (A AND (B AND C))" {
        val stepA = getStepId()
        val stepB = getStepId()
        val stepC = getStepId()
        val step = And(listOf(stepA, And(listOf(stepB, stepC))))

        step.isTerminated() shouldBe false
        step.update(stepA.commandId, getCompletedStatus())
        step.isTerminated() shouldBe false
        step.update(stepB.commandId, getCompletedStatus())
        step.isTerminated() shouldBe false
        step.update(stepC.commandId, getCompletedStatus())
        step.isTerminated() shouldBe true
        step shouldBe And(listOf(stepA, stepB, stepC))
    }

    "A OR B resolution" {
        val stepA = getStepId()
        val stepB = getStepId()
        val step = Or(listOf(stepA, stepB))

        step.update(stepA.commandId, getCompletedStatus())
        step shouldBe Or(listOf(stepA))
    }

    "A OR (B OR C) resolution" {
        val stepA = getStepId()
        val stepB = getStepId()
        val stepC = getStepId()
        val step = Or(listOf(stepA, Or(listOf(stepB, stepC))))

        step.update(stepB.commandId, getCompletedStatus())
        step shouldBe Or(listOf(stepB))
    }

    "A OR (B AND C) resolution" {
        val stepA = getStepId()
        val stepB = getStepId()
        val stepC = getStepId()
        val step = Or(listOf(stepA, And(listOf(stepB, stepC))))

        step.update(stepB.commandId, getCompletedStatus())
        step.update(stepC.commandId, getCompletedStatus())
        step.isTerminated() shouldBe true
        step shouldBe Or(listOf(And(listOf(stepB, stepC))))
    }

    "A AND (B OR C) resolution" {
        val stepA = getStepId()
        val stepB = getStepId()
        val stepC = getStepId()
        val step = And(listOf(stepA, Or(listOf(stepB, stepC))))

        step.update(stepB.commandId, getCompletedStatus())
        step shouldBe And(listOf(stepA, stepB))
    }

    "A OR (B AND (C OR D)) resolution" {
        val stepA = getStepId()
        val stepB = getStepId()
        val stepC = getStepId()
        val stepD = getStepId()
        val step = Or(listOf(stepA, And(listOf(stepB, Or(listOf(stepC, stepD))))))

        step.update(stepC.commandId, getCompletedStatus())
        step.update(stepB.commandId, getCompletedStatus())
        step shouldBe Or(listOf(And(listOf(stepB, stepC))))
    }
})
