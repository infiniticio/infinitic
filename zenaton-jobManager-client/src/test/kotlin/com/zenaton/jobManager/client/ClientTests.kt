package com.zenaton.jobManager.client

import com.zenaton.common.data.SerializedData
import com.zenaton.common.data.interfaces.IdInterface
import com.zenaton.jobManager.common.data.JobId
import com.zenaton.jobManager.common.data.JobInput
import com.zenaton.jobManager.common.data.JobMeta
import com.zenaton.jobManager.common.data.JobName
import com.zenaton.jobManager.common.messages.DispatchJob
import com.zenaton.jobManager.common.messages.ForJobEngineMessage
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot

class ClientTests : StringSpec({
    val dispatcher = mockk<Dispatcher>()
    val slot = slot<ForJobEngineMessage>()
    every { dispatcher.toJobEngine(capture(slot)) } just Runs
    val client = Client()
    client.dispatcher = dispatcher

    beforeTest {
        slot.clear()
    }

    "Should be able to dispatch method without parameter" {
        // when
        val job = client.dispatch<FakeTask> { m1() }
        // then
        slot.isCaptured shouldBe true
        val msg = slot.captured
        msg shouldBe DispatchJob(
            jobId = job.jobId,
            jobInput = JobInput(listOf()),
            jobName = JobName("${FakeTask::class.java.name}::m1"),
            jobMeta = JobMeta.forParameterTypes(listOf())
        )
    }

    "Should be able to dispatch a method with a primitive as parameter" {
        // when
        val job = client.dispatch<FakeTask> { m1(0) }
        // then
        slot.isCaptured shouldBe true
        val msg = slot.captured
        msg shouldBe DispatchJob(
            jobId = job.jobId,
            jobInput = JobInput(listOf(SerializedData.from(0))),
            jobName = JobName("${FakeTask::class.java.name}::m1"),
            jobMeta = JobMeta.forParameterTypes(listOf(Int::class.java.name))
        )
    }

    "Should be able to dispatch a method with multiple definition" {
        // when
        val job = client.dispatch<FakeTask> { m1("a") }
        // then
        slot.isCaptured shouldBe true
        val msg = slot.captured
        msg shouldBe DispatchJob(
            jobId = job.jobId,
            jobInput = JobInput(listOf(SerializedData.from("a"))),
            jobName = JobName("${FakeTask::class.java.name}::m1"),
            jobMeta = JobMeta.forParameterTypes(listOf(String::class.java.name))
        )
    }

    "Should be able to dispatch a method with multiple parameters" {
        // when
        val job = client.dispatch<FakeTask> { m1(0, "a") }
        // then
        slot.isCaptured shouldBe true
        val msg = slot.captured
        msg shouldBe DispatchJob(
            jobId = job.jobId,
            jobInput = JobInput(listOf(SerializedData.from(0), SerializedData.from("a"))),
            jobName = JobName("${FakeTask::class.java.name}::m1"),
            jobMeta = JobMeta.forParameterTypes(listOf(Int::class.java.name, String::class.java.name))
        )
    }

    "Should be able to dispatch a method with an interface as parameter" {
        // when
        val jobId = JobId()
        val job = client.dispatch<FakeTask> { m1(jobId) }
        // then
        slot.isCaptured shouldBe true
        val msg = slot.captured
        msg shouldBe DispatchJob(
            jobId = job.jobId,
            jobInput = JobInput(listOf(SerializedData.from(jobId))),
            jobName = JobName("${FakeTask::class.java.name}::m1"),
            jobMeta = JobMeta.forParameterTypes(listOf(IdInterface::class.java.name))
        )
    }
})

private interface FakeTask {
    fun m1()
    fun m1(i: Int): String
    fun m1(str: String): Any?
    fun m1(p1: Int, p2: String): String
    fun m1(id: IdInterface): String
}
