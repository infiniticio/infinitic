package com.zenaton.engine.topics.workflows

import com.zenaton.engine.data.decisions.DecisionId
import com.zenaton.engine.data.delays.DelayId
import com.zenaton.engine.data.tasks.TaskId
import com.zenaton.engine.data.tasks.TaskOutput
import com.zenaton.engine.data.types.DateTime
import com.zenaton.engine.data.workflows.WorkflowData
import com.zenaton.engine.data.workflows.WorkflowId
import com.zenaton.engine.data.workflows.WorkflowName
import com.zenaton.engine.data.workflows.WorkflowOutput
import com.zenaton.pulsar.workflows.PulsarMessage
import com.zenaton.pulsar.workflows.serializers.MessageConverter
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string

class WorkflowMessageTests : StringSpec({
    "DispatchedWorkflow should convert to PulsarMessage and back" {
        // given
        val msgIn = WorkflowDispatched(
            workflowId = WorkflowId(),
            workflowName = WorkflowName(Arb.string(1).toString()),
            workflowData = WorkflowData(Arb.string(1).toString().toByteArray()),
            dispatchedAt = DateTime()
        )
        // when
        val msgPulsar: PulsarMessage = MessageConverter.toPulsar(msgIn)
        val msgOut = MessageConverter.fromPulsar(msgPulsar) as WorkflowDispatched
        // then
        msgIn shouldBe msgOut
    }

    "WorkflowCompleted should convert to PulsarMessage and back" {
        // given
        val msgIn = WorkflowCompleted(
            workflowId = WorkflowId(),
            workflowOutput = WorkflowOutput(Arb.string(1).toString().toByteArray()),
            dispatchedAt = DateTime()
        )
        // when
        val msgPulsar: PulsarMessage = MessageConverter.toPulsar(msgIn)
        val msgOut = MessageConverter.fromPulsar(msgPulsar) as WorkflowCompleted
        // then
        msgIn shouldBe msgOut
    }

    "TaskCompleted should convert to PulsarMessage and back" {
        // given
        val msgIn = TaskCompleted(
            workflowId = WorkflowId(),
            taskId = TaskId(),
            taskOutput = TaskOutput(Arb.string(1).toString().toByteArray())
        )
        // when
        val msgPulsar: PulsarMessage = MessageConverter.toPulsar(msgIn)
        val msgOut = MessageConverter.fromPulsar(msgPulsar) as TaskCompleted
        // then
        msgIn shouldBe msgOut
    }

    "DelayCompleted should convert to PulsarMessage and back" {
        // given
        val msgIn = DelayCompleted(
            workflowId = WorkflowId(),
            delayId = DelayId()
        )
        // when
        val msgPulsar: PulsarMessage = MessageConverter.toPulsar(msgIn)
        val msgOut = MessageConverter.fromPulsar(msgPulsar) as DelayCompleted
        // then
        msgIn shouldBe msgOut
    }

    "DecisionCompleted should convert to PulsarMessage and back" {
        // given
        val msgIn = DecisionCompleted(
            workflowId = WorkflowId(),
            decisionId = DecisionId()
        )
        // when
        val msgPulsar: PulsarMessage = MessageConverter.toPulsar(msgIn)
        val msgOut = MessageConverter.fromPulsar(msgPulsar) as DecisionCompleted
        // then
        msgIn shouldBe msgOut
    }
})
