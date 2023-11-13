package io.infinitic.pulsar.topics

import io.infinitic.common.data.ClientName
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.workflows.data.workflows.WorkflowName

class TopicNamesDefault(override val tenant: String, override val namespace: String) : TopicNames {

  override fun producerName(workerName: String, type: TopicType) =
      "$workerName>>${type.subscriptionPrefix}"

  override fun consumerName(workerName: String, type: TopicType) =
      "$workerName<<${type.subscriptionPrefix}"

  override fun topic(type: ClientTopics, clientName: ClientName) =
      fullName("${type.subscriptionPrefix}:$clientName")

  override fun topic(type: GlobalTopics) = fullName(type.subscriptionPrefix)

  override fun topic(type: WorkflowTopics, workflowName: WorkflowName) =
      fullName("${type.subscriptionPrefix}:$workflowName")

  override fun topicDLQ(type: WorkflowTopics, workflowName: WorkflowName) =
      fullName("${type.subscriptionPrefix}-dlq:$workflowName")

  override fun topic(type: WorkflowTaskTopics, workflowName: WorkflowName) =
      fullName("${type.subscriptionPrefix}:$workflowName")

  override fun topicDLQ(type: WorkflowTaskTopics, workflowName: WorkflowName) =
      fullName("${type.subscriptionPrefix}-dlq:$workflowName")

  override fun topic(type: ServiceTopics, serviceName: ServiceName) =
      fullName("${type.subscriptionPrefix}:$serviceName")

  override fun topicDLQ(type: ServiceTopics, serviceName: ServiceName) =
      fullName("${type.subscriptionPrefix}-dlq:$serviceName")
}
