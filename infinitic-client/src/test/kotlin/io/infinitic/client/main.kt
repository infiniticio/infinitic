package io.infinitic.client

import io.infinitic.client.samples.FakeWorkflow
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.mockk.slot

fun main() {
    val taskSlot = slot<TaskEngineMessage>()
    val workflowSlot = slot<WorkflowEngineMessage>()
    val clientOutput = MockClientOutput(taskSlot, workflowSlot)
    val client = Client(clientOutput)
    clientOutput.client = client
    val fakeWorkflow = client.workflow(FakeWorkflow::class.java, "id")

//    fakeWorkflow.ch.send("")

    client.async(fakeWorkflow) { ch.send("etst") }
}
