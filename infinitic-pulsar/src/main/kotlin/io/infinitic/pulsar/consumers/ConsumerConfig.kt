package io.infinitic.pulsar.consumers

import org.apache.pulsar.client.api.ConsumerCryptoFailureAction

data class ConsumerConfig(
  val loadConf: Map<String, String>? = null,
  val subscriptionProperties: Map<String, String>? = null,
  val ackTimeoutSeconds: Double? = null,
  val isAckReceiptEnabled: Boolean? = null,
  val ackTimeoutTickTimeSeconds: Double? = null,
  val negativeAckRedeliveryDelaySeconds: Double? = null,
  val defaultCryptoKeyReader: String? = null,
  val cryptoFailureAction: ConsumerCryptoFailureAction? = null,
  val receiverQueueSize: Int? = null,
  val acknowledgmentGroupTimeSeconds: Double? = null,
  val replicateSubscriptionState: Boolean? = null,
  val maxTotalReceiverQueueSizeAcrossPartitions: Int? = null,
  val priorityLevel: Int? = null,
  val properties: Map<String, String>? = null,
  val autoUpdatePartitions: Boolean? = null,
  val autoUpdatePartitionsIntervalSeconds: Double? = null,
  val enableBatchIndexAcknowledgment: Boolean? = null,
  val maxPendingChunkedMessage: Int? = null,
  val autoAckOldestChunkedMessageOnQueueFull: Boolean? = null,
  val expireTimeOfIncompleteChunkedMessageSeconds: Double? = null,
  val startPaused: Boolean? = null,
  val maxRedeliverCount: Int = 3
)
