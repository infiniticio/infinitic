package io.infinitic.workflowManager.worker.exceptions

import io.infinitic.workflowManager.common.data.commands.NewCommand

sealed class WorkflowTaskException : RuntimeException()

data class NewStepException(val newCommand: NewCommand) : WorkflowTaskException()

class KnownStepException : WorkflowTaskException()
