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

import com.sksamuel.hoplite.ConfigLoader
import io.infinitic.common.monitoring.global.messages.MonitoringGlobalMessage
import io.infinitic.common.monitoring.perName.messages.MonitoringPerNameEngineMessage
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.workers.MessageToProcess
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.pulsar.config.Config
import io.infinitic.pulsar.config.Mode
import io.infinitic.pulsar.transport.PulsarConsumerFactory
import io.infinitic.pulsar.transport.PulsarOutputs
import io.infinitic.pulsar.workers.startPulsarMonitoringGlobalWorker
import io.infinitic.pulsar.workers.startPulsarMonitoringPerNameWorker
import io.infinitic.pulsar.workers.startPulsarTaskEngineWorker
import io.infinitic.pulsar.workers.startPulsarTaskExecutorWorker
import io.infinitic.pulsar.workers.startPulsarWorkflowEngineWorker
import io.infinitic.storage.inMemory.InMemoryStorage
import io.infinitic.tasks.executor.register.TaskExecutorRegisterImpl
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.pulsar.client.api.PulsarClient
import java.lang.RuntimeException
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor

class InfiniticWorker(
    configPath: String? = null,
    config: Config? = null,
    pulsarClient: PulsarClient? = null
) {
    init {
        require(configPath != null || config != null) { "Please provide a configuration" }
    }

    // useful for Java users
    data class Builder(
        var configPath: String? = null,
        var config: Config? = null,
        var pulsarClient: PulsarClient? = null,
    ) {
        fun configPath(configPath: String) = apply { this.configPath = configPath }
        fun config(config: Config) = apply { this.config = config }
        fun pulsarClient(pulsarClient: PulsarClient) = apply { this.pulsarClient = pulsarClient }
        fun build() = InfiniticWorker(configPath, config, pulsarClient)
    }

    // loaf config from file
    private val configFromFile: Config? = configPath?.let { ConfigLoader().loadConfigOrThrow(it) }

    // merge config **with precedence from file**
    val config: Config = if (config == null) configFromFile!! else {
        if (configFromFile == null) config else (config merge configFromFile)
    }

    // build pulsar client if not provided
    val pulsarClient: PulsarClient = pulsarClient
        ?: PulsarClient.builder().serviceUrl(this.config.pulsar.serviceUrl).build()

    fun start() = runBlocking {
        val tenant = config.pulsar.tenant
        val namespace = config.pulsar.namespace

        val pulsarConsumerFactory = PulsarConsumerFactory(pulsarClient, tenant, namespace)
        val pulsarOutputs = PulsarOutputs.from(pulsarClient, tenant, namespace)

        val logChannel = Channel<MessageToProcess<Any>>()

        launch(CoroutineName("logger")) {
            for (messageToProcess in logChannel) {
                val failed = if (messageToProcess.exception == null) "" else "(failed) "
                when (val message = messageToProcess.message) {
                    is MonitoringGlobalMessage ->
                        println("Monitoring Global  : $failed$message")
                    is MonitoringPerNameEngineMessage ->
                        println("Monitoring Per Name: $failed$message")
                    is TaskExecutorMessage ->
                        println("Task Executor      : $failed$message")
                    is TaskEngineMessage ->
                        println("Task engine        : $failed$message")
                    is WorkflowEngineMessage ->
                        println("Workflow engine    : $failed$message")
                    else -> throw RuntimeException("Unknown messageToProcess type: $messageToProcess")
                }
            }
        }

        if (config.workflowEngine.mode == Mode.worker) {
            repeat(config.workflowEngine.consumers) {
                startPulsarWorkflowEngineWorker(
                    it,
                    pulsarConsumerFactory.newWorkflowEngineConsumer(config.name, it),
                    pulsarOutputs.workflowEngineOutput,
                    InMemoryStorage(),
                    logChannel,
                )
            }
        }

        if (config.taskEngine.mode == Mode.worker) {
            repeat(config.taskEngine.consumers) {
                startPulsarTaskEngineWorker(
                    it,
                    pulsarConsumerFactory.newTaskEngineConsumer(config.name, it),
                    pulsarOutputs.taskEngineOutput,
                    InMemoryStorage(),
                    logChannel
                )
            }
        }

        if (config.monitoring.mode == Mode.worker) {
            repeat(config.monitoring.consumers) {
                startPulsarMonitoringPerNameWorker(
                    it,
                    pulsarConsumerFactory.newMonitoringPerNameEngineConsumer(config.name, it),
                    pulsarOutputs.monitoringPerNameOutput,
                    InMemoryStorage(),
                    logChannel,
                )
            }

            startPulsarMonitoringGlobalWorker(
                pulsarConsumerFactory.newMonitoringGlobalEngineConsumer(config.name),
                InMemoryStorage(),
                logChannel
            )
        }

        val taskExecutorRegister = TaskExecutorRegisterImpl()

        for (workflow in config.workflows) {
            if (workflow.mode == Mode.worker) {
                taskExecutorRegister.register(workflow.name) { workflow.getInstance() }

                repeat(workflow.consumers) {
                    startPulsarTaskExecutorWorker(
                        workflow.name,
                        it,
                        pulsarConsumerFactory.newWorkflowExecutorConsumer(config.name, it, workflow.name),
                        pulsarOutputs.taskExecutorOutput,
                        taskExecutorRegister,
                        logChannel,
                        workflow.concurrency
                    )
                }
            }
        }

        for (task in config.tasks) {
            if (task.mode == Mode.worker) {
                if (task.shared) {
                    val instance = task.getInstance()
                    taskExecutorRegister.register(task.name) { instance }
                } else {
                    taskExecutorRegister.register(task.name) { task.getInstance() }
                }

                repeat(task.consumers) {
                    startPulsarTaskExecutorWorker(
                        task.name,
                        it,
                        pulsarConsumerFactory.newTaskExecutorConsumer(config.name, it, task.name),
                        pulsarOutputs.taskExecutorOutput,
                        taskExecutorRegister,
                        logChannel,
                        task.concurrency
                    )
                }
            }
        }
    }
}

/**
 * Merge two data classes
 *
 * The resulting data class will contain:
 * - all fields of `other` which are non null
 * - the fields of `this` for the fields which are null in `other`
 *
 * The function is immutable, the original data classes are not changed
 * and a new data class instance is returned.
 *
 * Example usage:
 *
 *     val a = MyDataClass(...)
 *     val b = MyDataClass(...)
 *     val c = a merge b
 *
 *     https://gist.github.com/josdejong/fbb43ae33fcdd922040dac4ffc31aeaf
 */
inline infix fun <reified T : Any> T.merge(other: T): T {
    val nameToProperty = T::class.declaredMemberProperties.associateBy { it.name }
    val primaryConstructor = T::class.primaryConstructor!!
    val args = primaryConstructor.parameters.associate { parameter ->
        val property = nameToProperty[parameter.name]!!
        parameter to (property.get(other) ?: property.get(this))
    }
    return primaryConstructor.callBy(args)
}
