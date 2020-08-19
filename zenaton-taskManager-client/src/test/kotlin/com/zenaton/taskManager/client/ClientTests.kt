package com.zenaton.taskManager.client

import com.zenaton.common.data.SerializedData
import com.zenaton.common.data.interfaces.IdInterface
import com.zenaton.taskManager.common.data.TaskId
import com.zenaton.taskManager.common.data.TaskInput
import com.zenaton.taskManager.common.data.TaskMeta
import com.zenaton.taskManager.common.data.TaskName
import com.zenaton.taskManager.common.data.TaskOptions
import com.zenaton.taskManager.common.messages.DispatchTask
import com.zenaton.taskManager.common.messages.ForTaskEngineMessage
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot

class ClientTests : StringSpec({
    val dispatcher = mockk<Dispatcher>()
    val slot = slot<ForTaskEngineMessage>()
    every { dispatcher.toTaskEngine(capture(slot)) } just Runs
    val client = Client()
    client.dispatcher = dispatcher

    beforeTest {
        slot.clear()
    }

    "Should be able to dispatch method without parameter" {
        // when
        val job = client.dispatchTask<FakeTask> { m1() }
        // then
        slot.isCaptured shouldBe true
        val msg = slot.captured
        msg shouldBe DispatchTask(
            taskId = job.taskId,
            taskInput = TaskInput(listOf()),
            taskName = TaskName("${FakeTask::class.java.name}::m1"),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta().setParameterTypes(listOf())
        )
    }

    "Should be able to dispatch a method with a primitive as parameter" {
        // when
        val job = client.dispatchTask<FakeTask> { m1(0) }
        // then
        slot.isCaptured shouldBe true
        val msg = slot.captured
        msg shouldBe DispatchTask(
            taskId = job.taskId,
            taskInput = TaskInput(listOf(SerializedData.from(0))),
            taskName = TaskName("${FakeTask::class.java.name}::m1"),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta().setParameterTypes(listOf(Int::class.java.name))
        )
    }

    "Should be able to dispatch a method with multiple definition" {
        // when
        val job = client.dispatchTask<FakeTask> { m1("a") }
        // then
        slot.isCaptured shouldBe true
        val msg = slot.captured
        msg shouldBe DispatchTask(
            taskId = job.taskId,
            taskInput = TaskInput(listOf(SerializedData.from("a"))),
            taskName = TaskName("${FakeTask::class.java.name}::m1"),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta().setParameterTypes(listOf(String::class.java.name))
        )
    }

    "Should be able to dispatch a method with multiple parameters" {
        // when
        val job = client.dispatchTask<FakeTask> { m1(0, "a") }
        // then
        slot.isCaptured shouldBe true
        val msg = slot.captured
        msg shouldBe DispatchTask(
            taskId = job.taskId,
            taskInput = TaskInput(listOf(SerializedData.from(0), SerializedData.from("a"))),
            taskName = TaskName("${FakeTask::class.java.name}::m1"),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta().setParameterTypes(listOf(Int::class.java.name, String::class.java.name))
        )
    }

    "Should be able to dispatch a method with an interface as parameter" {
        // when
        val jobId = TaskId()
        val job = client.dispatchTask<FakeTask> { m1(jobId) }
        // then
        slot.isCaptured shouldBe true
        val msg = slot.captured

        msg shouldBe DispatchTask(
            taskId = job.taskId,
            taskInput = TaskInput(listOf(SerializedData.from(jobId))),
            taskName = TaskName("${FakeTask::class.java.name}::m1"),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta().setParameterTypes(listOf(IdInterface::class.java.name))
        )
    }

    // TODO: add tests for cancel method

    // TODO: add tests for retry method

    // TODO: add tests for options

    // TODO: add tests for meta
})

private interface FakeTask {
    fun m1()
    fun m1(i: Int): String
    fun m1(str: String): Any?
    fun m1(p1: Int, p2: String): String
    fun m1(id: IdInterface): String
}
