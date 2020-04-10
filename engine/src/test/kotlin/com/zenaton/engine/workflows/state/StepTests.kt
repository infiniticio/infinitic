package com.zenaton.engine.workflows.state

// import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
// import io.kotest.matchers.shouldNotBe

fun getStep() = Step.Id(UnitStepId())

class StepTests : StringSpec({
    "Step.Id by default is not completed " {
        val step1 = getStep()

        step1.isCompleted() shouldBe false
    }

    "Step.And will be completed only if all elements are completed" {
        val step1 = getStep()
        val step2 = getStep()
        val step3 = getStep()
        val step4 = Step.And(listOf(step1, step2, step3))

        step4.isCompleted() shouldBe false
        step1.completed = true
        step4.isCompleted() shouldBe false
        step2.completed = true
        step4.isCompleted() shouldBe false
        step3.completed = true
        step4.isCompleted() shouldBe true
    }

    "Step.Or will be completed if at least 1 elements is completed" {
        val step1 = getStep()
        val step2 = getStep()
        val step3 = getStep()
        val step4 = Step.Or(listOf(step1, step2, step3))

        step4.isCompleted() shouldBe false
        step1.completed = true
        step4.isCompleted() shouldBe true
        step2.completed = true
        step4.isCompleted() shouldBe true
        step3.completed = true
        step4.isCompleted() shouldBe true
    }

    "Step.Or with nested step" {
        val step1 = getStep()
        val step2 = getStep()
        val step3 = Step.Or(listOf(step1, step2))
        val step4 = getStep()
        val step5 = Step.Or(listOf(step3, step4))

        step5.isCompleted() shouldBe false
        step1.completed = true
        step5.isCompleted() shouldBe true
    }

    "Step.And with nested step" {
        val step1 = getStep()
        val step2 = getStep()
        val step3 = Step.Or(listOf(step1, step2))
        val step4 = getStep()
        val step5 = Step.And(listOf(step3, step4))

        step5.isCompleted() shouldBe false
        step1.completed = true
        step5.isCompleted() shouldBe false
        step4.completed = true
        step5.isCompleted() shouldBe true
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
