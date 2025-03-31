package io.infinitic.workflows.tag.storage

import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk

class BinaryWorkflowTagStorageTest : FunSpec(
    {
      test("getWorkflowStateKey should maintain consistent format") {
        // Given
        val workflowName = WorkflowName("test-name")
        val workflowTag = WorkflowTag("test-tag")
        val storage = BinaryWorkflowTagStorage(mockk())

        // When
        val key = storage.getTagSetIdsKey(workflowTag, workflowName)

        // Then
        key shouldBe "workflow:test-name|tag:test-tag|setIds"
      }
    },
)
