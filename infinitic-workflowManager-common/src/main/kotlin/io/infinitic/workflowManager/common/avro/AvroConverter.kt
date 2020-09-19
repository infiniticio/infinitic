package io.infinitic.workflowManager.common.avro

import io.infinitic.common.json.Json
import io.infinitic.workflowManager.common.data.instructions.PastCommand
import io.infinitic.workflowManager.common.data.instructions.StringPosition
import io.infinitic.workflowManager.common.data.methodRuns.MethodRun
import io.infinitic.workflowManager.common.data.methodRuns.MethodRunId
import io.infinitic.workflowManager.common.data.properties.Properties
import io.infinitic.workflowManager.common.data.properties.PropertyHash
import io.infinitic.workflowManager.common.data.properties.PropertyName
import io.infinitic.workflowManager.common.data.instructions.PastStep
import io.infinitic.workflowManager.common.data.steps.StepHash
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskId
import io.infinitic.workflowManager.common.data.workflows.WorkflowMessageIndex
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskInput
import io.infinitic.workflowManager.common.data.workflows.WorkflowId
import io.infinitic.workflowManager.common.data.workflows.WorkflowName
import io.infinitic.workflowManager.common.messages.CancelWorkflow
import io.infinitic.workflowManager.common.messages.ChildWorkflowCanceled
import io.infinitic.workflowManager.common.messages.ChildWorkflowCompleted
import io.infinitic.workflowManager.common.messages.WorkflowTaskCompleted
import io.infinitic.workflowManager.common.messages.WorkflowTaskDispatched
import io.infinitic.workflowManager.common.messages.TimerCompleted
import io.infinitic.workflowManager.common.messages.DispatchWorkflow
import io.infinitic.workflowManager.common.messages.ObjectReceived
import io.infinitic.workflowManager.common.messages.ForWorkflowEngineMessage
import io.infinitic.workflowManager.common.messages.Message
import io.infinitic.workflowManager.common.messages.TaskCanceled
import io.infinitic.workflowManager.common.messages.TaskCompleted
import io.infinitic.workflowManager.common.messages.TaskDispatched
import io.infinitic.workflowManager.common.messages.WorkflowCanceled
import io.infinitic.workflowManager.common.messages.WorkflowCompleted
import io.infinitic.workflowManager.common.states.WorkflowState
import io.infinitic.workflowManager.data.commands.AvroPastCommand
import io.infinitic.workflowManager.data.methodRuns.AvroMethodRun
import io.infinitic.workflowManager.data.steps.AvroPastStep
import io.infinitic.workflowManager.data.workflowTasks.AvroWorkflowTaskInput
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
import io.infinitic.workflowManager.messages.envelopes.AvroEnvelopeForWorkflowEngine
import io.infinitic.workflowManager.messages.envelopes.AvroForWorkflowEngineMessageType
import io.infinitic.workflowManager.states.AvroWorkflowState
import org.apache.avro.specific.SpecificRecordBase

// import io.infinitic.workflowManager.common.data.decisions.DecisionOutput

/**
 * This class does the mapping between avro-generated classes and classes actually used by our code
 */
object AvroConverter {

//    /**
//     *  State <-> Avro State
//     */
    fun fromStorage(avro: AvroWorkflowState) = WorkflowState(
        workflowId = WorkflowId(avro.workflowId),
        parentWorkflowId = avro.parentWorkflowId?.let { WorkflowId(it) },
        workflowName = WorkflowName(avro.workflowName),
        workflowOptions = convertJson(avro.workflowOptions),
        currentWorkflowTaskId = avro.currentWorkflowTaskId?.let { WorkflowTaskId(it) },
        currentMessageIndex = WorkflowMessageIndex(avro.currentWorkflowTaskIndex),
        currentMethodRuns = avro.currentMethodRuns.map { fromAvroMethodRun(it) }.toMutableList(),
        currentProperties = convertJson(avro.currentProperties),
        propertyStore = convertJson(avro.propertyStore),
        bufferedMessages = avro.bufferedMessages.map { fromWorkflowEngine(it) }.toMutableList()
    )

