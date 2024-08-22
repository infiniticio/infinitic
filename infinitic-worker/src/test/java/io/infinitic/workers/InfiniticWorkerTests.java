/**
 * "Commons Clause" License Condition v1.0
 * <p>
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 * <p>
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 * <p>
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 * <p>
 * Software: Infinitic
 * <p>
 * License: MIT License (https://opensource.org/licenses/MIT)
 * <p>
 * Licensor: infinitic.io
 */
package io.infinitic.workers;

import io.infinitic.common.transport.InfiniticConsumerAsync;
import io.infinitic.common.transport.InfiniticProducerAsync;
import io.infinitic.pulsar.PulsarInfiniticConsumerAsync;
import io.infinitic.pulsar.PulsarInfiniticProducerAsync;
import io.infinitic.pulsar.config.PulsarConfig;
import io.infinitic.storage.config.PostgresConfig;
import io.infinitic.storage.config.StorageConfig;
import io.infinitic.workers.config.WorkerConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;


class InfiniticWorkerTests {

    @Test
    public void canCreateWorkerProgrammaticallyWithPulsar() {
        PulsarConfig pulsar = PulsarConfig.builder()
                .brokerServiceUrl("pulsar://localhost:6650")
                .webServiceUrl("http://localhost:8080")
                .tenant("infinitic")
                .namespace("dev")
                .build();

        StorageConfig storage = StorageConfig.builder()
                .postgres(PostgresConfig
                        .builder()
                        .build()
                ).build();


        WorkerConfig workerConfig = WorkerConfig.builder()
                .pulsar(pulsar)
                .storage(storage)
                .build();

        try (InfiniticWorker worker = InfiniticWorker.fromConfig(workerConfig)) {
            InfiniticConsumerAsync c = worker.getConsumerAsync();
            InfiniticProducerAsync p = worker.getProducerAsync();
            assertInstanceOf(PulsarInfiniticConsumerAsync.class, c);
            assertInstanceOf(PulsarInfiniticProducerAsync.class, p);
        }
    }
}
