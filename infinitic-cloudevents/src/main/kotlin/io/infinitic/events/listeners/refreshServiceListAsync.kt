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
package io.infinitic.events.listeners

import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.transport.interfaces.InfiniticResources
import io.infinitic.events.config.EventListenerConfig
import kotlinx.coroutines.delay

internal suspend fun InfiniticResources.refreshServiceListAsync(
  config: EventListenerConfig,
  start: suspend (ServiceName) -> Unit
) {
  val processedServices = mutableSetOf<String>()

  while (true) {
    // Retrieve the list of services
    getServices().onSuccess { services ->
      val currentServices = services.filter { config.includeService(it) }

      // Determine new services that haven't been processed
      val newServices = currentServices.filterNot { it in processedServices }

      // Launch starter for each new service
      for (service in newServices) {
        start(ServiceName(service))
        // Add the service to the set of processed services
        processedServices.add(service)
      }
    }

    delay(config.serviceListConfig.listRefreshMillis)
  }
}
