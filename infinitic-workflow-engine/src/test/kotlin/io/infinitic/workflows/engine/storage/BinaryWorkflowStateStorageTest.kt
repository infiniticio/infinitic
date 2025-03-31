package io.infinitic.workflows.engine.storage

import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk

class BinaryWorkflowStateStorageTest : FunSpec(
    {
      test("getWorkflowStateKey should maintain consistent format") {
        // Given
        val workflowId = WorkflowId("test-id")
        val storage = BinaryWorkflowStateStorage(mockk())

        // When
        val key = storage.getWorkflowStateKey(workflowId)

        // Then
        key shouldBe "workflow.state.test-id"
      }
    },
)
