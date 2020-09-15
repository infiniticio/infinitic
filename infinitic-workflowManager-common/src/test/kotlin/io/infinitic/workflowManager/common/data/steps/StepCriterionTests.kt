package io.infinitic.workflowManager.common.data.steps

// fun getStepId() = Step.Id(CommandId(TaskId())) { Status.ONGOING }
//
// class StepCriterionTests : StringSpec({
//    "Step should not be completed by default" {
//        val step = getStepId()
//
//        step.isCompleted() shouldBe false
//    }
//
//    "Complete (OR A)" {
//        val stepA = getStepId()
//        val step = Or(listOf(stepA))
//
//        step.isCompleted() shouldBe false
//        step.complete(stepA.commandId)
//        step.isCompleted() shouldBe true
//    }
//
//    "Complete (AND A)" {
//        val stepA = getStepId()
//        val step = And(listOf(stepA))
//
//        step.isCompleted() shouldBe false
//        step.complete(stepA.commandId)
//        step.isCompleted() shouldBe true
//    }
//
//    "Complete (A AND B)" {
//        val stepA = getStepId()
//        val stepB = getStepId()
//        val step = And(listOf(stepA, stepB))
//
//        step.isCompleted() shouldBe false
//        step.complete(stepA.commandId)
//        step.isCompleted() shouldBe false
//        step.complete(stepB.commandId)
//        step.isCompleted() shouldBe true
//        step shouldBe And(listOf(stepA, stepB))
//    }
//
//    "Complete (A OR B)" {
//        val stepA = getStepId()
//        val stepB = getStepId()
//        val step = Or(listOf(stepA, stepB))
//
//        step.isCompleted() shouldBe false
//        step.complete(stepA.commandId)
//        step shouldBe Or(listOf(stepA))
//    }
//
//    "Complete (A OR (B OR C))" {
//        val stepA = getStepId()
//        val stepB = getStepId()
//        val stepC = getStepId()
//        val step = Or(listOf(stepA, Or(listOf(stepB, stepC))))
//
//        step.isCompleted() shouldBe false
//        step.complete(stepB.commandId)
//        step shouldBe Or(listOf(stepB))
//    }
//
//    "Complete (A AND (B OR C))" {
//        val stepA = getStepId()
//        val stepB = getStepId()
//        val stepC = getStepId()
//        val step = And(listOf(stepA, Or(listOf(stepB, stepC))))
//
//        step.isCompleted() shouldBe false
//        step.complete(stepA.commandId)
//        step.isCompleted() shouldBe false
//        step.complete(stepB.commandId)
//        step shouldBe And(listOf(stepA, stepB))
//    }
//
//    "Complete (A AND (B AND C))" {
//        val stepA = getStepId()
//        val stepB = getStepId()
//        val stepC = getStepId()
//        val step = And(listOf(stepA, And(listOf(stepB, stepC))))
//
//        step.isCompleted() shouldBe false
//        step.complete(stepA.commandId)
//        step.isCompleted() shouldBe false
//        step.complete(stepB.commandId)
//        step.isCompleted() shouldBe false
//        step.complete(stepC.commandId)
//        step.isCompleted() shouldBe true
//        step shouldBe And(listOf(stepA, stepB, stepC))
//    }
//
//    "A OR B resolution" {
//        val stepA = getStepId()
//        val stepB = getStepId()
//        val step = Or(listOf(stepA, stepB))
//
//        step.complete(stepA.commandId)
//        step shouldBe Or(listOf(stepA))
//    }
//
//    "A OR (B OR C) resolution" {
//        val stepA = getStepId()
//        val stepB = getStepId()
//        val stepC = getStepId()
//        val step = Or(listOf(stepA, Or(listOf(stepB, stepC))))
//
//        step.complete(stepB.commandId)
//        step shouldBe Or(listOf(stepB))
//    }
//
//    "A OR (B AND C) resolution" {
//        val stepA = getStepId()
//        val stepB = getStepId()
//        val stepC = getStepId()
//        val step = Or(listOf(stepA, And(listOf(stepB, stepC))))
//
//        step.complete(stepB.commandId)
//        step.complete(stepC.commandId)
//        step.isCompleted() shouldBe true
//        step shouldBe Or(listOf(And(listOf(stepB, stepC))))
//    }
//
//    "A AND (B OR C) resolution" {
//        val stepA = getStepId()
//        val stepB = getStepId()
//        val stepC = getStepId()
//        val step = And(listOf(stepA, Or(listOf(stepB, stepC))))
//
//        step.complete(stepB.commandId)
//        step shouldBe And(listOf(stepA, stepB))
//    }
//
//    "A OR (B AND (C OR D)) resolution" {
//        val stepA = getStepId()
//        val stepB = getStepId()
//        val stepC = getStepId()
//        val stepD = getStepId()
//        val step = Or(listOf(stepA, And(listOf(stepB, Or(listOf(stepC, stepD))))))
//
//        step.complete(stepC.commandId)
//        step.complete(stepB.commandId)
//        step shouldBe Or(listOf(And(listOf(stepB, stepC))))
//    }
// })
