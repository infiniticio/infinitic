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

data class Policies @JvmOverloads constructor(
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
)
