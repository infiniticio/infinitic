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
package io.infinitic.common.workflows.engine.state

import io.infinitic.common.data.MessageId
import io.infinitic.common.workers.config.WorkflowVersion
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class WorkflowStatusTest : StringSpec({

  "status should be COMPLETED when no workflow methods exist" {
    val state = WorkflowState(
        lastMessageId = MessageId("test-message-id"),
        workflowId = WorkflowId("test-workflow-id"),
        workflowName = WorkflowName("TestWorkflow"),
        workflowVersion = WorkflowVersion(1),
        workflowTags = setOf(),
        workflowMeta = WorkflowMeta(),
        runningWorkflowTaskId = null,
        runningWorkflowMethodId = null,
        positionInRunningWorkflowMethod = null,
        workflowMethods = mutableListOf()
    )

    WorkflowStatus.from(state) shouldBe WorkflowStatus.COMPLETED
  }

  "convenience data extraction should produce valid JSON" {
    val state = WorkflowState(
        lastMessageId = MessageId("test-message-id"),
        workflowId = WorkflowId("test-workflow-id"),
        workflowName = WorkflowName("TestWorkflow"),
        workflowVersion = WorkflowVersion(1),
        workflowTags = setOf(WorkflowTag("tag1"), WorkflowTag("tag2")),
        workflowMeta = WorkflowMeta(mapOf("key1" to "value1".toByteArray())),
        runningWorkflowTaskId = null,
        runningWorkflowMethodId = null,
        positionInRunningWorkflowMethod = null,
        workflowMethods = mutableListOf()
    )

    val convenienceData = state.toConvenienceData()

    // Verify status
    convenienceData.status shouldBe "COMPLETED"

    // Verify meta is valid JSON
    val metaJson = Json.parseToJsonElement(convenienceData.meta)
    metaJson.toString().contains("key1") shouldBe true

    // Verify tags is valid JSON array
    val tagsJson = Json.parseToJsonElement(convenienceData.tags)
    tagsJson.toString().contains("tag1") shouldBe true
    tagsJson.toString().contains("tag2") shouldBe true
  }
})
