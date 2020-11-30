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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Random

private fun log(msg: String) {
    val time = SimpleDateFormat("HH:mm:ss.sss").format(Date())
    println("$time [${Thread.currentThread().name}] $msg")
}

private const val N_WORKERS = 10

private fun CoroutineScope.startTaskEngine(
    dispatchChannel: ReceiveChannel<TaskEngineMessage>,
    taskEngineChannel: ReceiveChannel<TaskEngineMessage>,
    workerChannel: SendChannel<WorkerMessage>,
    ) = launch {
    val requested = mutableMapOf<Int, Boolean>()

    while(true) {
        select<Unit> {
            taskEngineChannel.onReceive { taskEngineMessage ->
                when (taskEngineMessage) {
                    is TaskCompleted -> {
                        log("TaskCompleted: ${taskEngineMessage.taskId}")
                        requested.remove(taskEngineMessage.taskId)
                        log("size: ${requested.size}")
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
                    is TaskFailed -> {
                        log("TaskFailed: ${taskEngineMessage.taskId}")
                        workerChannel.send(RunTask(taskEngineMessage.taskId))
                    }
                }
            }
        }
    }
}

private fun CoroutineScope.startWorker(
    workerChannel: ReceiveChannel<WorkerMessage>,
    taskEngineChannel: SendChannel<TaskEngineMessage>,
    errorChannel: SendChannel<TaskEngineMessage>,
) = launch(Dispatchers.Default) {
    for (workerMessage in workerChannel) {
        when (workerMessage) {
            is RunTask -> {
                val r = Random().nextInt(100)
                delay(r.toLong())
//                Thread.sleep(r.toLong())
                if (r > 20) {
                    taskEngineChannel.send(TaskCompleted(workerMessage.taskId, r.toLong()))
                } else {
                    launch {
                        delay(1000)
                        errorChannel.send(TaskFailed(workerMessage.taskId))
                    }
                }
            }
        }
    }
}

private fun CoroutineScope.processTasks(
    dispatchChannel: Channel<TaskEngineMessage>
) {
    val taskEngineChannel = Channel<TaskEngineMessage>(1)
    val workerChannel = Channel<WorkerMessage>()
    repeat(N_WORKERS) { startWorker(workerChannel, taskEngineChannel, dispatchChannel) }
    startTaskEngine(dispatchChannel, taskEngineChannel, workerChannel)
}

fun main() = runBlocking {
    withTimeout(10000) {
        val dispatchChannel = Channel<TaskEngineMessage>()
        processTasks(dispatchChannel)
        for (i in 1..1000) {
            dispatchChannel.send(DispatchTask(i))
        }

        Unit
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
    override val taskId: Int
) : TaskEngineMessage()

sealed class WorkerMessage() {
    abstract val taskId: Int
}

data class RunTask(
    override val taskId: Int,
) : WorkerMessage()
