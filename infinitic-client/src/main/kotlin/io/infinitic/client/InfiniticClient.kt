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

package io.infinitic.client

import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.tasks.engine.SendToTaskEngine
import io.infinitic.common.tasks.tags.SendToTaskTagEngine
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.tags.SendToWorkflowTagEngine
import kotlinx.coroutines.CoroutineScope

class InfiniticClient(
    override val scope: CoroutineScope,
    override val clientName: ClientName,
    override val sendToTaskTagEngine: SendToTaskTagEngine,
    override val sendToTaskEngine: SendToTaskEngine,
    override val sendToWorkflowTagEngine: SendToWorkflowTagEngine,
    override val sendToWorkflowEngine: SendToWorkflowEngine,
    val closeFn: () -> Unit
) : Client() {
    override fun close() { closeFn() }
}