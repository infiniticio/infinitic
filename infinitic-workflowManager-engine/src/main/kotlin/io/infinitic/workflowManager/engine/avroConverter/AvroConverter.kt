package io.infinitic.workflowManager.engine.avroConverter

import io.infinitic.common.data.DateTime
import io.infinitic.common.data.SerializedData
import io.infinitic.common.json.Json
import io.infinitic.taskManager.data.AvroSerializedData
import io.infinitic.workflowManager.engine.data.decisions.DecisionId
import io.infinitic.workflowManager.engine.data.decisions.DecisionInput
// import io.infinitic.workflowManager.engine.data.decisions.DecisionOutput
import io.infinitic.workflowManager.engine.data.WorkflowId
import io.infinitic.workflowManager.engine.data.WorkflowName
import io.infinitic.workflowManager.engine.data.commands.Command
import io.infinitic.workflowManager.engine.data.commands.CommandId
import io.infinitic.workflowManager.data.branches.AvroBranch
import io.infinitic.workflowManager.engine.data.branches.Branch
import io.infinitic.workflowManager.engine.data.branches.BranchId
import io.infinitic.workflowManager.engine.data.branches.BranchInput
import io.infinitic.workflowManager.engine.data.branches.BranchName
import io.infinitic.workflowManager.data.commands.AvroCommand
import io.infinitic.workflowManager.data.decisions.AvroDecisionInput
import io.infinitic.workflowManager.engine.data.properties.Properties
import io.infinitic.workflowManager.engine.data.properties.PropertyHash
import io.infinitic.workflowManager.engine.data.properties.PropertyKey
import io.infinitic.workflowManager.data.steps.AvroStep
import io.infinitic.workflowManager.data.steps.AvroStepCriterion
import io.infinitic.workflowManager.data.steps.AvroStepCriterionType
import io.infinitic.workflowManager.engine.data.steps.Step
import io.infinitic.workflowManager.engine.data.steps.StepCriterion
import io.infinitic.workflowManager.engine.data.steps.StepHash
import io.infinitic.workflowManager.messages.AvroCancelWorkflow
import io.infinitic.workflowManager.messages.AvroChildWorkflowCanceled
import io.infinitic.workflowManager.messages.AvroChildWorkflowCompleted
import io.infinitic.workflowManager.messages.AvroDecisionCompleted
import io.infinitic.workflowManager.messages.AvroDecisionDispatched
import io.infinitic.workflowManager.messages.AvroDelayCompleted
import io.infinitic.workflowManager.messages.AvroDispatchWorkflow
import io.infinitic.workflowManager.messages.AvroEventReceived
import io.infinitic.workflowManager.messages.AvroTaskCanceled
import io.infinitic.workflowManager.messages.AvroTaskCompleted
import io.infinitic.workflowManager.messages.AvroTaskDispatched
import io.infinitic.workflowManager.messages.AvroWorkflowCanceled
import io.infinitic.workflowManager.messages.AvroWorkflowCompleted
import io.infinitic.workflowManager.engine.messages.CancelWorkflow
import io.infinitic.workflowManager.engine.messages.ChildWorkflowCanceled
import io.infinitic.workflowManager.engine.messages.ChildWorkflowCompleted
import io.infinitic.workflowManager.engine.messages.DecisionCompleted
import io.infinitic.workflowManager.engine.messages.DecisionDispatched
import io.infinitic.workflowManager.engine.messages.DelayCompleted
import io.infinitic.workflowManager.engine.messages.DispatchWorkflow
import io.infinitic.workflowManager.engine.messages.EventReceived
import io.infinitic.workflowManager.engine.messages.ForWorkflowEngineMessage
import io.infinitic.workflowManager.engine.messages.Message
import io.infinitic.workflowManager.engine.messages.TaskCanceled
import io.infinitic.workflowManager.engine.messages.TaskCompleted
import io.infinitic.workflowManager.engine.messages.TaskDispatched
import io.infinitic.workflowManager.engine.messages.WorkflowCanceled
import io.infinitic.workflowManager.engine.messages.WorkflowCompleted
import io.infinitic.workflowManager.messages.envelopes.AvroEnvelopeForWorkflowEngine
import io.infinitic.workflowManager.messages.envelopes.AvroForWorkflowEngineMessageType
import io.infinitic.workflowManager.states.AvroWorkflowEngineState
import io.infinitic.workflowManager.engine.states.WorkflowEngineState
import org.apache.avro.specific.SpecificRecordBase

