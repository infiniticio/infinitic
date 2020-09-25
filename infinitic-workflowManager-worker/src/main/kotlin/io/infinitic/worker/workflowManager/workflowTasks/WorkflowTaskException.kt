package io.infinitic.worker.workflowManager.workflowTasks

sealed class WorkflowTaskException : RuntimeException()

class NewStepException() : WorkflowTaskException()

class KnownStepException : WorkflowTaskException()
