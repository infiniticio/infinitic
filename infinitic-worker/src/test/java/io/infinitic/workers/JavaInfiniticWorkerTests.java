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

import io.infinitic.common.workers.config.WithExponentialBackoffRetry;
import io.infinitic.events.config.EventListenerConfig;
import io.infinitic.storage.config.PostgresStorageConfig;
import io.infinitic.storage.config.StorageConfig;
import io.infinitic.transport.config.PulsarTransportConfig;
import io.infinitic.workers.config.*;
import io.infinitic.workflows.Workflow;
import io.infinitic.workflows.WorkflowCheckMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;


class JavaInfiniticWorkerTests {

    @Test
    public void canCreateWorkerThroughBuilders() {
        assertDoesNotThrow(() -> {
            PulsarTransportConfig transport = PulsarTransportConfig.builder()
                    .setBrokerServiceUrl("pulsar://localhost:6650")
                    .setWebServiceUrl("http://localhost:8080")
                    .setTenant("infinitic")
                    .setNamespace("dev")
                    .build();

            StorageConfig storage = PostgresStorageConfig.builder()
                    .setHost("localhost")
                    .setPort(5432)
                    .setUsername("postgres")
                    .setPassword("password")
                    .build();


            EventListenerConfig eventListener = EventListenerConfig.builder()
                    .setListener(new TestEventListener())
                    .allowServices("service1")
                    .allowServices(ServiceA.class)
                    .disallowServices("service2")
                    .disallowServices(ServiceA.class)
                    .allowWorkflows("workflow1")
                    .allowWorkflows(ServiceA.class)
                    .disallowWorkflows("workflow2")
                    .disallowWorkflows(ServiceA.class)
                    .setConcurrency(7)
                    .build();

            ServiceExecutorConfig serviceExecutor = ServiceExecutorConfig.builder()
                    .setServiceName("service1")
                    .setConcurrency(3)
                    .setFactory(ServiceA::new)
                    .withRetry(new WithExponentialBackoffRetry())
                    .setTimeoutSeconds(4.0)
                    .build();

            ServiceTagEngineConfig serviceTagEngine = ServiceTagEngineConfig.builder()
                    .setConcurrency(6)
                    .setServiceName("service1")
                    .build();

            WorkflowExecutorConfig workflowExecutor = WorkflowExecutorConfig.builder()
                    .setConcurrency(5)
                    .setTimeoutSeconds(3.0)
                    .addFactory(WorkflowA::new)
                    .setWorkflowName("workflow1")
                    .setCheckMode(WorkflowCheckMode.simple)
                    .build();

            WorkflowTagEngineConfig workflowTagEngine = WorkflowTagEngineConfig.builder()
                    .setConcurrency(4)
                    .setWorkflowName("workflow1")
                    .setStorage(storage)
                    .build();

            WorkflowStateEngineConfig workflowStateEngine = WorkflowStateEngineConfig.builder()
                    .setConcurrency(6)
                    .setWorkflowName("workflow1")
                    .build();

            try (InfiniticWorker worker = InfiniticWorker.builder()
                    .setTransport(transport)
                    .setStorage(storage)
                    .setEventListener(eventListener)
                    .addServiceExecutor(serviceExecutor)
                    .addServiceTagEngine(serviceTagEngine)
                    .addWorkflowExecutor(workflowExecutor)
                    .addWorkflowTagEngine(workflowTagEngine)
                    .addWorkflowStateEngine(workflowStateEngine)
                    .build()
            ) {
                // ok
            }
        });
    }
}

class ServiceA {
}

class WorkflowA extends Workflow {
}
