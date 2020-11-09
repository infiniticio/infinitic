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

package io.infinitic.engine.pulsar.main

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import io.infinitic.engine.tasks.storage.TaskStateKeyValueStorage
import io.infinitic.engine.workflows.storages.AvroKeyValueWorkflowStateStorage
import io.infinitic.messaging.api.dispatcher.AvroDispatcher
import io.infinitic.messaging.pulsar.PulsarTransport
import io.infinitic.storage.inmemory.inMemory
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
        val storage = inMemory()
        val taskStateStorage = TaskStateKeyValueStorage(storage)
        val workflowStateStorage = AvroKeyValueWorkflowStateStorage(storage)

        // FIXME: This must be configurable using a configuration file or command line arguments
        val dispatcher = AvroDispatcher(PulsarTransport.forPulsarClient(client))

        val application = Application.create(client, taskStateStorage, workflowStateStorage, dispatcher)
        application.run()
    }
}
