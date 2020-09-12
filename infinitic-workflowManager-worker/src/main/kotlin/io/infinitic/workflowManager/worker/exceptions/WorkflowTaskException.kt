package io.infinitic.workflowManager.worker.exceptions

sealed class WorkflowTaskException : RuntimeException()

class NewStepException() : WorkflowTaskException()

class KnownStepException : WorkflowTaskException()