/**
 * This class does the mapping between avro-generated classes and classes actually used by our code
 */
object AvroConverter {

    /**
     *  State <-> Avro State
     */
    fun fromStorage(avro: AvroWorkflowEngineState) = WorkflowEngineState(
        workflowId = WorkflowId(avro.workflowId),
        parentWorkflowId = if (avro.parentWorkflowId == null) null else WorkflowId(avro.parentWorkflowId),
        ongoingDecisionId = if (avro.ongoingDecisionId == null) null else DecisionId(avro.ongoingDecisionId),
        bufferedMessages = avro.bufferedMessages.map { fromWorkflowEngine(it) }.toMutableList(),
        store = convertJson(avro.store),
        runningBranches = avro.runningBranches.map { fromAvroBranch(it) }.toMutableList(),
        currentProperties = convertJson(avro.currentProperties)
    )

    fun toStorage(state: WorkflowEngineState) = AvroWorkflowEngineState
        .newBuilder()
        .setWorkflowId(state.workflowId.id)
        .setParentWorkflowId(state.parentWorkflowId?.id)
        .setOngoingDecisionId(state.ongoingDecisionId?.id)
        .setBufferedMessages(state.bufferedMessages.map { toWorkflowEngine(it) })
        .setStore(convertJson(state.store))
        .setRunningBranches(state.runningBranches.map { toAvroBranch(it) })
        .setCurrentProperties(convertJson(state.currentProperties))
        .build()

    /**
     *  Avro message <-> Avro Envelope
     */

    fun addEnvelopeToWorkflowEngineMessage(message: SpecificRecordBase): AvroEnvelopeForWorkflowEngine {
        val builder = AvroEnvelopeForWorkflowEngine.newBuilder()
        when (message) {
            is AvroCancelWorkflow -> builder.apply {
                workflowId = message.workflowId
                avroCancelWorkflow = message
                type = AvroForWorkflowEngineMessageType.AvroCancelWorkflow
            }
            is AvroChildWorkflowCanceled -> builder.apply {
                workflowId = message.workflowId
                avroChildWorkflowCanceled = message
                type = AvroForWorkflowEngineMessageType.AvroChildWorkflowCanceled
            }
            is AvroChildWorkflowCompleted -> builder.apply {
                workflowId = message.workflowId
                avroChildWorkflowCompleted = message
                type = AvroForWorkflowEngineMessageType.AvroChildWorkflowCompleted
            }
            is AvroDecisionCompleted -> builder.apply {
                workflowId = message.workflowId
                avroDecisionCompleted = message
                type = AvroForWorkflowEngineMessageType.AvroDecisionCompleted
            }
            is AvroDecisionDispatched -> builder.apply {
                workflowId = message.workflowId
                avroDecisionDispatched = message
                type = AvroForWorkflowEngineMessageType.AvroDecisionDispatched
            }
            is AvroDelayCompleted -> builder.apply {
                workflowId = message.workflowId
                avroDelayCompleted = message
                type = AvroForWorkflowEngineMessageType.AvroDelayCompleted
            }
            is AvroDispatchWorkflow -> builder.apply {
                workflowId = message.workflowId
                avroDispatchWorkflow = message
                type = AvroForWorkflowEngineMessageType.AvroDispatchWorkflow
            }
            is AvroEventReceived -> builder.apply {
                workflowId = message.workflowId
                avroEventReceived = message
                type = AvroForWorkflowEngineMessageType.AvroEventReceived
            }
            is AvroTaskCanceled -> builder.apply {
                workflowId = message.workflowId
                avroTaskCanceled = message
                type = AvroForWorkflowEngineMessageType.AvroTaskCanceled
            }
            is AvroTaskCompleted -> builder.apply {
                workflowId = message.workflowId
                avroTaskCompleted = message
                type = AvroForWorkflowEngineMessageType.AvroTaskCompleted
            }
            is AvroTaskDispatched -> builder.apply {
                workflowId = message.workflowId
                avroTaskDispatched = message
                type = AvroForWorkflowEngineMessageType.AvroTaskDispatched
            }
            is AvroWorkflowCanceled -> builder.apply {
                workflowId = message.workflowId
                avroWorkflowCanceled = message
                type = AvroForWorkflowEngineMessageType.AvroWorkflowCanceled
            }
            is AvroWorkflowCompleted -> builder.apply {
                workflowId = message.workflowId
                avroWorkflowCompleted = message
                type = AvroForWorkflowEngineMessageType.AvroWorkflowCompleted
            }
            else -> throw Exception("Unknown AvroWorkflowEngineMessage: ${message::class.qualifiedName}")
        }
        return builder.build()
    }

