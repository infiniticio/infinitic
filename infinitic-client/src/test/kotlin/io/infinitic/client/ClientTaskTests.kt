package io.infinitic.client

import io.infinitic.client.samples.FakeClass
import io.infinitic.client.samples.FakeInterface
import io.infinitic.client.samples.FakeTask
import io.infinitic.client.samples.FakeWorkflow
import io.infinitic.messaging.api.dispatcher.Dispatcher
import io.infinitic.common.taskManager.data.TaskInput
import io.infinitic.common.taskManager.data.TaskMeta
import io.infinitic.common.taskManager.data.TaskName
import io.infinitic.common.taskManager.data.TaskOptions
import io.infinitic.common.taskManager.messages.DispatchTask
import io.infinitic.common.taskManager.messages.ForTaskEngineMessage
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot

class ClientTaskTests : StringSpec({
    val dispatcher = mockk<Dispatcher>()
    val slot = slot<ForTaskEngineMessage>()
    coEvery { dispatcher.toTaskEngine(capture(slot)) } just Runs
    val client = Client(dispatcher)

    beforeTest {
        slot.clear()
    }

    "Should be able to dispatch method without parameter" {
        // when
        val task = client.dispatch(FakeTask::class.java) { m1() }
        // then
        slot.isCaptured shouldBe true
        val msg = slot.captured
        msg shouldBe DispatchTask(
            taskId = task.taskId,
            taskInput = TaskInput(),
            taskName = TaskName("${FakeTask::class.java.name}::m1"),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta().withParameterTypes(listOf())
        )
    }

    "Should be able to dispatch a method with a primitive as parameter" {
        // when
        val task = client.dispatch(FakeTask::class.java) { m1(0) }
        // then
        slot.isCaptured shouldBe true
        val msg = slot.captured
        msg shouldBe DispatchTask(
            taskId = task.taskId,
            taskInput = TaskInput(0),
            taskName = TaskName("${FakeTask::class.java.name}::m1"),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta().withParameterTypes(listOf(Int::class.java.name))
        )
    }

    "Should be able to dispatch a method with null as parameter" {
        // when
        val task = client.dispatch(FakeTask::class.java) { m1(null) }
        // then
        slot.isCaptured shouldBe true
        val msg = slot.captured
        msg shouldBe DispatchTask(
            taskId = task.taskId,
            taskInput = TaskInput(null),
            taskName = TaskName("${FakeTask::class.java.name}::m1"),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta().withParameterTypes(listOf(String::class.java.name))
        )
    }

    "Should be able to dispatch a method with multiple definition" {
        // when
        val task = client.dispatch(FakeTask::class.java) { m1("a") }
        // then
        slot.isCaptured shouldBe true
        val msg = slot.captured
        msg shouldBe DispatchTask(
            taskId = task.taskId,
            taskInput = TaskInput("a"),
            taskName = TaskName("${FakeTask::class.java.name}::m1"),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta().withParameterTypes(listOf(String::class.java.name))
        )
    }

    "Should be able to dispatch a method with multiple parameters" {
        // when
        val task = client.dispatch(FakeTask::class.java) { m1(0, "a") }
        // then
        slot.isCaptured shouldBe true
        val msg = slot.captured
        msg shouldBe DispatchTask(
            taskId = task.taskId,
            taskInput = TaskInput(0, "a"),
            taskName = TaskName("${FakeTask::class.java.name}::m1"),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta().withParameterTypes(listOf(Int::class.java.name, String::class.java.name))
        )
    }

    "Should be able to dispatch a method with an interface as parameter" {
        // when
        val fake = FakeClass()
        val task = client.dispatch(FakeTask::class.java) { m1(fake) }
        // then
        slot.isCaptured shouldBe true
        val msg = slot.captured

        msg shouldBe DispatchTask(
            taskId = task.taskId,
            taskInput = TaskInput(fake),
            taskName = TaskName("${FakeTask::class.java.name}::m1"),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta().withParameterTypes(listOf(FakeInterface::class.java.name))
        )
    }

    "Should be able to dispatch a method with a primitive as return value" {
        // when
        val task = client.dispatch(FakeTask::class.java) { m2() }
        // then
        slot.isCaptured shouldBe true
        val msg = slot.captured

        msg shouldBe DispatchTask(
            taskId = task.taskId,
            taskInput = TaskInput(),
            taskName = TaskName("${FakeTask::class.java.name}::m2"),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta().withParameterTypes(listOf())
        )
    }

    // TODO: add tests for cancel method

    // TODO: add tests for retry method

    // TODO: add tests for options

    // TODO: add tests for meta

    // TODO: add tests for error cases
})
