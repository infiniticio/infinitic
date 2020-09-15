package io.infinitic.workflowManager.common.data.steps

import io.infinitic.workflowManager.common.data.instructions.StringPosition

data class NewStep(
    val stepId: StepId = StepId(),
    val step: Step,
    val stepStringPosition: StringPosition,
    val stepHash: StepHash = step.hash()
)