    fun removeEnvelopeFromWorkflowEngineMessage(input: AvroEnvelopeForWorkflowEngine): SpecificRecordBase = when (input.type) {
        AvroForWorkflowEngineMessageType.AvroCancelWorkflow -> input.avroCancelWorkflow
        AvroForWorkflowEngineMessageType.AvroChildWorkflowCanceled -> input.avroChildWorkflowCanceled
        AvroForWorkflowEngineMessageType.AvroChildWorkflowCompleted -> input.avroChildWorkflowCompleted
        AvroForWorkflowEngineMessageType.AvroDecisionCompleted -> input.avroDecisionCompleted
        AvroForWorkflowEngineMessageType.AvroDecisionDispatched -> input.avroDecisionDispatched
        AvroForWorkflowEngineMessageType.AvroDelayCompleted -> input.avroDelayCompleted
        AvroForWorkflowEngineMessageType.AvroDispatchWorkflow -> input.avroDispatchWorkflow
        AvroForWorkflowEngineMessageType.AvroEventReceived -> input.avroEventReceived
        AvroForWorkflowEngineMessageType.AvroTaskCanceled -> input.avroTaskCanceled
        AvroForWorkflowEngineMessageType.AvroTaskCompleted -> input.avroTaskCompleted
        AvroForWorkflowEngineMessageType.AvroTaskDispatched -> input.avroTaskDispatched
        AvroForWorkflowEngineMessageType.AvroWorkflowCanceled -> input.avroWorkflowCanceled
        AvroForWorkflowEngineMessageType.AvroWorkflowCompleted -> input.avroWorkflowCompleted
        null -> throw Exception("Null type in $input")
    }

    /**
     *  Message <-> Avro Envelope
     */

    fun toWorkflowEngine(message: ForWorkflowEngineMessage): AvroEnvelopeForWorkflowEngine =
        addEnvelopeToWorkflowEngineMessage(toAvroMessage(message))

    fun fromWorkflowEngine(avro: AvroEnvelopeForWorkflowEngine) =
        fromAvroMessage(removeEnvelopeFromWorkflowEngineMessage(avro)) as ForWorkflowEngineMessage

    /**
     *  Message <-> Avro Message
     */

    fun fromAvroMessage(avro: SpecificRecordBase): Message = when (avro) {
        is AvroCancelWorkflow -> fromAvroMessage(avro)
        is AvroChildWorkflowCanceled -> fromAvroMessage(avro)
        is AvroChildWorkflowCompleted -> fromAvroMessage(avro)
        is AvroDecisionCompleted -> fromAvroMessage(avro)
        is AvroDecisionDispatched -> fromAvroMessage(avro)
        is AvroDelayCompleted -> fromAvroMessage(avro)
        is AvroDispatchWorkflow -> fromAvroMessage(avro)
        is AvroEventReceived -> fromAvroMessage(avro)
        is AvroTaskCanceled -> fromAvroMessage(avro)
        is AvroTaskCompleted -> fromAvroMessage(avro)
        is AvroTaskDispatched -> fromAvroMessage(avro)
        is AvroWorkflowCanceled -> fromAvroMessage(avro)
        is AvroWorkflowCompleted -> fromAvroMessage(avro)
        else -> throw Exception("Unknown SpecificRecordBase: ${avro::class.qualifiedName}")
    }

