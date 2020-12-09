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

package io.infinitic.worker.inMemory

// private fun log(msg: String) {
//    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
//    println("${LocalDateTime.now().format(formatter)} [${Thread.currentThread().name}] $msg")
// }
//
// private const val N_WORKERS = 100
//
// private fun CoroutineScope.startTaskEngine(
//    dispatchChannel: ReceiveChannel<TaskEngineMessage>,
//    resultChannel: ReceiveChannel<TaskEngineMessage>,
//    workerChannel: SendChannel<WorkerMessage>,
// ) = launch {
//    val requested = mutableMapOf<Int, Boolean>()
//
//    while (true) {
//        select<Unit> {
//            resultChannel.onReceive { taskEngineMessage ->
//                when (taskEngineMessage) {
//                    is TaskCompleted -> {
//                        requested.remove(taskEngineMessage.taskId)
//                    }
//                    is TaskFailed -> {
//                        workerChannel.send(RunTask(taskEngineMessage.taskId))
//                    }
//                }
//            }
//            dispatchChannel.onReceive { taskEngineMessage ->
//                when (taskEngineMessage) {
//                    is DispatchTask -> {
//                        log("DispatchTask: ${taskEngineMessage.taskId}")
//                        requested[taskEngineMessage.taskId] = true
//                        workerChannel.send(RunTask(taskEngineMessage.taskId))
//                    }
//                }
//            }
//        }
//    }
// }
//
// private fun CoroutineScope.startBufferWorker(
//    bufferChannel: ReceiveChannel<TaskEngineMessage>,
//    resultChannel: SendChannel<TaskEngineMessage>,
// ) = launch {
//    for (taskEngineMessage in bufferChannel) {
//        when (taskEngineMessage) {
//            is TaskCompleted -> {
//                launch {
//                    resultChannel.send(taskEngineMessage)
//                }
//            }
//            is TaskFailed -> {
//                launch {
//                    delay(taskEngineMessage.delay)
//                    resultChannel.send(taskEngineMessage)
//                }
//            }
//        }
//    }
// }
//
// private fun CoroutineScope.startWorker(
//    workerChannel: ReceiveChannel<WorkerMessage>,
//    bufferChannel: SendChannel<TaskEngineMessage>,
// ) = launch(Dispatchers.Default) {
//    for (workerMessage in workerChannel) {
//        when (workerMessage) {
//            is RunTask -> {
//                log("RunTask: ${workerMessage.taskId}")
//                // processing
//                val r = Random().nextInt(100)
//                delay(r.toLong())
//                // result
//                if (r > 20) {
//                    log("TaskCompleted: ${workerMessage.taskId}")
//                    bufferChannel.send(TaskCompleted(workerMessage.taskId, r.toLong()))
//                } else {
//                    log("TaskFailed: ${workerMessage.taskId}")
//                    bufferChannel.send(TaskFailed(workerMessage.taskId, 100L))
//                }
//            }
//        }
//    }
// }
//
// private fun CoroutineScope.processTasks(
//    dispatchChannel: Channel<TaskEngineMessage>
// ) {
//    val resultChannel = Channel<TaskEngineMessage>()
//    val bufferChannel = Channel<TaskEngineMessage>()
//    val workerChannel = Channel<WorkerMessage>()
//    startBufferWorker(bufferChannel, resultChannel)
//    repeat(N_WORKERS) { startWorker(workerChannel, bufferChannel) }
//    startTaskEngine(dispatchChannel, resultChannel, workerChannel)
// }
//
// fun main() = runBlocking {
//    withTimeout(1000000) {
//        val dispatchChannel = Channel<TaskEngineMessage>()
//        processTasks(dispatchChannel)
//        for (i in 1..100000) {
//            dispatchChannel.send(DispatchTask(i))
//        }
//    }
// }
//
// sealed class TaskEngineMessage() {
//    abstract val taskId: Int
// }
//
// data class DispatchTask(
//    override val taskId: Int,
// ) : TaskEngineMessage()
//
// data class TaskCompleted(
//    override val taskId: Int,
//    val output: Long
// ) : TaskEngineMessage()
//
// data class TaskFailed(
//    override val taskId: Int,
//    val delay: Long
// ) : TaskEngineMessage()
//
// sealed class WorkerMessage() {
//    abstract val taskId: Int
// }
//
// data class RunTask(
//    override val taskId: Int,
// ) : WorkerMessage()
