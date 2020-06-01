package com.zenaton.workflowengine.topics.workflows.state

import com.zenaton.commons.utils.json.Json
import com.zenaton.taskManager.data.JobId
import com.zenaton.workflowengine.topics.workflows.state.StepCriterion.And
import com.zenaton.workflowengine.topics.workflows.state.StepCriterion.Or
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

fun getStep() = StepCriterion.Id(ActionId(JobId()))

class StepCriterionTests : StringSpec({
    "Step should not be completed by default" {
        val step = getStep()

        step.isCompleted() shouldBe false
    }

    "Complete (OR A)" {
        val stepA = getStep()
        val step = Or(listOf(stepA))

        step.isCompleted() shouldBe false
        step.complete(stepA.actionId)
        step.isCompleted() shouldBe true
    }

    "Complete (AND A)" {
        val stepA = getStep()
        val step = And(listOf(stepA))

        step.isCompleted() shouldBe false
        step.complete(stepA.actionId)
        step.isCompleted() shouldBe true
    }

    "Complete (A AND B)" {
        val stepA = getStep()
        val stepB = getStep()
        val step = And(listOf(stepA, stepB))

        step.isCompleted() shouldBe false
        step.complete(stepA.actionId)
        step.isCompleted() shouldBe false
        step.complete(stepB.actionId)
        step.isCompleted() shouldBe true
        step shouldBe And(listOf(stepA, stepB))
    }

    "Complete (A OR B)" {
        val stepA = getStep()
        val stepB = getStep()
        val step = Or(listOf(stepA, stepB))

        step.isCompleted() shouldBe false
        step.complete(stepA.actionId)
        step shouldBe Or(listOf(stepA))
    }

    "Complete (A OR (B OR C))" {
        val stepA = getStep()
        val stepB = getStep()
        val stepC = getStep()
        val step = Or(listOf(stepA, Or(listOf(stepB, stepC))))

        step.isCompleted() shouldBe false
        step.complete(stepB.actionId)
        step shouldBe Or(listOf(stepB))
    }

    "Complete (A AND (B OR C))" {
        val stepA = getStep()
        val stepB = getStep()
        val stepC = getStep()
        val step = And(listOf(stepA, Or(listOf(stepB, stepC))))

        step.isCompleted() shouldBe false
        step.complete(stepA.actionId)
        step.isCompleted() shouldBe false
        step.complete(stepB.actionId)
        step shouldBe And(listOf(stepA, stepB))
    }

    "Complete (A AND (B AND C))" {
        val stepA = getStep()
        val stepB = getStep()
        val stepC = getStep()
        val step = And(listOf(stepA, And(listOf(stepB, stepC))))

        step.isCompleted() shouldBe false
        step.complete(stepA.actionId)
        step.isCompleted() shouldBe false
        step.complete(stepB.actionId)
        step.isCompleted() shouldBe false
        step.complete(stepC.actionId)
        step.isCompleted() shouldBe true
        step shouldBe And(listOf(stepA, stepB, stepC))
    }

    "A OR B resolution" {
        val stepA = getStep()
        val stepB = getStep()
        val step = Or(listOf(stepA, stepB))

        step.complete(stepA.actionId)
        step shouldBe Or(listOf(stepA))
    }

    "A OR (B OR C) resolution" {
        val stepA = getStep()
        val stepB = getStep()
        val stepC = getStep()
        val step = Or(listOf(stepA, Or(listOf(stepB, stepC))))

        step.complete(stepB.actionId)
        step shouldBe Or(listOf(stepB))
    }

    "A OR (B AND C) resolution" {
        val stepA = getStep()
        val stepB = getStep()
        val stepC = getStep()
        val step = Or(listOf(stepA, And(listOf(stepB, stepC))))

        step.complete(stepB.actionId)
        step.complete(stepC.actionId)
        step.isCompleted() shouldBe true
        step shouldBe Or(listOf(And(listOf(stepB, stepC))))
    }

    "A AND (B OR C) resolution" {
        val stepA = getStep()
        val stepB = getStep()
        val stepC = getStep()
        val step = And(listOf(stepA, Or(listOf(stepB, stepC))))

        step.complete(stepB.actionId)
        step shouldBe And(listOf(stepA, stepB))
    }

    "A OR (B AND (C OR D)) resolution" {
        val stepA = getStep()
        val stepB = getStep()
        val stepC = getStep()
        val stepD = getStep()
        val step = Or(listOf(stepA, And(listOf(stepB, Or(listOf(stepC, stepD))))))

        step.complete(stepC.actionId)
        step.complete(stepB.actionId)
        step shouldBe Or(listOf(And(listOf(stepB, stepC))))
    }

    "A serialization" {
        val stepA = getStep()

        stepA shouldBe Json.parse<StepCriterion>(Json.stringify(stepA))
    }

    "A AND B serialization" {
        val stepA = getStep()
        val stepB = getStep()
        val step = And(listOf(stepA, stepB))

        step shouldBe Json.parse<StepCriterion>(Json.stringify(step))
    }

    "A OR B serialization" {
        val stepA = getStep()
        val stepB = getStep()
        val step = Or(listOf(stepA, stepB))

        step shouldBe Json.parse<StepCriterion>(Json.stringify(step))
    }

    "A AND (B OR C) serialization" {
        val stepA = getStep()
        val stepB = getStep()
        val stepC = getStep()
        val step = And(listOf(stepA, Or(listOf(stepB, stepC))))

        step shouldBe Json.parse<StepCriterion>(Json.stringify(step))
    }

    "A OR (B AND C) serialization" {
        val stepA = getStep()
        val stepB = getStep()
        val stepC = getStep()
        val step = Or(listOf(stepA, And(listOf(stepB, stepC))))

        step shouldBe Json.parse<StepCriterion>(Json.stringify(step))
    }

    "A OR (B OR C) serialization" {
        val stepA = getStep()
        val stepB = getStep()
        val stepC = getStep()
        val step = Or(listOf(stepA, Or(listOf(stepB, stepC))))

        step shouldBe Json.parse<StepCriterion>(Json.stringify(step))
    }

    "A AND (B AND C) serialization" {
        val stepA = getStep()
        val stepB = getStep()
        val stepC = getStep()
        val step = Or(listOf(stepA, Or(listOf(stepB, stepC))))

        step shouldBe Json.parse<StepCriterion>(Json.stringify(step))
    }
})
