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

package io.infinitic.pulsar

import io.infinitic.common.data.Name
import io.infinitic.common.storage.keySet.CachedKeySetStorage
import io.infinitic.common.storage.keyValue.CachedKeyValueStorage
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.config.WorkerConfig
import io.infinitic.config.cache.getKeySetCache
import io.infinitic.config.cache.getKeyValueCache
import io.infinitic.config.data.Metrics
import io.infinitic.config.data.TagEngine
import io.infinitic.config.data.Task
import io.infinitic.config.data.TaskEngine
import io.infinitic.config.data.Workflow
import io.infinitic.config.data.WorkflowEngine
import io.infinitic.config.loaders.loadConfigFromFile
import io.infinitic.config.loaders.loadConfigFromResource
import io.infinitic.config.storage.getKeySetStorage
import io.infinitic.config.storage.getKeyValueStorage
import io.infinitic.metrics.perName.engine.storage.BinaryMetricsPerNameStateStorage
import io.infinitic.pulsar.transport.PulsarConsumerFactory
import io.infinitic.pulsar.transport.PulsarOutput
import io.infinitic.pulsar.workers.startPulsarMetricsPerNameEngines
import io.infinitic.pulsar.workers.startPulsarTaskEngines
import io.infinitic.pulsar.workers.startPulsarTaskExecutors
import io.infinitic.pulsar.workers.startPulsarTaskTagEngines
import io.infinitic.pulsar.workers.startPulsarWorkflowEngines
import io.infinitic.pulsar.workers.startPulsarWorkflowTagEngines
import io.infinitic.tags.tasks.storage.BinaryTaskTagStorage
import io.infinitic.tags.workflows.storage.BinaryWorkflowTagStorage
import io.infinitic.tasks.TaskExecutorRegister
import io.infinitic.tasks.engine.storage.BinaryTaskStateStorage
import io.infinitic.tasks.executor.register.TaskExecutorRegisterImpl
import io.infinitic.workflows.engine.storage.BinaryWorkflowStateStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import org.apache.pulsar.client.api.PulsarClient
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors

