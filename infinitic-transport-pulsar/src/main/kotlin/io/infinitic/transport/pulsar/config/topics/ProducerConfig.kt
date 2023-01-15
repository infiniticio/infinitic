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
package io.infinitic.transport.pulsar.config.topics

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
