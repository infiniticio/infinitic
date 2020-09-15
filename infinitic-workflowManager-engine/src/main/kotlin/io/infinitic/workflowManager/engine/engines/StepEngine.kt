package io.infinitic.workflowManager.engine.engines

import io.infinitic.workflowManager.common.data.steps.Step

class StepEngine(private val step: Step) {
//    @JsonIgnore
//    fun isCompleted() = step.isCompleted()
//
//    fun completeTask(taskId: TaskId, properties: Properties): Boolean {
//        return complete(CommandId(taskId), properties)
//    }
//
//    fun completeChildWorkflow(workflowId: WorkflowId, properties: Properties): Boolean {
//        return complete(CommandId(workflowId), properties)
//    }
//
//    fun completeDelay(delayId: DelayId, properties: Properties): Boolean {
//        return complete(CommandId(delayId), properties)
//    }
//
//    fun completeEvent(eventId: EventId, properties: Properties): Boolean {
//        return complete(CommandId(eventId), properties)
//    }
//
//    private fun complete(commandId: CommandId, properties: Properties): Boolean {
//        if (! isCompleted()) {
//            step.complete(commandId)
//            if (step.isCompleted()) {
// //                    propertiesAfterCompletion = properties.copy()
//                TODO()
//                return true
//            }
//        }
//        return false
//    }
}
