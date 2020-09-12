package io.infinitic.workflowManager.worker.exceptions

import io.infinitic.workflowManager.common.data.commands.NewCommand

sealed class WorkflowTaskException : RuntimeException()

class NewStepException() : WorkflowTaskException()

class KnownStepException : WorkflowTaskException()
