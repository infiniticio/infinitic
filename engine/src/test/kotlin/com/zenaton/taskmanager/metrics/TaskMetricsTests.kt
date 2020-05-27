package com.zenaton.taskmanager.metrics

import com.zenaton.taskmanager.data.TaskId
import com.zenaton.taskmanager.data.TaskName
import com.zenaton.taskmanager.data.TaskStatus
import com.zenaton.taskmanager.messages.metrics.TaskStatusUpdated
import com.zenaton.taskmanager.metrics.state.TaskMetricsStateStorage
import io.kotest.core.spec.style.ShouldSpec
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify

class TaskMetricsTests : ShouldSpec({
    context("TaskMetrics.handle") {
        should("call TaskMetricsStateStorage.updateTaskStatusCountersByName for TaskStatusUpdate message") {
            val stateStorage = mockk<TaskMetricsStateStorage>()
            every { stateStorage.updateTaskStatusCountersByName(any(), any(), any()) } just runs

            val taskMetrics = TaskMetrics()
            taskMetrics.stateStorage = stateStorage

            taskMetrics.handle(TaskStatusUpdated(
                taskId = TaskId(),
                taskName = TaskName("SomeTask"),
                oldStatus = TaskStatus.RUNNING_OK,
                newStatus = TaskStatus.RUNNING_ERROR
            ))

            verify {
                stateStorage.updateTaskStatusCountersByName(TaskName("SomeTask"), TaskStatus.RUNNING_OK, TaskStatus.RUNNING_ERROR)
            }
        }
    }
})
