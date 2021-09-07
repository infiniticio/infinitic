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

package io.infinitic.pulsar.topics

import io.infinitic.pulsar.PulsarInfiniticAdmin
import io.infinitic.transport.pulsar.topicPolicies.TopicPolicy
import mu.KotlinLogging
import org.apache.pulsar.client.admin.PulsarAdminException
import org.apache.pulsar.client.admin.Topics
import org.apache.pulsar.common.policies.data.DelayedDeliveryPolicies
import org.apache.pulsar.common.policies.data.RetentionPolicies

interface TopicSet {
    val prefix: String
}

internal fun Topics.createInfiniticPartitionedTopic(
    topicName: String,
    withDelay: Boolean,
    topicPolicy: TopicPolicy?,
) {
    createPartitionedTopic(topicName, 1)

    if (topicPolicy != null) {
        try {
            applyInfiniticTopicPolicy(topicName, topicPolicy, withDelay)
        } catch (e: PulsarAdminException.NotAllowedException) {
            val logger = KotlinLogging.logger(PulsarInfiniticAdmin::class.java.name)
            logger.warn {
                "Unable to set topic policy: " +
                    "please check that your namespace has recommended settings" +
                    " (see ${TopicPolicy::class.java.name})"
            }
        }
    }
}

private fun Topics.applyInfiniticTopicPolicy(topicName: String, topicPolicy: TopicPolicy, withDelay: Boolean) {
    enableDeduplication(topicName, topicPolicy.deduplicationEnabled)
    setRetention(
        topicName,
        RetentionPolicies(
            topicPolicy.retentionTimeInMinutes,
            topicPolicy.retentionSizeInMB
        )
    )
    setMessageTTL(topicName, topicPolicy.messageTTLInSeconds)
    topicPolicy.maxMessageSize?.also { setMaxMessageSize(topicName, it) }
    setDelayedDeliveryPolicy(
        topicName,
        DelayedDeliveryPolicies(topicPolicy.delayedDeliveryTickTimeMillis, withDelay)
    )
}
