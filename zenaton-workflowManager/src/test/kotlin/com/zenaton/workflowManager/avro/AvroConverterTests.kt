package com.zenaton.workflowManager.avro

import com.zenaton.workflowManager.TestFactory
import com.zenaton.workflowManager.data.actions.Action
import com.zenaton.workflowManager.data.branches.Branch
import com.zenaton.workflowManager.data.state.getStepId
import com.zenaton.workflowManager.data.steps.StepCriterion
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import kotlin.reflect.KClass

fun getStepId() = TestFactory.get(StepCriterion.Id::class)

class AvroConverterTests : ShouldSpec({
    context("Actions") {
        should("should all be avro-reversible") {
            fun checkAction(klass: KClass<out Action>) {
                if (klass.isSealed) {
                    klass.sealedSubclasses.forEach { checkAction(it) }
                } else {
                    val o1 = TestFactory.get(klass)
                    val o2 = AvroConverter.toAvro(o1)
                    val o3 = AvroConverter.fromAvro(o2)
                    val o4 = AvroConverter.toAvro(o3)
                    o1 shouldBe o3
                    o2 shouldBe o4
                }
            }
            checkAction(Action::class)
        }
    }

    context("StepCriterion") {
        should("A should be avro reversible") {
            val step = getStepId()

            checkStepCriterionReversibility(step)
        }

        should("A AND B should be avro reversible") {
            val stepA = getStepId()
            val stepB = getStepId()
            val step = StepCriterion.And(listOf(stepA, stepB))

            checkStepCriterionReversibility(step)
        }

        should("A OR B should be avro reversible") {
            val stepA = getStepId()
            val stepB = getStepId()
            val step = StepCriterion.Or(listOf(stepA, stepB))

            checkStepCriterionReversibility(step)
        }

        should("A AND (B OR C) should be avro reversible") {
            val stepA = getStepId()
            val stepB = getStepId()
            val stepC = getStepId()
            val step = StepCriterion.And(listOf(stepA, StepCriterion.Or(listOf(stepB, stepC))))

            checkStepCriterionReversibility(step)
        }

        should("A OR (B AND C) should be avro reversible") {
            val stepA = getStepId()
            val stepB = getStepId()
            val stepC = getStepId()
            val step = StepCriterion.Or(listOf(stepA, StepCriterion.And(listOf(stepB, stepC))))

            checkStepCriterionReversibility(step)
        }

        should("A OR (B OR C) should be avro reversible") {
            val stepA = getStepId()
            val stepB = getStepId()
            val stepC = getStepId()
            val step = StepCriterion.Or(listOf(stepA, StepCriterion.Or(listOf(stepB, stepC))))

            checkStepCriterionReversibility(step)
        }

        should("A AND (B AND C) should be avro reversible") {
            val stepA = getStepId()
            val stepB = getStepId()

            val stepC = getStepId()
            val step = StepCriterion.Or(listOf(stepA, StepCriterion.Or(listOf(stepB, stepC))))

            checkStepCriterionReversibility(step)
        }
    }

    context("Branch") {
        should("should be avro-reversible") {
            val o1 = TestFactory.get(Branch::class)
            val o2 = AvroConverter.toAvro(o1)
            val o3 = AvroConverter.fromAvro(o2)
            val o4 = AvroConverter.toAvro(o3)
            o1 shouldBe o3
            o2 shouldBe o4
        }
    }
})

fun checkStepCriterionReversibility(o1: StepCriterion) {
    val o2 = AvroConverter.toAvro(o1)
    val o3 = AvroConverter.fromAvro(o2)
    val o4 = AvroConverter.toAvro(o3)
    o1 shouldBe o3
    o2 shouldBe o4
}
