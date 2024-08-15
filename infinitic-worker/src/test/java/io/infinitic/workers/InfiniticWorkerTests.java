package io.infinitic.workers;

import io.infinitic.common.transport.InfiniticConsumerAsync;
import io.infinitic.common.transport.InfiniticProducerAsync;
import io.infinitic.pulsar.PulsarInfiniticConsumerAsync;
import io.infinitic.pulsar.PulsarInfiniticProducerAsync;
import io.infinitic.pulsar.config.PulsarConfig;
import io.infinitic.pulsar.config.auth.AuthenticationTokenConfig;
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

        WorkerConfig workerConfig = WorkerConfig.builder()
                .pulsar(pulsar)
                .build();

        try (InfiniticWorker worker = InfiniticWorker.fromConfig(workerConfig)) {
            InfiniticConsumerAsync c = worker.getConsumerAsync();
            InfiniticProducerAsync p = worker.getProducerAsync();
            assertInstanceOf(PulsarInfiniticConsumerAsync.class, c);
            assertInstanceOf(PulsarInfiniticProducerAsync.class, p);
        }
    }
}
