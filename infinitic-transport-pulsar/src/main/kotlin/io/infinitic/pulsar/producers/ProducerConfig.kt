/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

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
) {

  companion object {
    @JvmStatic
    fun builder() = ProducerConfigBuilder()

    const val DEFAULT_MAX_REDELIVER_COUNT = 3
  }

  /**
   * ProducerConfig builder (Useful for Java user)
   */
  class ProducerConfigBuilder {
    private val default = ProducerConfig()

    private var autoUpdatePartitions = default.autoUpdatePartitions
    private var autoUpdatePartitionsIntervalSeconds = default.autoUpdatePartitionsIntervalSeconds
    private var batchingMaxBytes = default.batchingMaxBytes
    private var batchingMaxMessages = default.batchingMaxMessages
    private var batchingMaxPublishDelaySeconds = default.batchingMaxPublishDelaySeconds
    private var blockIfQueueFull = default.blockIfQueueFull
    private var compressionType = default.compressionType
    private var cryptoFailureAction = default.cryptoFailureAction
    private var defaultCryptoKeyReader = default.defaultCryptoKeyReader
    private var encryptionKey = default.encryptionKey
    private var enableBatching = default.enableBatching
    private var enableChunking = default.enableChunking
    private var enableLazyStartPartitionedProducers = default.enableLazyStartPartitionedProducers
    private var enableMultiSchema = default.enableMultiSchema
    private var hashingScheme = default.hashingScheme
    private var messageRoutingMode = default.messageRoutingMode
    private var properties = default.properties
    private var roundRobinRouterBatchingPartitionSwitchFrequency =
        default.roundRobinRouterBatchingPartitionSwitchFrequency
    private var sendTimeoutSeconds = default.sendTimeoutSeconds

    fun setAutoUpdatePartitions(autoUpdatePartitions: Boolean) =
        apply { this.autoUpdatePartitions = autoUpdatePartitions }

    fun setAutoUpdatePartitionsIntervalSeconds(autoUpdatePartitionsIntervalSeconds: Double) =
        apply { this.autoUpdatePartitionsIntervalSeconds = autoUpdatePartitionsIntervalSeconds }

    fun setBatchingMaxBytes(batchingMaxBytes: Int) =
        apply { this.batchingMaxBytes = batchingMaxBytes }

    fun setBatchingMaxMessages(batchingMaxMessages: Int) =
        apply { this.batchingMaxMessages = batchingMaxMessages }

    fun setBatchingMaxPublishDelaySeconds(batchingMaxPublishDelaySeconds: Double) =
        apply { this.batchingMaxPublishDelaySeconds = batchingMaxPublishDelaySeconds }

    fun setBlockIfQueueFull(blockIfQueueFull: Boolean) =
        apply { this.blockIfQueueFull = blockIfQueueFull }

    fun setCompressionType(compressionType: CompressionType) =
        apply { this.compressionType = compressionType }

    fun setCryptoFailureAction(cryptoFailureAction: ProducerCryptoFailureAction) =
        apply { this.cryptoFailureAction = cryptoFailureAction }

    fun setDefaultCryptoKeyReader(defaultCryptoKeyReader: String) =
        apply { this.defaultCryptoKeyReader = defaultCryptoKeyReader }

    fun setEncryptionKey(encryptionKey: String) =
        apply { this.encryptionKey = encryptionKey }

    fun setEnableBatching(enableBatching: Boolean) =
        apply { this.enableBatching = enableBatching }

    fun setEnableChunking(enableChunking: Boolean) =
        apply { this.enableChunking = enableChunking }

    fun setEnableLazyStartPartitionedProducers(enableLazyStartPartitionedProducers: Boolean) =
        apply { this.enableLazyStartPartitionedProducers = enableLazyStartPartitionedProducers }

    fun setEnableMultiSchema(enableMultiSchema: Boolean) =
        apply { this.enableMultiSchema = enableMultiSchema }

    fun setHashingScheme(hashingScheme: HashingScheme) =
        apply { this.hashingScheme = hashingScheme }

    fun setMessageRoutingMode(messageRoutingMode: MessageRoutingMode) =
        apply { this.messageRoutingMode = messageRoutingMode }

    fun setProperties(properties: Map<String, String>) =
        apply { this.properties = properties }

    fun setRoundRobinRouterBatchingPartitionSwitchFrequency(
      roundRobinRouterBatchingPartitionSwitchFrequency: Int
    ) = apply {
      this.roundRobinRouterBatchingPartitionSwitchFrequency =
          roundRobinRouterBatchingPartitionSwitchFrequency
    }

    fun setSendTimeoutSeconds(sendTimeoutSeconds: Double) =
        apply { this.sendTimeoutSeconds = sendTimeoutSeconds }

    fun build() = ProducerConfig(
        autoUpdatePartitions,
        autoUpdatePartitionsIntervalSeconds,
        batchingMaxBytes,
        batchingMaxMessages,
        batchingMaxPublishDelaySeconds,
        blockIfQueueFull,
        compressionType,
        cryptoFailureAction,
        defaultCryptoKeyReader,
        encryptionKey,
        enableBatching,
        enableChunking,
        enableLazyStartPartitionedProducers,
        enableMultiSchema,
        hashingScheme,
        messageRoutingMode,
        properties,
        roundRobinRouterBatchingPartitionSwitchFrequency,
        sendTimeoutSeconds,
    )
  }
}

