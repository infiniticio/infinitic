package io.infinitic.client

import io.infinitic.client.samples.FakeTask
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.mockk.slot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.random.Random

fun main() = runBlocking {
    val taskSlot = slot<TaskEngineMessage>()
    val workflowSlot = slot<WorkflowEngineMessage>()
    val client = InfiniticClient(MockClientOutput(taskSlot, workflowSlot))

    val fake = client.task(FakeTask::class.java)

    println(client.async(fake) { m1() })

//    val h = ResponseHandler(this)
//    launch {
//        try {
//            val result = h.sync("1")
//            println("result: $result")
//        } catch (e: Exception) {
//            println(e)
//        }
//    }

    Unit
}

class ResponseHandler(
    private val scope: CoroutineScope,
) {
    private val responseFlow = MutableSharedFlow<String>(replay = 0)

    suspend fun sync(callId: String): String {
        var output: String? = null
        val job = scope.launch {
            // Listen for all responses
            withTimeout(4000L) {
                responseFlow.collect {
                    if (it == callId) {
                        output = it
                        this.cancel()
                    }
                }
            }
        }
        // Simulate queue behavior
        useQueue(callId)

        // wait for receiving the asynchronous response
        job.join()

        return output ?: throw Exception("Timeout")
    }

    private suspend fun useQueue(callId: String) {
        // Simulate sending to queue
        println("sending $callId")
        val delay = Random.nextLong(0, 5000)
        delay(delay)
        // Simulate receiving from queue
        println("receiving $callId after $delay")
        responseFlow.emit(callId)
    }
}
