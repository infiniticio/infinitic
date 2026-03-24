/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, "Sell" means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.workflows.tag.storage

import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.storage.databases.inMemory.InMemoryKeySetStorage
import io.infinitic.storage.keySet.KeySetPage
import io.infinitic.storage.keySet.KeySetStorage
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
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

      test("getWorkflowIdsPage should page deterministically with in-memory storage") {
        val workflowName = WorkflowName("test-name")
        val workflowTag = WorkflowTag("test-tag")
        val storage = BinaryWorkflowTagStorage(InMemoryKeySetStorage())
        val workflowIds = listOf(
            WorkflowId("workflow-b"),
            WorkflowId("workflow-a"),
            WorkflowId("workflow-c"),
        )

        workflowIds.forEach { storage.addWorkflowId(workflowTag, workflowName, it) }

        val firstPage = storage.getWorkflowIdsPage(workflowTag, workflowName, limit = 2)
        firstPage.workflowIds shouldContainExactly listOf(
            WorkflowId("workflow-a"),
            WorkflowId("workflow-b"),
        )
        firstPage.nextCursor shouldBe "2"

        val secondPage = storage.getWorkflowIdsPage(
            workflowTag,
            workflowName,
            limit = 2,
            cursor = firstPage.nextCursor,
        )
        secondPage.workflowIds shouldContainExactly listOf(WorkflowId("workflow-c"))
        secondPage.nextCursor shouldBe null
      }

      test("getWorkflowIdsPage should deduplicate logical duplicates across backend pages") {
        val workflowName = WorkflowName("test-name")
        val workflowTag = WorkflowTag("test-tag")
        val id1 = WorkflowId("workflow-a")
        val id2 = WorkflowId("workflow-b")
        val keySetStorage = object : KeySetStorage {
          override suspend fun get(key: String) = emptySet<ByteArray>()

          override suspend fun getPage(key: String, limit: Int, cursor: String?) = when (cursor) {
            null -> KeySetPage(
                values = listOf(id1.toString().toByteArray()),
                nextCursor = "cursor-1",
            )

            "cursor-1" -> KeySetPage(
                values = listOf(id1.toString().toByteArray(), id2.toString().toByteArray()),
                nextCursor = "cursor-2",
            )

            else -> KeySetPage(values = emptyList(), nextCursor = null)
          }

          override suspend fun add(key: String, value: ByteArray) = Unit

          override suspend fun remove(key: String, value: ByteArray) = Unit

          override suspend fun get(keys: Set<String>) = emptyMap<String, Set<ByteArray>>()

          override suspend fun update(
            add: Map<String, Set<ByteArray>>,
            remove: Map<String, Set<ByteArray>>,
          ) = Unit

          override fun flush() = Unit

          override fun close() = Unit
        }
        val storage = BinaryWorkflowTagStorage(keySetStorage)

        val page = storage.getWorkflowIdsPage(workflowTag, workflowName, limit = 2)

        page.workflowIds shouldContainExactly listOf(id1, id2)
        page.nextCursor shouldBe "cursor-2"
      }
    },
)
