package io.infinitic.workflowManager.engine.engines

import io.infinitic.workflowManager.common.data.methodRuns.MethodRun

class MethodRunEngine(val branch: MethodRun) {
//    fun completeTask(taskId: TaskId, taskOutput: TaskOutput, properties: Properties): Boolean {
//        // complete action if relevant
//        branch.pastInstructionsInMethod
//            .filterIsInstance<TaskDispatched>()
//            .firstOrNull { a: TaskDispatched -> a.taskId == taskId && a.commandStatus != CommandStatus.COMPLETED }
//            ?.apply {
//                this.taskOutput = taskOutput
//                commandStatus = CommandStatus.COMPLETED
//            }
//
//        // does this task complete the current step?
//        return lastStep().completeTask(taskId, properties)
//    }
//
//    fun completeChildWorkflow(childWorkflowId: WorkflowId, childOutput: MethodOutput, properties: Properties): Boolean {
//        // complete action if relevant
//        branch.pastInstructions
//            .filterIsInstance<ChildWorkflowDispatched>()
//            .firstOrNull { a -> a.childWorkflowId == childWorkflowId && a.commandStatus != CommandStatus.COMPLETED }
//            ?.apply {
//                this.childWorkflowOutput = childOutput
//                commandStatus = CommandStatus.COMPLETED
//            }
//
//        // does this task complete the current step?
//        return lastStep().completeChildWorkflow(childWorkflowId, properties)
//    }
//
//    fun completeDelay(delayId: DelayId, properties: Properties): Boolean {
//        // complete action if relevant
//        branch.pastInstructions
//            .filterIsInstance<DelayWaited>()
//            .firstOrNull { a -> a.delayId == delayId && a.commandStatus != CommandStatus.COMPLETED }
//            ?.apply {
//                commandStatus = CommandStatus.COMPLETED
//            }
//
//        // does this task complete the current step?
//        return lastStep().completeDelay(delayId, properties)
//    }
//
//    fun completeEvent(eventName: EventName, eventData: EventData, properties: Properties): Boolean {
//        // complete action if relevant
//        val event = branch.pastInstructions
//            .filterIsInstance<EventWaited>()
//            .firstOrNull { a -> a.eventName == eventName && a.commandStatus != CommandStatus.COMPLETED }
//            ?.apply {
//                this.eventData = eventData
//                commandStatus = CommandStatus.COMPLETED
//            }
//
//        // does this task complete the current step?
//        return if (event != null) lastStep().completeEvent(event.eventId, properties) else false
//    }
//
//    private fun lastStep(): PastStep {
//        // last step must be a PastStep
//        return branch.pastInstructions.last() as PastStep
//    }
}