@Suppress("MemberVisibilityCanBePrivate", "unused")
class InfiniticWorker(
    @JvmField val pulsarClient: PulsarClient,
    @JvmField val config: WorkerConfig
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        /*
        Create InfiniticWorker from a WorkerConfig
        */
        @JvmStatic
        fun fromConfig(config: WorkerConfig): InfiniticWorker {
            // build Pulsar client from config
            val pulsarClient: PulsarClient = PulsarClient
                .builder()
                .serviceUrl(config.pulsar.serviceUrl)
                .build()

            return InfiniticWorker(pulsarClient, config)
        }

        /*
        Create InfiniticWorker from a WorkerConfig loaded from a resource
        */
        @JvmStatic
        fun fromConfigResource(vararg resources: String) =
            fromConfig(loadConfigFromResource(resources.toList()))

        /*
        Create InfiniticWorker from a WorkerConfig loaded from a file
        */
        @JvmStatic
        fun fromConfigFile(vararg files: String) =
            fromConfig(loadConfigFromFile(files.toList()))
    }

    /*
    Close worker
    */
    fun close() = pulsarClient.close()

    /*
    Start worker
    */
    fun start() {
        with(CoroutineScope(Executors.newCachedThreadPool().asCoroutineDispatcher())) {
            logger.info("InfiniticWorker - starting with config {}", config)

            start(pulsarClient, config)
        }
    }

    private fun CoroutineScope.start(
        pulsarClient: PulsarClient,
        config: WorkerConfig
    ) = launch {

        val workerName = getPulsarName(pulsarClient, config.name)
        val tenant = config.pulsar.tenant
        val namespace = config.pulsar.namespace
        val pulsarConsumerFactory = PulsarConsumerFactory(pulsarClient, tenant, namespace)
        val pulsarOutput = PulsarOutput.from(pulsarClient, tenant, namespace, workerName)

        val taskExecutorRegister = TaskExecutorRegisterImpl()

        for (workflow in config.workflows) {
            val workflowName = WorkflowName(workflow.name)
            println("Workflow $workflowName:")

            // starting engines managing tags of workflows
            workflow.tagEngine?.let {
                startWorkflowTagEngines(workflowName, workerName, it, pulsarConsumerFactory, pulsarOutput)
            }

            // starting engines managing workflowTasks
            workflow.taskEngine?.let {
                startTaskEngines(workflowName, workerName, it, pulsarConsumerFactory, pulsarOutput)
            }

            // starting engines managing workflows
            workflow.workflowEngine?.let {
                startWorkflowEngines(workflowName, workerName, it, pulsarConsumerFactory, pulsarOutput)
            }

            // starting task executors running workflows tasks
            workflow.`class`?.let {
                startWorkflowTaskExecutors(workflowName, workerName, workflow, taskExecutorRegister, pulsarConsumerFactory, pulsarOutput)
            }
        }

        for (task in config.tasks) {
            val taskName = TaskName(task.name)
            println("Task $taskName:")

            // starting engines managing tags of tasks
            task.tagEngine?.let {
                startTaskTagEngines(taskName, workerName, it, pulsarConsumerFactory, pulsarOutput)
            }

            // starting engines managing tasks
            task.taskEngine?.let {
                startTaskEngines(taskName, workerName, it, pulsarConsumerFactory, pulsarOutput)
            }

            // starting task executors running tasks
            task.`class`?.let {
                startTaskExecutors(taskName, workerName, task, taskExecutorRegister, pulsarConsumerFactory, pulsarOutput)
            }

            task.metrics?.let {
                startMetricsPerNameEngines(taskName, workerName, it, pulsarConsumerFactory, pulsarOutput)
            }
        }

        println("Worker \"$workerName\" ready")
    }

    private fun CoroutineScope.startTaskTagEngines(
        taskName: TaskName,
        consumerName: String,
        tagEngine: TagEngine,
        pulsarConsumerFactory: PulsarConsumerFactory,
        pulsarOutput: PulsarOutput
    ) {
        println(
            "- tag engine".padEnd(25) + ": (" +
                "instances: ${tagEngine.concurrencyOrDefault} ".padEnd(15) +
                ", storage: ${tagEngine.stateStorage}".padEnd(20) +
                ", cache: ${tagEngine.stateCacheOrDefault})".padEnd(20)
        )
        startPulsarTaskTagEngines(
            taskName,
            consumerName,
            tagEngine.concurrencyOrDefault,
            BinaryTaskTagStorage(
                CachedKeyValueStorage(
                    tagEngine.stateCacheOrDefault.getKeyValueCache(config),
                    tagEngine.stateStorage!!.getKeyValueStorage(config)
                ),
                CachedKeySetStorage(
                    tagEngine.stateCacheOrDefault.getKeySetCache(config),
                    tagEngine.stateStorage!!.getKeySetStorage(config)
                )
            ),
            pulsarConsumerFactory,
            pulsarOutput
        )
    }

    private fun CoroutineScope.startTaskEngines(
        name: Name,
        consumerName: String,
        taskEngine: TaskEngine,
        pulsarConsumerFactory: PulsarConsumerFactory,
        pulsarOutput: PulsarOutput
    ) {
        println(
            "- task engine".padEnd(25) + ": (" +
                "instances: ${taskEngine.concurrency} ".padEnd(15) +
                ", storage: ${taskEngine.stateStorage}".padEnd(20) +
                ", cache: ${taskEngine.stateCacheOrDefault})".padEnd(20)
        )
        startPulsarTaskEngines(
            name,
            consumerName,
            taskEngine.concurrency,
            BinaryTaskStateStorage(
                CachedKeyValueStorage(
                    taskEngine.stateCacheOrDefault.getKeyValueCache(config),
                    taskEngine.stateStorage!!.getKeyValueStorage(config)
                )
            ),
            pulsarConsumerFactory,
            pulsarOutput
        )
    }

    private fun CoroutineScope.startWorkflowTagEngines(
        workflowName: WorkflowName,
        consumerName: String,
        tagEngine: TagEngine,
        pulsarConsumerFactory: PulsarConsumerFactory,
        pulsarOutput: PulsarOutput
    ) {
        println(
            "- tag engine".padEnd(25) + ": (" +
                "instances: ${tagEngine.concurrencyOrDefault} ".padEnd(15) +
                ", storage: ${tagEngine.stateStorage}".padEnd(20) +
                ", cache: ${tagEngine.stateCacheOrDefault})".padEnd(20)
        )
        startPulsarWorkflowTagEngines(
            workflowName,
            consumerName,
            tagEngine.concurrencyOrDefault,
            BinaryWorkflowTagStorage(
                CachedKeyValueStorage(
                    tagEngine.stateCacheOrDefault.getKeyValueCache(config),
                    tagEngine.stateStorage!!.getKeyValueStorage(config)
                ),
                CachedKeySetStorage(
                    tagEngine.stateCacheOrDefault.getKeySetCache(config),
                    tagEngine.stateStorage!!.getKeySetStorage(config)
                )
            ),
            pulsarConsumerFactory,
            pulsarOutput
        )
    }

    private fun CoroutineScope.startWorkflowEngines(
        workflowName: WorkflowName,
        consumerName: String,
        workflowEngine: WorkflowEngine,
        pulsarConsumerFactory: PulsarConsumerFactory,
        pulsarOutput: PulsarOutput
    ) {
        println(
            "- workflow engine".padEnd(25) + ": (" +
                "instances: ${workflowEngine.concurrency} ".padEnd(15) +
                ", storage: ${workflowEngine.stateStorage}".padEnd(20) +
                ", cache: ${workflowEngine.stateCacheOrDefault})".padEnd(20)
        )
        startPulsarWorkflowEngines(
            workflowName,
            consumerName,
            workflowEngine.concurrency,
            BinaryWorkflowStateStorage(
                CachedKeyValueStorage(
                    workflowEngine.stateCacheOrDefault.getKeyValueCache(config),
                    workflowEngine.stateStorage!!.getKeyValueStorage(config)
                )
            ),
            pulsarConsumerFactory,
            pulsarOutput
        )
    }

    private fun CoroutineScope.startTaskExecutors(
        taskName: TaskName,
        consumerName: String,
        task: Task,
        taskExecutorRegister: TaskExecutorRegister,
        pulsarConsumerFactory: PulsarConsumerFactory,
        pulsarOutput: PulsarOutput
    ) {
        println("- task executor".padEnd(25) + ": (instances: ${task.concurrency})")
        logger.info("InfiniticWorker - starting {} task executor for {}", task.concurrency, task.name)
        taskExecutorRegister.registerTask(task.name) { task.instance }
        startPulsarTaskExecutors(
            taskName,
            task.concurrency,
            consumerName,
            taskExecutorRegister,
            pulsarConsumerFactory,
            pulsarOutput
        )
    }

    private fun CoroutineScope.startWorkflowTaskExecutors(
        workflowName: WorkflowName,
        consumerName: String,
        workflow: Workflow,
        taskExecutorRegister: TaskExecutorRegister,
        pulsarConsumerFactory: PulsarConsumerFactory,
        pulsarOutput: PulsarOutput
    ) {
        println("- workflow executor".padEnd(25) + ": (instances: ${workflow.concurrency})")
        logger.info("InfiniticWorker - starting {} workflow executors for {}", workflow.concurrency, workflow.name)
        taskExecutorRegister.registerWorkflow(workflow.name) { workflow.instance }
        startPulsarTaskExecutors(
            workflowName,
            workflow.concurrency,
            consumerName,
            taskExecutorRegister,
            pulsarConsumerFactory,
            pulsarOutput
        )
    }

    private fun CoroutineScope.startMetricsPerNameEngines(
        taskName: TaskName,
        consumerName: String,
        metrics: Metrics,
        pulsarConsumerFactory: PulsarConsumerFactory,
        pulsarOutput: PulsarOutput
    ) {
        println(
            "- metrics engine".padEnd(25) + ": (" +
                ", storage: ${metrics.stateStorage}".padEnd(20) +
                ", cache: ${metrics.stateCacheOrDefault})".padEnd(20)
        )
        logger.info("InfiniticWorker - starting metrics engine for {}", taskName)
        startPulsarMetricsPerNameEngines(
            taskName,
            consumerName,
            pulsarConsumerFactory,
            BinaryMetricsPerNameStateStorage(
                CachedKeyValueStorage(
                    metrics.stateCacheOrDefault.getKeyValueCache(config),
                    metrics.stateStorage!!.getKeyValueStorage(config)
                )
            ),
            {}
        )
//        logger.info("InfiniticWorker - starting metrics global engine")
//        startPulsarMetricsGlobalEngine(
//            consumerName,
//            pulsarConsumerFactory,
//            BinaryMetricsGlobalStateStorage(
//                CachedKeyValueStorage(
//                    metrics.stateCacheOrDefault.getKeyValueCache(config),
//                    metrics.stateStorage!!.getKeyValueStorage(config)
//                )
//            )
//        )
    }
}
