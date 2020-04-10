package com.zenaton.engine.workflows.state

// import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
// import io.kotest.matchers.shouldNotBe

fun getStep() = Step.Id(UnitStepId())

class StepTests : StringSpec({
    "Step is not completed by default" {
        val step = getStep()

        step.isCompleted() shouldBe false
    }

    "Condition to complete (A AND B AND C)" {
        val stepA = getStep()
        val stepB = getStep()
        val stepC = getStep()
        val step = Step.And(listOf(stepA, stepB, stepC))

        step.isCompleted() shouldBe false
        stepA.completed = true
        step.isCompleted() shouldBe false
        stepB.completed = true
        step.isCompleted() shouldBe false
        stepC.completed = true
        step.isCompleted() shouldBe true
    }

    "Condition to complete (A OR B OR C)" {
        val stepA = getStep()
        val stepB = getStep()
        val stepC = getStep()
        val step = Step.Or(listOf(stepA, stepB, stepC))

        step.isCompleted() shouldBe false
        stepA.completed = true
        step.isCompleted() shouldBe true
        stepB.completed = true
        step.isCompleted() shouldBe true
        stepC.completed = true
        step.isCompleted() shouldBe true
    }

    "Condition to complete (A OR (B OR C))" {
        val stepA = getStep()
        val stepB = getStep()
        val stepC = getStep()
        val step = Step.Or(listOf(stepA, Step.Or(listOf(stepB, stepC))))

        step.isCompleted() shouldBe false
        stepB.completed = true
        step.isCompleted() shouldBe true
    }

    "Condition to complete (A AND (B OR C))"  {
        val stepA = getStep()
        val stepB = getStep()
        val stepC = getStep()
        val step = Step.And(listOf(stepA, Step.Or(listOf(stepB, stepC))))

        step.isCompleted() shouldBe false
        stepA.completed = true
        step.isCompleted() shouldBe false
        stepB.completed = true
        step.isCompleted() shouldBe true
    }

    "A OR B resolution" {
        val stepA = getStep()
        val stepB = getStep()
        var step: Step = Step.Or(listOf(stepA, stepB))

        stepA.completed = true
        step = step.resolve()
        step shouldBe stepA
    }

    "A OR (B OR C) resolution" {
        val stepA = getStep()
        val stepB = getStep()
        val stepC = getStep()
        var step: Step = Step.Or(listOf(stepA, Step.Or(listOf(stepB, stepC))))

        stepB.completed = true
        step = step.resolve()
        step shouldBe stepB
    }

    "A OR (B AND C) resolution" {
        val stepA = getStep()
        val stepB = getStep()
        val stepC = getStep()
        var step: Step = Step.Or(listOf(stepA, Step.And(listOf(stepB, stepC))))

        stepB.completed = true
        step = step.resolve()
        stepC.completed = true
        step = step.resolve()
        step shouldBe Step.And(listOf(stepB, stepC))
    }

    "A AND (B OR C) resolution" {
        val stepA = getStep()
        val stepB = getStep()
        val stepC = getStep()
        var step: Step = Step.And(listOf(stepA, Step.Or(listOf(stepB, stepC))))

        stepB.completed = true
        step = step.resolve()
        step shouldBe Step.And(listOf(stepA, stepB))
    }

    "A OR (B AND (C OR D)) resolution" {
        val stepA = getStep()
        val stepB = getStep()
        val stepC = getStep()
        val stepD = getStep()
        var step: Step = Step.Or(listOf(stepA, Step.And(listOf(stepB, Step.Or(listOf(stepC, stepD))))))

        stepC.completed = true
        step = step.resolve()
        stepB.completed = true
        step = step.resolve()
        step shouldBe Step.And(listOf(stepB, stepC))
    }
})
