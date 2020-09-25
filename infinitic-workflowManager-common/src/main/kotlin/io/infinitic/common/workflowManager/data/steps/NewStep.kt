package io.infinitic.common.workflowManager.data.steps

import io.infinitic.common.workflowManager.data.methodRuns.MethodPosition

data class NewStep(
    val stepId: StepId = StepId(),
    val step: Step,
    val stepPosition: MethodPosition,
    val stepHash: StepHash = step.hash()
)
