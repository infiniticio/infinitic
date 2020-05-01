package com.zenaton.pulsar.topics.tasks

import com.zenaton.engine.data.tasks.TaskState
import com.zenaton.engine.topics.tasks.TaskStaterInterface
import com.zenaton.pulsar.utils.StateSerDe
import com.zenaton.pulsar.utils.StateSerDeInterface
import org.apache.pulsar.functions.api.Context

class TaskStater(private val context: Context) : TaskStaterInterface {

        // StateSerDe injection
        private var serDe: StateSerDeInterface = StateSerDe

        override fun getState(key: String): TaskState? {
        return context.getState(key) ?. let { serDe.deserialize(it) }
        }

        override fun createState(state: TaskState) {
        context.putState(state.getKey(), serDe.serialize(state))
        }

        override fun updateState(state: TaskState) {
        context.putState(state.getKey(), serDe.serialize(state))
        }

        override fun deleteState(state: TaskState) {
        context.deleteState(state.getKey())
        }
}
