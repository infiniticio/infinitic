package io.infinitic.pulsar.producers

import org.apache.pulsar.client.api.CompressionType
import org.apache.pulsar.client.api.HashingScheme
import org.apache.pulsar.client.api.MessageRoutingMode
import org.apache.pulsar.client.api.ProducerCryptoFailureAction

data class ProducerConfig(
  val autoUpdatePartitions: Boolean? = null,
  val autoUpdatePartitionsIntervalSeconds: Double? = null,
  val batchingMaxBytes: Int? = null,
  val batchingMaxMessages: Int? = null,
  val batchingMaxPublishDelaySeconds: Double? = null,
  val blockIfQueueFull: Boolean = true,
  val compressionType: CompressionType? = null,
  val cryptoFailureAction: ProducerCryptoFailureAction? = null,
  val defaultCryptoKeyReader: String? = null,
  val encryptionKey: String? = null,
  val enableBatching: Boolean? = null,
  val enableChunking: Boolean? = null,
  val enableLazyStartPartitionedProducers: Boolean? = null,
  val enableMultiSchema: Boolean? = null,
  val hashingScheme: HashingScheme? = null,
  val messageRoutingMode: MessageRoutingMode? = null,
  val properties: Map<String, String>? = null,
  val roundRobinRouterBatchingPartitionSwitchFrequency: Int? = null,
  val sendTimeoutSeconds: Double? = null
)
