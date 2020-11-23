// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.engines.pulsar.main

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import io.infinitic.engines.monitoringGlobal.storage.MonitoringGlobalStateKeyValueStorage
import io.infinitic.engines.monitoringPerName.storage.MonitoringPerNameStateKeyValueStorage
import io.infinitic.engines.tasks.storage.TaskStateKeyValueStorage
import io.infinitic.engines.workflows.storage.WorkflowStateKeyValueStorage
import io.infinitic.messaging.pulsar.extensions.messageBuilder
import io.infinitic.messaging.pulsar.senders.getSendToMonitoringGlobal
import io.infinitic.messaging.pulsar.senders.getSendToMonitoringPerName
import io.infinitic.messaging.pulsar.senders.getSendToTaskEngine
import io.infinitic.messaging.pulsar.senders.getSendToWorkers
import io.infinitic.messaging.pulsar.senders.getSendToWorkflowEngine
import io.infinitic.storage.inMemory.InMemoryStorage
import kotlinx.coroutines.runBlocking
import org.apache.pulsar.client.api.PulsarClient

fun main(args: Array<String>) = mainBody {
    runBlocking {
        val parsedArgs = ArgParser(args).parseInto(::ApplicationArgs)

        // FIXME: This must be configurable using a configuration file or command line arguments
        val client: PulsarClient = PulsarClient.builder()
            .serviceUrl(parsedArgs.pulsarUrl)
            .build()

        // FIXME: This must be configurable using a configuration file or command line arguments
        val workflowStateStorage = WorkflowStateKeyValueStorage(InMemoryStorage())
        val taskStateStorage = TaskStateKeyValueStorage(InMemoryStorage())
        val monitoringGlobalStateStorage = MonitoringGlobalStateKeyValueStorage(InMemoryStorage())
        val monitoringPerNameStateStorage = MonitoringPerNameStateKeyValueStorage(InMemoryStorage())

        // FIXME: This must be configurable using a configuration file or command line arguments
        val application = Application(
            client,
            workflowStateStorage,
            taskStateStorage,
            monitoringPerNameStateStorage,
            monitoringGlobalStateStorage,
            getSendToWorkflowEngine(client.messageBuilder()),
            getSendToTaskEngine(client.messageBuilder()),
            getSendToMonitoringPerName(client.messageBuilder()),
            getSendToMonitoringGlobal(client.messageBuilder()),
            getSendToWorkers(client.messageBuilder())
        )
        application.run()
    }
}
