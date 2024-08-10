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
package io.infinitic.pulsar.config.policies

import org.apache.pulsar.common.policies.data.SchemaCompatibilityStrategy

@Suppress("unused")
data class PoliciesConfig(
    // Retain messages for 7 days
  val retentionTimeInMinutes: Int = 60 * 24 * 7,
    // Retain messages up to 1GB
  val retentionSizeInMB: Long = 1024,
    // Expire messages after 14 days
  val messageTTLInSeconds: Int = 3600 * 24 * 14,
    // Expire delayed messages after 1 year
  val delayedTTLInSeconds: Int = 3600 * 24 * 366,
    // Delayed delivery tick time = 1 second
  val delayedDeliveryTickTimeMillis: Long = 1000,
    // Changes allowed: add optional fields, delete fields
  val schemaCompatibilityStrategy: SchemaCompatibilityStrategy = SchemaCompatibilityStrategy.BACKWARD_TRANSITIVE,
    // Disallow auto topic creation
  val allowAutoTopicCreation: Boolean = false,
    // Enforce schema validation
  val schemaValidationEnforced: Boolean = true,
    // Allow auto update schema
  val isAllowAutoUpdateSchema: Boolean = true,
    // Enable message deduplication
  val deduplicationEnabled: Boolean = true,
) {

  companion object {
    @JvmStatic
    fun builder() = PoliciesConfigBuilder()
  }

  /**
   * PoliciesConfig builder (Useful for Java user)
   */
  class PoliciesConfigBuilder {
    private val default = PoliciesConfig()

    private var retentionTimeInMinutes = default.retentionTimeInMinutes
    private var retentionSizeInMB = default.retentionSizeInMB
    private var messageTTLInSeconds = default.messageTTLInSeconds
    private var delayedTTLInSeconds = default.delayedTTLInSeconds
    private var delayedDeliveryTickTimeMillis = default.delayedDeliveryTickTimeMillis
    private var schemaCompatibilityStrategy = default.schemaCompatibilityStrategy
    private var allowAutoTopicCreation = default.allowAutoTopicCreation
    private var schemaValidationEnforced = default.schemaValidationEnforced
    private var isAllowAutoUpdateSchema = default.isAllowAutoUpdateSchema
    private var deduplicationEnabled = default.deduplicationEnabled

    fun setRetentionTimeInMinutes(retentionTimeInMinutes: Int) =
        apply { this.retentionTimeInMinutes = retentionTimeInMinutes }

    fun setRetentionSizeInMB(retentionSizeInMB: Long) =
        apply { this.retentionSizeInMB = retentionSizeInMB }

    fun setMessageTTLInSeconds(messageTTLInSeconds: Int) =
        apply { this.messageTTLInSeconds = messageTTLInSeconds }

    fun setDelayedTTLInSeconds(delayedTTLInSeconds: Int) =
        apply { this.delayedTTLInSeconds = delayedTTLInSeconds }

    fun setDelayedDeliveryTickTimeMillis(delayedDeliveryTickTimeMillis: Long) =
        apply { this.delayedDeliveryTickTimeMillis = delayedDeliveryTickTimeMillis }

    fun setSchemaCompatibilityStrategy(schemaCompatibilityStrategy: SchemaCompatibilityStrategy) =
        apply { this.schemaCompatibilityStrategy = schemaCompatibilityStrategy }

    fun setAllowAutoTopicCreation(allowAutoTopicCreation: Boolean) =
        apply { this.allowAutoTopicCreation = allowAutoTopicCreation }

    fun setSchemaValidationEnforced(schemaValidationEnforced: Boolean) =
        apply { this.schemaValidationEnforced = schemaValidationEnforced }

    fun setIsAllowAutoUpdateSchema(isAllowAutoUpdateSchema: Boolean) =
        apply { this.isAllowAutoUpdateSchema = isAllowAutoUpdateSchema }

    fun setDeduplicationEnabled(deduplicationEnabled: Boolean) =
        apply { this.deduplicationEnabled = deduplicationEnabled }

    fun build() = PoliciesConfig(
        retentionTimeInMinutes,
        retentionSizeInMB,
        messageTTLInSeconds,
        delayedTTLInSeconds,
        delayedDeliveryTickTimeMillis,
        schemaCompatibilityStrategy,
        allowAutoTopicCreation,
        schemaValidationEnforced,
        isAllowAutoUpdateSchema,
        deduplicationEnabled,
    )
  }
}
