package io.infinitic.common.workflows.data.steps

import io.infinitic.common.workflows.data.methodRuns.MethodPosition

data class NewStep(
    val stepId: StepId = StepId(),
    val step: Step,
    val stepPosition: MethodPosition,
    val stepHash: StepHash = step.hash()
)
