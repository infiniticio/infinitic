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

import io.infinitic.pulsar.config.PulsarConfig;
import io.infinitic.storage.config.PostgresConfig;
import io.infinitic.storage.config.StorageConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;


class JavaInfiniticWorkerTests {

    @Test
    public void canCreateWorkerProgrammaticallyWithPulsar() {
        assertDoesNotThrow(() -> {
            PulsarConfig pulsar = PulsarConfig.builder()
                    .setBrokerServiceUrl("pulsar://localhost:6650")
                    .setWebServiceUrl("http://localhost:8080")
                    .setTenant("infinitic")
                    .setNamespace("dev")
                    .build();

            StorageConfig storage = StorageConfig.builder()
                    .setDatabase(
                            PostgresConfig.builder()
                                    .setHost("localhost")
                                    .setPort(5432)
                                    .setUserName("postgres")
                                    .setPassword("password")
                                    .build()
                    ).build();


            try (InfiniticWorker worker = InfiniticWorker.builder()
                    .setPulsar(pulsar)
                    .setStorage(storage)
                    .build()
            ) {
            }
        });
    }
}
