package io.infinitic.worker.workflowTask

sealed class WorkflowTaskException : RuntimeException()

class NewStepException() : WorkflowTaskException()

class KnownStepException : WorkflowTaskException()
