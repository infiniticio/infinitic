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

package io.infinitic.tests.versioning

import io.infinitic.clients.InfiniticClient
import io.infinitic.workers.InfiniticWorker
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

internal class VersionedWorkflowTests : StringSpec({

    // each test should not be longer than 10s
    timeout = 10000

    val worker = autoClose(InfiniticWorker.fromConfigResource("/pulsar.yml"))
    val client = autoClose(InfiniticClient.fromConfigResource("/pulsar.yml"))

    val versionedWorkflow = client.newWorkflow(VersionedWorkflow::class.java)

    beforeSpec {
        worker.startAsync()
    }

    beforeTest {
        worker.registry.flush()
    }

    "Dispatch workflow should use last version" {
        versionedWorkflow.name() shouldBe VersionedWorkflowImpl_1::class.java.name
    }
})
