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

package io.infinitic.pulsar.consumers

import org.apache.pulsar.client.api.ConsumerCryptoFailureAction

@Suppress("unused")
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
  val maxRedeliverCount: Int? = null
) {
  companion object {
    @JvmStatic
    fun builder() = ConsumerConfigBuilder()

    const val DEFAULT_MAX_REDELIVER_COUNT = 3
  }

  fun getMaxRedeliverCount() = maxRedeliverCount ?: DEFAULT_MAX_REDELIVER_COUNT


  /**
   * ConsumerConfig builder (Useful for Java user)
   */
  class ConsumerConfigBuilder {
    private val default = ConsumerConfig()
    private var loadConf = default.loadConf
    private var subscriptionProperties = default.subscriptionProperties
    private var ackTimeoutSeconds = default.ackTimeoutSeconds
    private var isAckReceiptEnabled = default.isAckReceiptEnabled
    private var ackTimeoutTickTimeSeconds = default.ackTimeoutTickTimeSeconds
    private var negativeAckRedeliveryDelaySeconds = default.negativeAckRedeliveryDelaySeconds
    private var defaultCryptoKeyReader = default.defaultCryptoKeyReader
    private var cryptoFailureAction = default.cryptoFailureAction
    private var receiverQueueSize = default.receiverQueueSize
    private var acknowledgmentGroupTimeSeconds = default.acknowledgmentGroupTimeSeconds
    private var replicateSubscriptionState = default.replicateSubscriptionState
    private var maxTotalReceiverQueueSizeAcrossPartitions =
        default.maxTotalReceiverQueueSizeAcrossPartitions
    private var priorityLevel = default.priorityLevel
    private var properties = default.properties
    private var autoUpdatePartitions = default.autoUpdatePartitions
    private var autoUpdatePartitionsIntervalSeconds = default.autoUpdatePartitionsIntervalSeconds
    private var enableBatchIndexAcknowledgment = default.enableBatchIndexAcknowledgment
    private var maxPendingChunkedMessage = default.maxPendingChunkedMessage
    private var autoAckOldestChunkedMessageOnQueueFull =
        default.autoAckOldestChunkedMessageOnQueueFull
    private var expireTimeOfIncompleteChunkedMessageSeconds =
        default.expireTimeOfIncompleteChunkedMessageSeconds
    private var startPaused = default.startPaused
    private var maxRedeliverCount = default.maxRedeliverCount

    fun loadConf(loadConf: Map<String, String>) =
        apply { this.loadConf = loadConf }

    fun subscriptionProperties(subscriptionProperties: Map<String, String>) =
        apply { this.subscriptionProperties = subscriptionProperties }

    fun ackTimeoutSeconds(ackTimeoutSeconds: Double) =
        apply { this.ackTimeoutSeconds = ackTimeoutSeconds }

    fun isAckReceiptEnabled(isAckReceiptEnabled: Boolean) =
        apply { this.isAckReceiptEnabled = isAckReceiptEnabled }

    fun ackTimeoutTickTimeSeconds(ackTimeoutTickTimeSeconds: Double) =
        apply { this.ackTimeoutTickTimeSeconds = ackTimeoutTickTimeSeconds }

    fun negativeAckRedeliveryDelaySeconds(negativeAckRedeliveryDelaySeconds: Double) =
        apply { this.negativeAckRedeliveryDelaySeconds = negativeAckRedeliveryDelaySeconds }

    fun defaultCryptoKeyReader(defaultCryptoKeyReader: String) =
        apply { this.defaultCryptoKeyReader = defaultCryptoKeyReader }

    fun cryptoFailureAction(cryptoFailureAction: ConsumerCryptoFailureAction) =
        apply { this.cryptoFailureAction = cryptoFailureAction }

    fun receiverQueueSize(receiverQueueSize: Int) =
        apply { this.receiverQueueSize = receiverQueueSize }

    fun acknowledgmentGroupTimeSeconds(acknowledgmentGroupTimeSeconds: Double) =
        apply { this.acknowledgmentGroupTimeSeconds = acknowledgmentGroupTimeSeconds }

    fun replicateSubscriptionState(replicateSubscriptionState: Boolean) =
        apply { this.replicateSubscriptionState = replicateSubscriptionState }

    fun maxTotalReceiverQueueSizeAcrossPartitions(maxTotalReceiverQueueSizeAcrossPartitions: Int) =
        apply {
          this.maxTotalReceiverQueueSizeAcrossPartitions = maxTotalReceiverQueueSizeAcrossPartitions
        }

    fun priorityLevel(priorityLevel: Int) =
        apply { this.priorityLevel = priorityLevel }

    fun properties(properties: Map<String, String>) =
        apply { this.properties = properties }

    fun autoUpdatePartitions(autoUpdatePartitions: Boolean) =
        apply { this.autoUpdatePartitions = autoUpdatePartitions }

    fun autoUpdatePartitionsIntervalSeconds(autoUpdatePartitionsIntervalSeconds: Double) =
        apply { this.autoUpdatePartitionsIntervalSeconds = autoUpdatePartitionsIntervalSeconds }

    fun enableBatchIndexAcknowledgment(enableBatchIndexAcknowledgment: Boolean) =
        apply { this.enableBatchIndexAcknowledgment = enableBatchIndexAcknowledgment }

    fun maxPendingChunkedMessage(maxPendingChunkedMessage: Int) =
        apply { this.maxPendingChunkedMessage = maxPendingChunkedMessage }

    fun autoAckOldestChunkedMessageOnQueueFull(autoAckOldestChunkedMessageOnQueueFull: Boolean) =
        apply {
          this.autoAckOldestChunkedMessageOnQueueFull = autoAckOldestChunkedMessageOnQueueFull
        }

    fun expireTimeOfIncompleteChunkedMessageSeconds(expireTimeOfIncompleteChunkedMessageSeconds: Double) =
        apply {
          this.expireTimeOfIncompleteChunkedMessageSeconds =
              expireTimeOfIncompleteChunkedMessageSeconds
        }

    fun startPaused(startPaused: Boolean) =
        apply { this.startPaused = startPaused }

    fun maxRedeliverCount(maxRedeliverCount: Int) =
        apply { this.maxRedeliverCount = maxRedeliverCount }

    fun build() = ConsumerConfig(
        loadConf,
        subscriptionProperties,
        ackTimeoutSeconds,
        isAckReceiptEnabled,
        ackTimeoutTickTimeSeconds,
        negativeAckRedeliveryDelaySeconds,
        defaultCryptoKeyReader,
        cryptoFailureAction,
        receiverQueueSize,
        acknowledgmentGroupTimeSeconds,
        replicateSubscriptionState,
        maxTotalReceiverQueueSizeAcrossPartitions,
        priorityLevel,
        properties,
        autoUpdatePartitions,
        autoUpdatePartitionsIntervalSeconds,
        enableBatchIndexAcknowledgment,
        maxPendingChunkedMessage,
        autoAckOldestChunkedMessageOnQueueFull,
        expireTimeOfIncompleteChunkedMessageSeconds,
        startPaused,
        maxRedeliverCount,
    )
  }
}
