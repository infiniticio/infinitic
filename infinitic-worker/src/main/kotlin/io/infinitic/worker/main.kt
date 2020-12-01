package io.infinitic.worker

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withTimeout
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Random

private fun log(msg: String) {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    println("${LocalDateTime.now().format(formatter)} [${Thread.currentThread().name}] $msg")
}

private const val N_WORKERS = 100

private fun CoroutineScope.startTaskEngine(
    dispatchChannel: ReceiveChannel<TaskEngineMessage>,
    resultChannel: ReceiveChannel<TaskEngineMessage>,
    workerChannel: SendChannel<WorkerMessage>,
) = launch {
    val requested = mutableMapOf<Int, Boolean>()

    while (true) {
        select<Unit> {
            resultChannel.onReceive { taskEngineMessage ->
                when (taskEngineMessage) {
                    is TaskCompleted -> {
                        requested.remove(taskEngineMessage.taskId)
                    }
                    is TaskFailed -> {
                        workerChannel.send(RunTask(taskEngineMessage.taskId))
                    }
                }
            }
            dispatchChannel.onReceive { taskEngineMessage ->
                when (taskEngineMessage) {
                    is DispatchTask -> {
                        log("DispatchTask: ${taskEngineMessage.taskId}")
                        requested[taskEngineMessage.taskId] = true
                        workerChannel.send(RunTask(taskEngineMessage.taskId))
                    }
                }
            }
        }
    }
}

private fun CoroutineScope.startBufferWorker(
    bufferChannel: ReceiveChannel<TaskEngineMessage>,
    resultChannel: SendChannel<TaskEngineMessage>,
) = launch {
    for (taskEngineMessage in bufferChannel) {
        when (taskEngineMessage) {
            is TaskCompleted -> {
                launch {
                    resultChannel.send(taskEngineMessage)
                }
            }
            is TaskFailed -> {
                launch {
                    delay(taskEngineMessage.delay)
                    resultChannel.send(taskEngineMessage)
                }
            }
        }
    }
}

private fun CoroutineScope.startWorker(
    workerChannel: ReceiveChannel<WorkerMessage>,
    bufferChannel: SendChannel<TaskEngineMessage>,
) = launch(Dispatchers.Default) {
    for (workerMessage in workerChannel) {
        when (workerMessage) {
            is RunTask -> {
                log("RunTask: ${workerMessage.taskId}")
                // processing
                val r = Random().nextInt(100)
                delay(r.toLong())
                // result
                if (r > 20) {
                    log("TaskCompleted: ${workerMessage.taskId}")
                    bufferChannel.send(TaskCompleted(workerMessage.taskId, r.toLong()))
                } else {
                    log("TaskFailed: ${workerMessage.taskId}")
                    bufferChannel.send(TaskFailed(workerMessage.taskId, 100L))
                }
            }
        }
    }
}

private fun CoroutineScope.processTasks(
    dispatchChannel: Channel<TaskEngineMessage>
) {
    val resultChannel = Channel<TaskEngineMessage>()
    val bufferChannel = Channel<TaskEngineMessage>()
    val workerChannel = Channel<WorkerMessage>()
    startBufferWorker(bufferChannel, resultChannel)
    repeat(N_WORKERS) { startWorker(workerChannel, bufferChannel) }
    startTaskEngine(dispatchChannel, resultChannel, workerChannel)
}

fun main() = runBlocking {
    withTimeout(1000000) {
        val dispatchChannel = Channel<TaskEngineMessage>()
        processTasks(dispatchChannel)
        for (i in 1..100000) {
            dispatchChannel.send(DispatchTask(i))
        }
    }
}

sealed class TaskEngineMessage() {
    abstract val taskId: Int
}

data class DispatchTask(
    override val taskId: Int,
) : TaskEngineMessage()

data class TaskCompleted(
    override val taskId: Int,
    val output: Long
) : TaskEngineMessage()

data class TaskFailed(
    override val taskId: Int,
    val delay: Long
) : TaskEngineMessage()

sealed class WorkerMessage() {
    abstract val taskId: Int
}

data class RunTask(
    override val taskId: Int,
) : WorkerMessage()
