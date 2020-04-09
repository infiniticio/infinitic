package com.zenaton.engine.workflows.state

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.UUID

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

    "Step.Or resolution" {
        val step1 = getStep()
        val step2 = getStep()
        var step : Step = Step.Or(listOf(step1, step2))

        step1.completed = true
        step = step.resolve()
        step shouldBe step1
    }

    "Step.Or Or-nested resolution" {
        val step1 = getStep()
        val step2 = getStep()
        val step3 = Step.Or(listOf(step1, step2))
        val step4 = getStep()
        var step : Step = Step.Or(listOf(step4, step3))

        step1.completed = true
        step = step.resolve()
        step shouldBe step1
    }

    "Step.Or And-nested resolution" {
        val step1 = getStep()
        val step2 = getStep()
        val step3 = Step.And(listOf(step1, step2))
        val step4 = getStep()
        var step : Step = Step.Or(listOf(step4, step3))

        step1.completed = true
        step2.completed = true
        step = step.resolve()
        step shouldBe step3
    }

    "Step.And nested resolution" {
        val step1 = getStep()
        val step2 = getStep()
        val step3 = Step.Or(listOf(step1, step2))
        val step4 = getStep()
        var step : Step = Step.And(listOf(step4, step3))

        step1.completed = true
        step = step.resolve()
        (step as Step.And).steps shouldBe listOf(step4, step1)
    }
})
