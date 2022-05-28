/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.common.workflows.data.workflowTasks

import com.github.avrokotlin.avro4k.AvroDefault
import io.infinitic.common.data.ReturnValue
import io.infinitic.common.workflows.data.commands.PastCommand
import io.infinitic.common.workflows.data.properties.PropertyName
import io.infinitic.common.workflows.data.properties.PropertyValue
import io.infinitic.common.workflows.data.steps.NewStep
import kotlinx.serialization.Serializable

@Serializable
data class WorkflowTaskReturnValue(
    @AvroDefault("0.9.7")
    val version: String = io.infinitic.version,
    val newCommands: List<PastCommand>,
    val newStep: NewStep?,
    val properties: Map<PropertyName, PropertyValue>,
    val methodReturnValue: ReturnValue?
)
