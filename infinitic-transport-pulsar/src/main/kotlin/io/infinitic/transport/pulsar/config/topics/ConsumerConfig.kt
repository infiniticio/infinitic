/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.transport.pulsar.config.topics

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
