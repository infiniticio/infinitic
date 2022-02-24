// /**
// * "Commons Clause" License Condition v1.0
// *
// * The Software is provided to you by the Licensor under the License, as defined
// * below, subject to the following condition.
// *
// * Without limiting other conditions in the License, the grant of rights under the
// * License will not include, and the License does not grant to you, the right to
// * Sell the Software.
// *
// * For purposes of the foregoing, “Sell” means practicing any or all of the rights
// * granted to you under the License to provide to third parties, for a fee or
// * other consideration (including without limitation fees for hosting or
// * consulting/ support services related to the Software), a product or service
// * whose value derives, entirely or substantially, from the functionality of the
// * Software. Any license notice or attribution required by the License must also
// * include this Commons Clause License Condition notice.
// *
// * Software: Infinitic
// *
// * License: MIT License (https://opensource.org/licenses/MIT)
// *
// * Licensor: infinitic.io
// */
//
// package io.infinitic.transport.inMemory
//
// import io.infinitic.common.clients.messages.ClientMessage
// import io.infinitic.common.clients.transport.ClientMessageToProcess
// import io.infinitic.common.data.MillisDuration
// import io.infinitic.common.metrics.global.messages.MetricsGlobalMessage
// import io.infinitic.common.metrics.perName.messages.MetricsPerNameMessage
// import io.infinitic.common.tasks.data.TaskName
// import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
// import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
// import io.infinitic.common.tasks.tags.messages.TaskTagEngineMessage
// import io.infinitic.common.transport.Sender
// import io.infinitic.common.workers.MessageToProcess
// import io.infinitic.common.workflows.data.workflows.WorkflowName
// import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
// import io.infinitic.common.workflows.tags.messages.WorkflowTagEngineMessage
// import io.infinitic.metrics.global.engine.worker.MetricsGlobalMessageToProcess
// import io.infinitic.metrics.perName.engine.worker.MetricsPerNameMessageToProcess
// import io.infinitic.tasks.engine.worker.TaskEngineMessageToProcess
// import io.infinitic.tasks.executor.worker.TaskExecutorMessageToProcess
// import io.infinitic.workflows.engine.worker.WorkflowEngineMessageToProcess
// import kotlinx.coroutines.CoroutineScope
// import kotlinx.coroutines.channels.Channel
// import kotlinx.coroutines.delay
// import kotlinx.coroutines.future.future
// import mu.KotlinLogging
// import java.util.concurrent.ConcurrentHashMap
//
// class InMemorySender : Sender {
//    private val logger = KotlinLogging.logger {}
//
//    lateinit var scope: CoroutineScope
//
//    val logChannel = Channel<MessageToProcess<Any>>()
//
//    val clientChannel = Channel<ClientMessageToProcess>()
//
//    val taskTagChannel = ConcurrentHashMap<TaskName, Channel<TaskTagMessageToProcess>>()
//    val taskEngineChannel = ConcurrentHashMap<TaskName, Channel<TaskEngineMessageToProcess>>()
//    val taskExecutorChannel= ConcurrentHashMap<TaskName, Channel<TaskExecutorMessageToProcess>>()
//    val taskMetricsChannel = ConcurrentHashMap<TaskName, Channel<MetricsPerNameMessageToProcess>>()
//
//    val workflowTagChannel = ConcurrentHashMap<WorkflowName, Channel<WorkflowTagMessageToProcess>>()
//    val workflowEngineChannel = ConcurrentHashMap<WorkflowName, Channel<WorkflowEngineMessageToProcess>>()
//    val workflowTaskEngineChannel = ConcurrentHashMap<WorkflowName, Channel<TaskEngineMessageToProcess>>()
//    val workflowExecutorChannel = ConcurrentHashMap<WorkflowName, Channel<TaskExecutorMessageToProcess>>()
//    val workflowMetricsChannel = ConcurrentHashMap<WorkflowName, Channel<MetricsPerNameMessageToProcess>>()
//
//    val globalMetricsChannel = Channel<MetricsGlobalMessageToProcess>()
//
//    override fun initTask(name: TaskName) {
//        taskTagChannel[name] = Channel()
//        taskEngineChannel[name] = Channel()
//        taskExecutorChannel[name] = Channel()
//        taskMetricsChannel[name] = Channel()
//    }
//
//    override fun initWorkflow(name: WorkflowName) {
//        workflowTagChannel[name] = Channel()
//        workflowEngineChannel[name] = Channel()
//        workflowTaskEngineChannel[name] = Channel()
//        workflowExecutorChannel[name] = Channel()
//        workflowMetricsChannel[name] = Channel()
//    }
//
//    override fun sendToClient(message: ClientMessage) {
//        logger.debug { "sendToClient $message" }
//        // As it's a back loop, we trigger it asynchronously to avoid deadlocks
//        scope.future {
//            clientChannel.send(InMemoryMessageToProcess(message))
//        }
//    }
//
//    override fun sendToTaskTag(message: TaskTagEngineMessage) {
//        logger.debug { "sendToTaskTag $message" }
//        // As it's a back loop, we trigger it asynchronously to avoid deadlocks
//        scope.future {
//            taskTagChannel[message.taskName]!!.send(InMemoryMessageToProcess(message))
//        }.join()
//    }
//
//    override fun sendToTaskEngine(message: TaskEngineMessage, after: MillisDuration) {
//        logger.debug { "sendToTaskEngine $message" }
//        scope.future {
//            delay(after.long)
//            taskEngineChannel[message.taskName]!!.send(InMemoryMessageToProcess(message))
//        }.join()
//    }
//
//    override fun sendToWorkflowTaskEngine(message: TaskEngineMessage) {
//        logger.debug { "sendToWorkflowTaskEngine $message" }
//        scope.future {
//            workflowTaskEngineChannel[name]!!.send(InMemoryMessageToProcess(message))
//        }.join()
//    }
//
//    override fun sendToWorkflowTag(message: WorkflowTagEngineMessage) {
//        logger.debug { "sendToWorkflowTag $message" }
//        scope.future {
//            workflowTagChannel[message.workflowName]!!.send(InMemoryMessageToProcess(message))
//        }.join()
//    }
//
//    override fun sendToWorkflowEngine(message: WorkflowEngineMessage, after: MillisDuration) {
//        logger.debug { "sendToWorkflowEngine $message" }
//        // As it's a back loop, we trigger it asynchronously to avoid deadlocks
//        scope.future {
//            delay(after.long)
//            workflowEngineChannel[message.workflowName]!!.send(InMemoryMessageToProcess(message))
//        }
//    }
//
//    override fun sendToTaskExecutors(message: TaskExecutorMessage) {
//        logger.debug { "sendToTaskExecutors $message" }
//        scope.future {
//            taskExecutorChannel[message.taskName]!!.send(InMemoryMessageToProcess(message))
//        }.join()
//    }
//
//    override fun sendToWorkflowExecutors(message: TaskExecutorMessage) {
//        logger.debug { "sendToWorkflowExecutors $message" }
//        scope.future {
//            taskExecutorChannel[message.taskName]!!.send(InMemoryMessageToProcess(message))
//        }.join()
//    }
//
//    override fun sendToTaskMetrics(message: MetricsPerNameMessage) {
//        logger.debug { "sendToTaskMetrics $message" }
//        scope.future {
//            taskMetricsChannel[message.taskName]!!.send(InMemoryMessageToProcess(message))
//        }.join()
//    }
//
//    override fun sendToWorkflowMetrics(message: MetricsPerNameMessage) {
//        logger.debug { "sendToWorkflowMetrics $message" }
//        scope.future {
//            taskMetricsChannel[message.taskName]!!.send(InMemoryMessageToProcess(message))
//        }.join()
//    }
//
//    override fun sendToGlobalMetrics(message: MetricsGlobalMessage) {
//        logger.debug { "sendToGlobalMetrics $message" }
//        scope.future {
//            globalMetricsChannel.send(InMemoryMessageToProcess(message))
//        }.join()
//    }
// }
