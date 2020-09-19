package io.infinitic.workflowManager.worker.workflowTasks

sealed class WorkflowTaskException : RuntimeException()

class NewStepException() : WorkflowTaskException()

class KnownStepException : WorkflowTaskException()
