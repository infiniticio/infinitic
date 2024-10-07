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
package io.infinitic.common.transport.logged

import io.github.oshai.kotlinlogging.KLogger
import io.infinitic.common.transport.InfiniticResources

class LoggedInfiniticResources(
  private val logger: KLogger,
  private val resources: InfiniticResources,
) : InfiniticResources {

  override suspend fun getServices(): Result<Set<String>> {
    logger.trace { "Getting list of services" }
    return resources.getServices().also { result ->
      result.onSuccess {
        logger.debug { "List of services: $it" }
      }
      result.onFailure {
        logger.error(it) { "Unable to retrieve the list of services" }
      }
    }
  }

  override suspend fun getWorkflows(): Result<Set<String>> {
    logger.trace { "Getting list of workflows" }
    return resources.getWorkflows().also { result ->
      result.onSuccess {
        logger.debug { "List of workflows: $it" }
      }
      result.onFailure {
        logger.error(it) { "Unable to retrieve the list of workflows" }
      }
    }
  }

  override suspend fun deleteTopicForClient(clientName: String): Result<String?> {
    logger.trace { "Deleting response topic for client $clientName" }
    return resources.deleteTopicForClient(clientName).also { result ->
      result.onSuccess {
        it?.let { logger.debug { "Response topic '$it' deleted" } }
      }
      result.onFailure {
        logger.error(it) {
          "Unable to delete response topic for client $clientName, please delete it manually"
        }
      }
    }
  }
}
