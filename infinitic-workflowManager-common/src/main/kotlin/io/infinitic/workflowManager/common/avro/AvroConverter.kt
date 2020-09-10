package io.infinitic.workflowManager.common.avro

// import io.infinitic.workflowManager.common.data.decisions.DecisionOutput

/**
 * This class does the mapping between avro-generated classes and classes actually used by our code
 */
object AvroConverter {

//    /**
//     *  State <-> Avro State
//     */
//    fun fromStorage(avro: AvroWorkflowEngineState) = WorkflowEngineState(
//        workflowId = WorkflowId(avro.workflowId),
//        parentWorkflowId = if (avro.parentWorkflowId == null) null else WorkflowId(avro.parentWorkflowId),
//        ongoingWorkflowTaskId = if (avro.ongoingDecisionId == null) null else WorkflowTaskId(avro.ongoingDecisionId),
//        bufferedMessages = avro.bufferedMessages.map { fromWorkflowEngine(it) }.toMutableList(),
//        store = convertJson(avro.store),
//        runningBranches = avro.runningBranches.map { fromAvroBranch(it) }.toMutableList(),
//        currentProperties = convertJson(avro.currentProperties)
//    )
//
//    fun toStorage(state: WorkflowEngineState) = AvroWorkflowEngineState
//        .newBuilder()
//        .setWorkflowId(state.workflowId.id)
//        .setParentWorkflowId(state.parentWorkflowId?.id)
//        .setOngoingDecisionId(state.ongoingWorkflowTaskId?.id)
//        .setBufferedMessages(state.bufferedMessages.map { toWorkflowEngine(it) })
//        .setStore(convertJson(state.store))
//        .setRunningBranches(state.runningBranches.map { toAvroBranch(it) })
//        .setCurrentProperties(convertJson(state.currentProperties))
//        .build()
//
//    /**
//     *  Avro message <-> Avro Envelope
//     */
//
//    fun addEnvelopeToWorkflowEngineMessage(message: SpecificRecordBase): AvroEnvelopeForWorkflowEngine {
//        val builder = AvroEnvelopeForWorkflowEngine.newBuilder()
//        when (message) {
//            is AvroCancelWorkflow -> builder.apply {
//                workflowId = message.workflowId
//                avroCancelWorkflow = message
//                type = AvroForWorkflowEngineMessageType.AvroCancelWorkflow
//            }
//            is AvroChildWorkflowCanceled -> builder.apply {
//                workflowId = message.workflowId
//                avroChildWorkflowCanceled = message
//                type = AvroForWorkflowEngineMessageType.AvroChildWorkflowCanceled
//            }
//            is AvroChildWorkflowCompleted -> builder.apply {
//                workflowId = message.workflowId
//                avroChildWorkflowCompleted = message
//                type = AvroForWorkflowEngineMessageType.AvroChildWorkflowCompleted
//            }
//            is AvroDecisionCompleted -> builder.apply {
//                workflowId = message.workflowId
//                avroDecisionCompleted = message
//                type = AvroForWorkflowEngineMessageType.AvroDecisionCompleted
//            }
//            is AvroDecisionDispatched -> builder.apply {
//                workflowId = message.workflowId
//                avroDecisionDispatched = message
//                type = AvroForWorkflowEngineMessageType.AvroDecisionDispatched
//            }
//            is AvroDelayCompleted -> builder.apply {
//                workflowId = message.workflowId
//                avroDelayCompleted = message
//                type = AvroForWorkflowEngineMessageType.AvroDelayCompleted
//            }
//            is AvroDispatchWorkflow -> builder.apply {
//                workflowId = message.workflowId
//                avroDispatchWorkflow = message
//                type = AvroForWorkflowEngineMessageType.AvroDispatchWorkflow
//            }
//            is AvroEventReceived -> builder.apply {
//                workflowId = message.workflowId
//                avroEventReceived = message
//                type = AvroForWorkflowEngineMessageType.AvroEventReceived
//            }
//            is AvroTaskCanceled -> builder.apply {
//                workflowId = message.workflowId
//                avroTaskCanceled = message
//                type = AvroForWorkflowEngineMessageType.AvroTaskCanceled
//            }
//            is AvroTaskCompleted -> builder.apply {
//                workflowId = message.workflowId
//                avroTaskCompleted = message
//                type = AvroForWorkflowEngineMessageType.AvroTaskCompleted
//            }
//            is AvroTaskDispatched -> builder.apply {
//                workflowId = message.workflowId
//                avroTaskDispatched = message
//                type = AvroForWorkflowEngineMessageType.AvroTaskDispatched
//            }
//            is AvroWorkflowCanceled -> builder.apply {
//                workflowId = message.workflowId
//                avroWorkflowCanceled = message
//                type = AvroForWorkflowEngineMessageType.AvroWorkflowCanceled
//            }
//            is AvroWorkflowCompleted -> builder.apply {
//                workflowId = message.workflowId
//                avroWorkflowCompleted = message
//                type = AvroForWorkflowEngineMessageType.AvroWorkflowCompleted
//            }
//            else -> throw Exception("Unknown AvroWorkflowEngineMessage: ${message::class.qualifiedName}")
//        }
//        return builder.build()
//    }
//
//    fun removeEnvelopeFromWorkflowEngineMessage(input: AvroEnvelopeForWorkflowEngine): SpecificRecordBase = when (input.type) {
//        AvroForWorkflowEngineMessageType.AvroCancelWorkflow -> input.avroCancelWorkflow
//        AvroForWorkflowEngineMessageType.AvroChildWorkflowCanceled -> input.avroChildWorkflowCanceled
//        AvroForWorkflowEngineMessageType.AvroChildWorkflowCompleted -> input.avroChildWorkflowCompleted
//        AvroForWorkflowEngineMessageType.AvroDecisionCompleted -> input.avroDecisionCompleted
//        AvroForWorkflowEngineMessageType.AvroDecisionDispatched -> input.avroDecisionDispatched
//        AvroForWorkflowEngineMessageType.AvroDelayCompleted -> input.avroDelayCompleted
//        AvroForWorkflowEngineMessageType.AvroDispatchWorkflow -> input.avroDispatchWorkflow
//        AvroForWorkflowEngineMessageType.AvroEventReceived -> input.avroEventReceived
//        AvroForWorkflowEngineMessageType.AvroTaskCanceled -> input.avroTaskCanceled
//        AvroForWorkflowEngineMessageType.AvroTaskCompleted -> input.avroTaskCompleted
//        AvroForWorkflowEngineMessageType.AvroTaskDispatched -> input.avroTaskDispatched
//        AvroForWorkflowEngineMessageType.AvroWorkflowCanceled -> input.avroWorkflowCanceled
//        AvroForWorkflowEngineMessageType.AvroWorkflowCompleted -> input.avroWorkflowCompleted
//        null -> throw Exception("Null type in $input")
//    }
//
//    /**
//     *  Message <-> Avro Envelope
//     */
//
//    fun toWorkflowEngine(message: ForWorkflowEngineMessage): AvroEnvelopeForWorkflowEngine =
//        addEnvelopeToWorkflowEngineMessage(toAvroMessage(message))
//
//    fun fromWorkflowEngine(avro: AvroEnvelopeForWorkflowEngine) =
//        fromAvroMessage(removeEnvelopeFromWorkflowEngineMessage(avro)) as ForWorkflowEngineMessage
//
//    /**
//     *  Message <-> Avro Message
//     */
//
//    fun fromAvroMessage(avro: SpecificRecordBase): Message = when (avro) {
//        is AvroCancelWorkflow -> fromAvroMessage(avro)
//        is AvroChildWorkflowCanceled -> fromAvroMessage(avro)
//        is AvroChildWorkflowCompleted -> fromAvroMessage(avro)
//        is AvroDecisionCompleted -> fromAvroMessage(avro)
//        is AvroDecisionDispatched -> fromAvroMessage(avro)
//        is AvroDelayCompleted -> fromAvroMessage(avro)
//        is AvroDispatchWorkflow -> fromAvroMessage(avro)
//        is AvroEventReceived -> fromAvroMessage(avro)
//        is AvroTaskCanceled -> fromAvroMessage(avro)
//        is AvroTaskCompleted -> fromAvroMessage(avro)
//        is AvroTaskDispatched -> fromAvroMessage(avro)
//        is AvroWorkflowCanceled -> fromAvroMessage(avro)
//        is AvroWorkflowCompleted -> fromAvroMessage(avro)
//        else -> throw Exception("Unknown SpecificRecordBase: ${avro::class.qualifiedName}")
//    }
//
//    private fun fromAvroMessage(avro: AvroCancelWorkflow) = convertJson<CancelWorkflow>(avro)
//    private fun fromAvroMessage(avro: AvroChildWorkflowCanceled) = convertJson<ChildWorkflowCanceled>(avro)
//    private fun fromAvroMessage(avro: AvroChildWorkflowCompleted) = convertJson<ChildWorkflowCompleted>(avro)
//    private fun fromAvroMessage(avro: AvroDecisionCompleted) = convertJson<DecisionCompleted>(avro)
//    private fun fromAvroMessage(avro: AvroDelayCompleted) = convertJson<DelayCompleted>(avro)
//    private fun fromAvroMessage(avro: AvroDispatchWorkflow) = convertJson<DispatchWorkflow>(avro)
//    private fun fromAvroMessage(avro: AvroEventReceived) = convertJson<EventReceived>(avro)
//    private fun fromAvroMessage(avro: AvroTaskCanceled) = convertJson<TaskCanceled>(avro)
//    private fun fromAvroMessage(avro: AvroTaskCompleted) = convertJson<TaskCompleted>(avro)
//    private fun fromAvroMessage(avro: AvroTaskDispatched) = convertJson<TaskDispatched>(avro)
//    private fun fromAvroMessage(avro: AvroWorkflowCanceled) = convertJson<WorkflowCanceled>(avro)
//    private fun fromAvroMessage(avro: AvroWorkflowCompleted) = convertJson<WorkflowCompleted>(avro)
//
//    fun fromAvroMessage(avro: AvroDecisionDispatched) = DecisionDispatched(
//        workflowTaskId = WorkflowTaskId(avro.decisionId),
//        workflowId = WorkflowId(avro.workflowId),
//        workflowName = WorkflowName(avro.workflowName),
//        workflowTaskInput = fromAvroDecisionInput(avro.decisionInput)
//    )
//
//    fun toAvroMessage(message: Message): SpecificRecordBase = when (message) {
//        is CancelWorkflow -> toAvroMessage(message)
//        is ChildWorkflowCanceled -> toAvroMessage(message)
//        is ChildWorkflowCompleted -> toAvroMessage(message)
//        is DecisionCompleted -> toAvroMessage(message)
//        is DecisionDispatched -> toAvroMessage(message)
//        is DelayCompleted -> toAvroMessage(message)
//        is DispatchWorkflow -> toAvroMessage(message)
//        is EventReceived -> toAvroMessage(message)
//        is TaskCanceled -> toAvroMessage(message)
//        is TaskCompleted -> toAvroMessage(message)
//        is TaskDispatched -> toAvroMessage(message)
//        is WorkflowCanceled -> toAvroMessage(message)
//        is WorkflowCompleted -> toAvroMessage(message)
//    }
//
//    private fun toAvroMessage(message: CancelWorkflow) = convertJson<AvroCancelWorkflow>(message)
//    private fun toAvroMessage(message: ChildWorkflowCanceled) = convertJson<AvroChildWorkflowCanceled>(message)
//    private fun toAvroMessage(message: ChildWorkflowCompleted) = convertJson<AvroChildWorkflowCompleted>(message)
//    private fun toAvroMessage(message: DecisionCompleted) = convertJson<AvroDecisionCompleted>(message)
//    private fun toAvroMessage(message: DelayCompleted) = convertJson<AvroDelayCompleted>(message)
//    private fun toAvroMessage(message: DispatchWorkflow) = convertJson<AvroDispatchWorkflow>(message)
//    private fun toAvroMessage(message: EventReceived) = convertJson<AvroEventReceived>(message)
//    private fun toAvroMessage(message: TaskCanceled) = convertJson<AvroTaskCanceled>(message)
//    private fun toAvroMessage(message: TaskCompleted) = convertJson<AvroTaskCompleted>(message)
//    private fun toAvroMessage(message: TaskDispatched) = convertJson<AvroTaskDispatched>(message)
//    private fun toAvroMessage(message: WorkflowCanceled) = convertJson<AvroWorkflowCanceled>(message)
//    private fun toAvroMessage(message: WorkflowCompleted) = convertJson<AvroWorkflowCompleted>(message)
//
//    fun toAvroMessage(message: DecisionDispatched) = AvroDecisionDispatched.newBuilder().apply {
//        decisionId = message.workflowTaskId.id
//        workflowId = message.workflowId.id
//        workflowName = message.workflowName.name
//        decisionInput = toAvroDecisionInput(message.workflowTaskInput)
//    }.build()
//
//    /**
//     *  Decision Input
//     */
//
//    fun toAvroDecisionInput(obj: WorkflowTaskInput): AvroDecisionInput = AvroDecisionInput.newBuilder().apply {
//        workflowName = obj.workflowName.name
//        branches = obj.branches.map { toAvroBranch(it) }
//        store = convertJson(obj.store)
//    }.build()
//
//    fun fromAvroDecisionInput(avro: AvroDecisionInput) = WorkflowTaskInput(
//        workflowName = WorkflowName(avro.workflowName),
//        workflowId = WorkflowId(avro.workflowId),
//        branches = avro.branches.map { fromAvroBranch(it) },
//        store = convertJson(avro.store)
//    )
//
//    /**
//     *  Decision Output
//     */
//
// //    fun toAvroDecisionOutput(obj: DecisionOutput): AvroDecisionOutput = AvroDecisionOutput.newBuilder().apply {
// //        branches = obj.branches.map { toAvroBranch(it) }
// //    }.build()
// //
// //    fun fromAvroDecisionOutput(avro: AvroDecisionOutput) = DecisionOuput(
// //        branches = avro.branches.map { fromAvroBranch(it) },
// //        store = convertJson(avro.store)
// //    )
//
//    /**
//     *  StepCriteria
//     */
//
//    fun toAvroStepCriterion(obj: Step): AvroStepCriterion = when (obj) {
//        is Step.Id -> AvroStepCriterion.newBuilder().apply {
//            type = AvroStepCriterionType.ID
//            commandId = obj.commandId.id
//            commandStatus = obj.commandStatus
//        }.build()
//        is Step.Or -> AvroStepCriterion.newBuilder().apply {
//            type = AvroStepCriterionType.OR
//            commandCriteria = obj.commands.map { toAvroStepCriterion(it) }
//        }.build()
//        is Step.And -> AvroStepCriterion.newBuilder().apply {
//            type = AvroStepCriterionType.AND
//            commandCriteria = obj.commands.map { toAvroStepCriterion(it) }
//        }.build()
//    }
//
//    fun fromAvroStepCriterion(avro: AvroStepCriterion): Step = when (avro.type) {
//        AvroStepCriterionType.ID -> Step.Id(
//            commandId = CommandId(avro.commandId),
//            commandStatus = avro.commandStatus
//        )
//        AvroStepCriterionType.OR -> Step.Or(
//            commands = avro.commandCriteria.map { fromAvroStepCriterion(it) }
//        )
//        AvroStepCriterionType.AND -> Step.And(
//            commands = avro.commandCriteria.map { fromAvroStepCriterion(it) }
//        )
//        null -> throw Exception("this should not happen")
//    }
//
//    /**
//     *  Steps
//     */
//    fun toAvroStep(obj: PastStep): AvroStep = AvroStep.newBuilder().apply {
//        stepHash = obj.stepHash.hash
//        criterion = toAvroStepCriterion(obj.step)
//        propertiesAfterCompletion = convertJson(obj.propertiesAfterCompletion)
//    }.build()
//
//    fun fromAvroStep(avro: AvroStep) = PastStep(
//        stepPosition = InstructionPosition("1"), // TODO
//        stepHash = StepHash(avro.stepHash),
//        step = fromAvroStepCriterion(avro.criterion),
//        propertiesAfterCompletion = convertJson(avro.propertiesAfterCompletion)
//    )
//
//    /**
//     *  Branches
//     */
//
//    fun toAvroBranch(obj: Branch): AvroBranch = AvroBranch.newBuilder().apply {
//        branchId = obj.branchId.id
//        methodName = convertJson(obj.workflowMethod)
//        methodInput = convertJson(obj.workflowMethodInput)
//        propertiesAtStart = toAvroProperties(obj.propertiesAtStart)
//        steps = obj.pastInstructions.map { toAvroStep(it) }
//        commands = obj.pastInstructions.map { convertJson<AvroCommand>(it) }
//    }.build()
//
//    fun fromAvroBranch(avro: AvroBranch) = Branch(
//        branchId = BranchId(avro.branchId),
//        workflowMethod = convertJson(avro.methodName),
//        workflowMethodInput = convertJson(avro.methodInput),
//        propertiesAtStart = fromAvroProperties(avro.propertiesAtStart),
//        pastInstructions = avro.steps.map { fromAvroStep(it) },
//    )
//
//    /**
//     *  Properties
//     */
//
//    fun toAvroProperties(obj: Properties): Map<String, String> = convertJson(obj)
//
//    fun fromAvroProperties(avro: Map<String, String>) = Properties(
//        avro
//            .mapValues { PropertyHash(it.value) }
//            .mapKeys { PropertyName(it.key) }
//    )
//
//    /**
//     *  Mapping function by Json serialization/deserialization
//     */
//    inline fun <reified T : Any> convertJson(from: Any?): T = Json.parse(Json.stringify(from))
}
