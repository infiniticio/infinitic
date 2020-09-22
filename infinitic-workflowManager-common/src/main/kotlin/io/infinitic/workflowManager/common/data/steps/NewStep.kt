package io.infinitic.workflowManager.common.data.steps

import io.infinitic.workflowManager.common.data.methodRuns.MethodPosition

data class NewStep(
    val stepId: StepId = StepId(),
    val step: Step,
    val stepMethodPosition: MethodPosition,
    val stepHash: StepHash = step.hash()
)