    fun toStorage(state: WorkflowState) = AvroWorkflowState
        .newBuilder()
        .setWorkflowId("${state.workflowId}")
        .setParentWorkflowId(state.parentWorkflowId?.toString())
        .setWorkflowName("${state.workflowName}")
        .setWorkflowOptions(convertJson(state.workflowOptions))
        .setCurrentWorkflowTaskId("${state.currentWorkflowTaskId}")
        .setCurrentWorkflowTaskIndex(convertJson(state.currentMessageIndex))
        .setCurrentMethodRuns(state.currentMethodRuns.map { toAvroMethodRun(it) })
        .setCurrentProperties(convertJson(state.currentProperties))
        .setPropertyStore(convertJson(state.propertyStore))
        .setBufferedMessages(state.bufferedMessages.map { toWorkflowEngine(it) })
        .build()
//
//    /**
//     *  Avro message <-> Avro Envelope
//     */
//
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
            else -> throw RuntimeException("Unknown AvroWorkflowEngineMessage: ${message::class.qualifiedName}")
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
    private fun fromAvroMessage(avro: AvroDecisionCompleted) = convertJson<WorkflowTaskCompleted>(avro)
    private fun fromAvroMessage(avro: AvroDelayCompleted) = convertJson<TimerCompleted>(avro)
    private fun fromAvroMessage(avro: AvroDispatchWorkflow) = convertJson<DispatchWorkflow>(avro)
    private fun fromAvroMessage(avro: AvroEventReceived) = convertJson<ObjectReceived>(avro)
    private fun fromAvroMessage(avro: AvroTaskCanceled) = convertJson<TaskCanceled>(avro)
    private fun fromAvroMessage(avro: AvroTaskCompleted) = convertJson<TaskCompleted>(avro)
    private fun fromAvroMessage(avro: AvroTaskDispatched) = convertJson<TaskDispatched>(avro)
    private fun fromAvroMessage(avro: AvroWorkflowCanceled) = convertJson<WorkflowCanceled>(avro)
    private fun fromAvroMessage(avro: AvroWorkflowCompleted) = convertJson<WorkflowCompleted>(avro)

    fun fromAvroMessage(avro: AvroDecisionDispatched) = WorkflowTaskDispatched(
        workflowTaskId = WorkflowTaskId(avro.workflowTaskId),
        workflowId = WorkflowId(avro.workflowId),
        workflowName = WorkflowName(avro.workflowName),
        workflowTaskInput = fromAvroDecisionInput(avro.workflowTaskInput)
    )

    fun toAvroMessage(message: Message): SpecificRecordBase = when (message) {
        is CancelWorkflow -> toAvroMessage(message)
        is ChildWorkflowCanceled -> toAvroMessage(message)
        is ChildWorkflowCompleted -> toAvroMessage(message)
        is WorkflowTaskCompleted -> toAvroMessage(message)
        is WorkflowTaskDispatched -> toAvroMessage(message)
        is TimerCompleted -> toAvroMessage(message)
        is DispatchWorkflow -> toAvroMessage(message)
        is ObjectReceived -> toAvroMessage(message)
        is TaskCanceled -> toAvroMessage(message)
        is TaskCompleted -> toAvroMessage(message)
        is TaskDispatched -> toAvroMessage(message)
        is WorkflowCanceled -> toAvroMessage(message)
        is WorkflowCompleted -> toAvroMessage(message)
    }

    private fun toAvroMessage(message: CancelWorkflow) = convertJson<AvroCancelWorkflow>(message)
    private fun toAvroMessage(message: ChildWorkflowCanceled) = convertJson<AvroChildWorkflowCanceled>(message)
    private fun toAvroMessage(message: ChildWorkflowCompleted) = convertJson<AvroChildWorkflowCompleted>(message)
    private fun toAvroMessage(message: WorkflowTaskCompleted) = convertJson<AvroDecisionCompleted>(message)
    private fun toAvroMessage(message: TimerCompleted) = convertJson<AvroDelayCompleted>(message)
    private fun toAvroMessage(message: DispatchWorkflow) = convertJson<AvroDispatchWorkflow>(message)
    private fun toAvroMessage(message: ObjectReceived) = convertJson<AvroEventReceived>(message)
    private fun toAvroMessage(message: TaskCanceled) = convertJson<AvroTaskCanceled>(message)
    private fun toAvroMessage(message: TaskCompleted) = convertJson<AvroTaskCompleted>(message)
    private fun toAvroMessage(message: TaskDispatched) = convertJson<AvroTaskDispatched>(message)
    private fun toAvroMessage(message: WorkflowCanceled) = convertJson<AvroWorkflowCanceled>(message)
    private fun toAvroMessage(message: WorkflowCompleted) = convertJson<AvroWorkflowCompleted>(message)

    fun toAvroMessage(message: WorkflowTaskDispatched) = AvroDecisionDispatched.newBuilder().apply {
        workflowTaskId = message.workflowTaskId.id
        workflowId = message.workflowId.id
        workflowName = message.workflowName.name
        workflowTaskInput = toAvroDecisionInput(message.workflowTaskInput)
    }.build()

    /**
     *  Decision Input
     */

    fun toAvroDecisionInput(obj: WorkflowTaskInput): AvroWorkflowTaskInput = AvroWorkflowTaskInput.newBuilder().apply {
        workflowId = "${obj.workflowId}"
        workflowName = "${obj.workflowName}"
        workflowOptions = convertJson(obj.workflowOptions)
        workflowPropertyStore = convertJson(obj.workflowPropertyStore)
        workflowTaskIndex = convertJson(obj.workflowMessageIndex)
        methoRun = toAvroMethodRun(obj.methodRun)
    }.build()

    fun fromAvroDecisionInput(avro: AvroWorkflowTaskInput) = WorkflowTaskInput(
        workflowId = WorkflowId(avro.workflowId),
        workflowName = WorkflowName(avro.workflowName),
        workflowOptions = convertJson(avro.workflowOptions),
        workflowPropertyStore = convertJson(avro.workflowOptions),
        workflowMessageIndex = WorkflowMessageIndex(avro.workflowTaskIndex),
        methodRun = fromAvroMethodRun(avro.methoRun)
    )

