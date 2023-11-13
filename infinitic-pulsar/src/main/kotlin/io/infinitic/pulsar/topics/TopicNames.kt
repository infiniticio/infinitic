package io.infinitic.pulsar.topics

import io.infinitic.common.data.ClientName
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.workflows.data.workflows.WorkflowName

interface TopicNames {
  val tenant: String
  
  val namespace: String

  fun fullName(topic: String) = "persistent://$tenant/$namespace/$topic"

  fun producerName(workerName: String, type: TopicType): String

  fun consumerName(workerName: String, type: TopicType): String

  fun topic(type: GlobalTopics): String

  fun topic(type: ClientTopics, clientName: ClientName): String

  fun topic(type: WorkflowTopics, workflowName: WorkflowName): String

  fun topicDLQ(type: WorkflowTopics, workflowName: WorkflowName): String

  fun topic(type: ServiceTopics, serviceName: ServiceName): String

  fun topicDLQ(type: ServiceTopics, serviceName: ServiceName): String

  fun topic(type: WorkflowTaskTopics, workflowName: WorkflowName): String

  fun topicDLQ(type: WorkflowTaskTopics, workflowName: WorkflowName): String

  fun topic(type: TopicType, name: String): String =
      when (type) {
        is ClientTopics -> topic(type, ClientName(name))
        is GlobalTopics -> topic(type)
        is WorkflowTopics -> topic(type, WorkflowName(name))
        is WorkflowTaskTopics -> topic(type, WorkflowName(name))
        is ServiceTopics -> topic(type, ServiceName(name))
        else -> thisShouldNotHappen()
      }

  fun topicDLQ(type: TopicType, name: String): String? =
      when (type) {
        is WorkflowTopics -> topicDLQ(type, WorkflowName(name))
        is ServiceTopics -> topicDLQ(type, ServiceName(name))
        is WorkflowTaskTopics -> topicDLQ(type, WorkflowName(name))
        else -> null
      }
}