    private fun fromAvroMessage(avro: AvroCancelWorkflow) = convertJson<CancelWorkflow>(avro)
    private fun fromAvroMessage(avro: AvroChildWorkflowCanceled) = convertJson<ChildWorkflowCanceled>(avro)
    private fun fromAvroMessage(avro: AvroChildWorkflowCompleted) = convertJson<ChildWorkflowCompleted>(avro)
    private fun fromAvroMessage(avro: AvroDecisionCompleted) = convertJson<DecisionCompleted>(avro)
    private fun fromAvroMessage(avro: AvroDelayCompleted) = convertJson<DelayCompleted>(avro)
    private fun fromAvroMessage(avro: AvroDispatchWorkflow) = convertJson<DispatchWorkflow>(avro)
    private fun fromAvroMessage(avro: AvroEventReceived) = convertJson<EventReceived>(avro)
    private fun fromAvroMessage(avro: AvroTaskCanceled) = convertJson<TaskCanceled>(avro)
    private fun fromAvroMessage(avro: AvroTaskCompleted) = convertJson<TaskCompleted>(avro)
    private fun fromAvroMessage(avro: AvroTaskDispatched) = convertJson<TaskDispatched>(avro)
    private fun fromAvroMessage(avro: AvroWorkflowCanceled) = convertJson<WorkflowCanceled>(avro)
    private fun fromAvroMessage(avro: AvroWorkflowCompleted) = convertJson<WorkflowCompleted>(avro)

    fun fromAvroMessage(avro: AvroDecisionDispatched) = DecisionDispatched(
        decisionId = DecisionId(avro.decisionId),
        workflowId = WorkflowId(avro.workflowId),
        workflowName = WorkflowName(avro.workflowName),
        decisionInput = fromAvroDecisionInput(avro.decisionInput)
    )

    fun toAvroMessage(message: Message): SpecificRecordBase = when (message) {
        is CancelWorkflow -> toAvroMessage(message)
        is ChildWorkflowCanceled -> toAvroMessage(message)
        is ChildWorkflowCompleted -> toAvroMessage(message)
        is DecisionCompleted -> toAvroMessage(message)
        is DecisionDispatched -> toAvroMessage(message)
        is DelayCompleted -> toAvroMessage(message)
        is DispatchWorkflow -> toAvroMessage(message)
        is EventReceived -> toAvroMessage(message)
        is TaskCanceled -> toAvroMessage(message)
        is TaskCompleted -> toAvroMessage(message)
        is TaskDispatched -> toAvroMessage(message)
        is WorkflowCanceled -> toAvroMessage(message)
        is WorkflowCompleted -> toAvroMessage(message)
    }

    private fun toAvroMessage(message: CancelWorkflow) = convertJson<AvroCancelWorkflow>(message)
    private fun toAvroMessage(message: ChildWorkflowCanceled) = convertJson<AvroChildWorkflowCanceled>(message)
    private fun toAvroMessage(message: ChildWorkflowCompleted) = convertJson<AvroChildWorkflowCompleted>(message)
    private fun toAvroMessage(message: DecisionCompleted) = convertJson<AvroDecisionCompleted>(message)
    private fun toAvroMessage(message: DelayCompleted) = convertJson<AvroDelayCompleted>(message)
    private fun toAvroMessage(message: DispatchWorkflow) = convertJson<AvroDispatchWorkflow>(message)
    private fun toAvroMessage(message: EventReceived) = convertJson<AvroEventReceived>(message)
    private fun toAvroMessage(message: TaskCanceled) = convertJson<AvroTaskCanceled>(message)
    private fun toAvroMessage(message: TaskCompleted) = convertJson<AvroTaskCompleted>(message)
    private fun toAvroMessage(message: TaskDispatched) = convertJson<AvroTaskDispatched>(message)
    private fun toAvroMessage(message: WorkflowCanceled) = convertJson<AvroWorkflowCanceled>(message)
    private fun toAvroMessage(message: WorkflowCompleted) = convertJson<AvroWorkflowCompleted>(message)

    fun toAvroMessage(message: DecisionDispatched) = AvroDecisionDispatched.newBuilder().apply {
        decisionId = message.decisionId.id
        workflowId = message.workflowId.id
        workflowName = message.workflowName.name
        decisionInput = toAvroDecisionInput(message.decisionInput)
    }.build()

    /**
     *  Decision Input
     */

    fun toAvroDecisionInput(obj: DecisionInput): AvroDecisionInput = AvroDecisionInput.newBuilder().apply {
        branches = obj.branches.map { toAvroBranch(it) }
        store = convertJson(obj.store)
    }.build()

    fun fromAvroDecisionInput(avro: AvroDecisionInput) = DecisionInput(
        branches = avro.branches.map { fromAvroBranch(it) },
        store = convertJson(avro.store)
    )