    /**
     *  Decision Output
     */

//     fun toAvroDecisionOutput(obj: DecisionOutput): AvroDecisionOutput = AvroDecisionOutput.newBuilder().apply {
//         branches = obj.branches.map { toAvroBranch(it) }
//     }.build()
//
//     fun fromAvroDecisionOutput(avro: AvroDecisionOutput) = DecisionOuput(
//         branches = avro.branches.map { fromAvroBranch(it) },
//         store = convertJson(avro.store)
//     )

    /**
     *  StepCriteria
     */

//    fun toAvroStepId(obj: Step.Id): AvroStepId = when (obj) {
//        is Step.Id -> AvroStep.newBuilder().apply {
//            type = AvroStepType.ID
//            commandId = obj.commandId.id
//            commandStatus = obj.commandStatus
//        }.build()
//        is Step.Or -> AvroStep.newBuilder().apply {
//            type = AvroStepType.OR
//            commandCriteria = obj.commands.map { this.toAvroStep(it) }
//        }.build()
//        is Step.And -> AvroStep.newBuilder().apply {
//            type = AvroStepType.AND
//            commandCriteria = obj.commands.map { this.toAvroStep(it) }
//        }.build()
//    }
//
//    fun fromAvroStep(avro: AvroStep): Step = when (avro.type) {
//        AvroStepType.ID -> Step.Id(
//            commandId = CommandId(avro.commandId),
//            commandStatus = avro.commandStatus
//        )
//        AvroStepType.OR -> Step.Or(
//            commands = avro.commandCriteria.map { fromAvroStep(it) }
//        )
//        AvroStepType.AND -> Step.And(
//            commands = avro.commandCriteria.map { fromAvroStep(it) }
//        )
//        null -> throw Exception("this should not happen")
//    }

    /**
     *  Steps
     */
    fun toAvroPastStep(obj: PastStep): AvroPastStep = AvroPastStep.newBuilder().apply {
        stepPosition = "${obj.stringPosition}"
        step = convertJson(obj.step)
        stepHash = "${obj.stepHash}"
        stepStatus = convertJson(obj.stepStatus)
        workflowPropertiesAfterCompletion = convertJson(obj.propertiesAtTermination)
        completedFromWorkflowTaskIndex = convertJson(obj.workflowMessageIndexAtTermination)
    }.build()

    fun fromAvroPastStep(avro: AvroPastStep) = PastStep(
        stringPosition = StringPosition(avro.stepPosition),
        step = convertJson(avro.step),
        stepHash = StepHash(avro.stepHash),
        stepStatus = convertJson(avro.stepStatus),
        propertiesAtTermination = convertJson(avro.workflowPropertiesAfterCompletion),
        workflowMessageIndexAtTermination = WorkflowMessageIndex(avro.completedFromWorkflowTaskIndex)
    )

    /**
     *  Branches
     */

    fun toAvroMethodRun(obj: MethodRun): AvroMethodRun = AvroMethodRun.newBuilder().apply {
        isMain = obj.isMain
        methodRunId = "${obj.methodRunId}"
        methodName = convertJson(obj.methodName)
        methodInput = convertJson(obj.methodInput)
        messageIndexAtStart = obj.messageIndexAtStart.int
        methodPropertiesAtStart = toAvroProperties(obj.propertiesAtStart)
        methodPastInstructions = obj.pastInstructions.map {
            when (it) {
                is PastCommand -> convertJson<AvroPastCommand>(it)
                is PastStep -> convertJson<AvroPastStep>(it)
                else -> throw RuntimeException()
            }
        }
    }.build()

    fun fromAvroMethodRun(avro: AvroMethodRun) = MethodRun(
        isMain = avro.isMain,
        methodRunId = MethodRunId(avro.methodRunId),
        methodName = convertJson(avro.methodName),
        methodInput = convertJson(avro.methodInput),
        messageIndexAtStart = WorkflowMessageIndex(avro.messageIndexAtStart),
        propertiesAtStart = fromAvroProperties(avro.methodPropertiesAtStart),
        pastInstructions = avro.methodPastInstructions.map {
            when (it) {
                is AvroPastCommand -> convertJson<PastCommand>(it)
                is AvroPastStep -> convertJson<PastStep>(it)
                else -> throw RuntimeException()
            }
        }.toMutableList()
    )

    /**
     *  Properties
     */

    fun toAvroProperties(obj: Properties): Map<String, String> = convertJson(obj)

    fun fromAvroProperties(avro: Map<String, String>) = Properties(
        avro
            .mapValues { PropertyHash(it.value) }
            .mapKeys { PropertyName(it.key) }
            .toMutableMap()
    )

    /**
     *  Mapping function by Json serialization/deserialization
     */
    inline fun <reified T : Any> convertJson(from: Any?): T = Json.parse(Json.stringify(from))
}
