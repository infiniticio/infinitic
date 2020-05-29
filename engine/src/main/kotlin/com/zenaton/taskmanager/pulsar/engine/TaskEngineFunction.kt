package com.zenaton.taskmanager.pulsar.engine

import com.zenaton.taskmanager.engine.TaskEngine
import com.zenaton.taskmanager.messages.engine.AvroTaskEngineMessage
import com.zenaton.taskmanager.pulsar.avro.TaskAvroConverter
import com.zenaton.taskmanager.pulsar.dispatcher.PulsarTaskDispatcher
import com.zenaton.taskmanager.pulsar.engine.state.PulsarTaskEngineStateStorage
import com.zenaton.taskmanager.pulsar.logger.PulsarTaskLogger
import com.zenaton.workflowengine.pulsar.topics.workflows.dispatcher.WorkflowDispatcher
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

/**
 * This class provides the function used to trigger TaskEngine from the tasks topic
 */
class TaskEngineFunction : Function<AvroTaskEngineMessage, Void> {

    // task engine injection
    var taskEngine = TaskEngine()
    // avro converter injection
    var avroConverter = TaskAvroConverter

    override fun process(input: AvroTaskEngineMessage, context: Context?): Void? {
        val ctx = context ?: throw NullPointerException("Null Context received")

        taskEngine.logger = PulsarTaskLogger(ctx)
        taskEngine.taskDispatcher = PulsarTaskDispatcher(ctx)
        taskEngine.workflowDispatcher = WorkflowDispatcher(ctx)
        taskEngine.stateStorage = PulsarTaskEngineStateStorage(ctx)

        try {
            taskEngine.handle(avroConverter.fromAvro(input))
        } catch (e: Exception) {
            taskEngine.logger.error("Error:%s for message:%s", e, input)
            throw e
        }

        return null
    }
}
