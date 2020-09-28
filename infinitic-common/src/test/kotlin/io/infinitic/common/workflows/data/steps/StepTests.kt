package io.infinitic.common.workflows.data.steps

import io.infinitic.common.workflows.data.commands.CommandId
import io.infinitic.common.workflows.data.commands.CommandOutput
import io.infinitic.common.workflows.data.commands.CommandStatusCompleted
import io.infinitic.common.workflows.data.commands.CommandStatusOngoing
import io.infinitic.common.workflows.data.steps.Step.Or
import io.infinitic.common.workflows.data.steps.Step.And
import io.infinitic.common.workflows.data.workflows.WorkflowMessageIndex
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

fun getStepId() = Step.Id(CommandId(), CommandStatusOngoing)
fun getCompletedStatus(output: Any? = null, index: Int = 0) = CommandStatusCompleted(
    completionResult = CommandOutput(output),
    completionWorkflowMessageIndex = WorkflowMessageIndex(index)
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
