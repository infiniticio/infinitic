package com.zenaton.workflowManager.pulsar

import com.zenaton.common.data.SerializedData
import com.zenaton.common.json.Json
import com.zenaton.jobManager.data.JobId
import com.zenaton.workflowManager.avro.AvroConverter
import com.zenaton.workflowManager.data.DecisionInput
import com.zenaton.workflowManager.data.actions.Action
import com.zenaton.workflowManager.data.actions.ActionId
import com.zenaton.workflowManager.data.actions.AvroAction
import com.zenaton.workflowManager.data.branches.AvroBranch
import com.zenaton.workflowManager.data.branches.Branch
import com.zenaton.workflowManager.data.steps.AvroStepCriterion
import com.zenaton.workflowManager.data.steps.Step
import com.zenaton.workflowManager.data.steps.StepCriterion
import com.zenaton.workflowManager.messages.DecisionDispatched
import com.zenaton.workflowManager.pulsar.utils.TestFactory
import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters
import org.jeasy.random.FieldPredicates
import org.jeasy.random.api.Randomizer
import java.nio.ByteBuffer
import kotlin.random.Random
import kotlin.reflect.KClass

fun main() {

    val o = TestFactory.random(DecisionDispatched::class)
    val a = AvroConverter.toWorkflowEngine(o)
    val b = AvroConverter.toWorkflowEngine(o)
    println(a)

    println(a == b)

    val o2 = AvroConverter.fromWorkflowEngine(a)
    println(o2)

    println(o == o2)

}