    /**
     *  Decision Output
     */

//    fun toAvroDecisionOutput(obj: DecisionOutput): AvroDecisionOutput = AvroDecisionOutput.newBuilder().apply {
//        branches = obj.branches.map { toAvroBranch(it) }
//    }.build()
//
//    fun fromAvroDecisionOutput(avro: AvroDecisionOutput) = DecisionOuput(
//        branches = avro.branches.map { fromAvroBranch(it) },
//        store = convertJson(avro.store)
//    )

    /**
     *  StepCriteria
     */

    fun toAvroStepCriterion(obj: StepCriterion): AvroStepCriterion = when (obj) {
        is StepCriterion.Id -> AvroStepCriterion.newBuilder().apply {
            type = AvroStepCriterionType.ID
            commandId = obj.commandId.id
            commandStatus = obj.commandStatus
        }.build()
        is StepCriterion.Or -> AvroStepCriterion.newBuilder().apply {
            type = AvroStepCriterionType.OR
            commandCriteria = obj.commandCriteria.map { toAvroStepCriterion(it) }
        }.build()
        is StepCriterion.And -> AvroStepCriterion.newBuilder().apply {
            type = AvroStepCriterionType.AND
            commandCriteria = obj.commandCriteria.map { toAvroStepCriterion(it) }
        }.build()
    }

    fun fromAvroStepCriterion(avro: AvroStepCriterion): StepCriterion = when (avro.type) {
        AvroStepCriterionType.ID -> StepCriterion.Id(
            commandId = CommandId(avro.commandId),
            commandStatus = avro.commandStatus
        )
        AvroStepCriterionType.OR -> StepCriterion.Or(
            commandCriteria = avro.commandCriteria.map { fromAvroStepCriterion(it) }
        )
        AvroStepCriterionType.AND -> StepCriterion.And(
            commandCriteria = avro.commandCriteria.map { fromAvroStepCriterion(it) }
        )
        null -> throw Exception("this should not happen")
    }

    /**
     *  Steps
     */
    fun toAvroStep(obj: Step): AvroStep = AvroStep.newBuilder().apply {
        stepHash = obj.stepHash.hash
        criterion = toAvroStepCriterion(obj.criterion)
        propertiesAfterCompletion = convertJson(obj.propertiesAfterCompletion)
    }.build()

    fun fromAvroStep(avro: AvroStep) = Step(
        stepHash = StepHash(avro.stepHash),
        criterion = fromAvroStepCriterion(avro.criterion),
        propertiesAfterCompletion = convertJson(avro.propertiesAfterCompletion)
    )

    /**
     *  Branches
     */

    fun toAvroBranch(obj: Branch): AvroBranch = AvroBranch.newBuilder().apply {
        branchId = obj.branchId.id
        branchName = obj.branchName.name
        branchInput = obj.branchInput.input.map { convertJson<AvroSerializedData>(it) }
        propertiesAtStart = toAvroProperties(obj.propertiesAtStart)
        dispatchedAt = convertJson(obj.dispatchedAt)
        steps = obj.steps.map { toAvroStep(it) }
        commands = obj.commands.map { convertJson<AvroCommand>(it) }
    }.build()

    fun fromAvroBranch(avro: AvroBranch) = Branch(
        branchId = BranchId(avro.branchId),
        branchName = BranchName(avro.branchName),
        branchInput = BranchInput(avro.branchInput.map { convertJson<SerializedData>(it) }),
        propertiesAtStart = fromAvroProperties(avro.propertiesAtStart),
        dispatchedAt = DateTime(avro.dispatchedAt),
        steps = avro.steps.map { fromAvroStep(it) },
        commands = avro.commands.map { convertJson<Command>(it) }
    )

    /**
     *  Properties
     */

    fun toAvroProperties(obj: Properties): Map<String, String> = convertJson(obj)

    fun fromAvroProperties(avro: Map<String, String>) = Properties(
        avro
            .mapValues { PropertyHash(it.value) }
            .mapKeys { PropertyKey(it.key) }
    )

    /**
     *  Mapping function by Json serialization/deserialization
     */
    inline fun <reified T : Any> convertJson(from: Any): T = Json.parse(Json.stringify(from))
}
