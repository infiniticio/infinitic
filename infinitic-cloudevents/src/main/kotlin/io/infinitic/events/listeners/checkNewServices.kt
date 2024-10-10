package io.infinitic.events.listeners

import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.transport.InfiniticResources
import io.infinitic.common.transport.logged.LoggedInfiniticResources
import io.infinitic.events.EventListener
import io.infinitic.events.config.EventListenerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

context(CoroutineScope)
fun InfiniticResources.checkNewServices(
  config: EventListenerConfig,
  start: suspend (ServiceName) -> Unit
) = launch {
  val processedServices = mutableSetOf<String>()
  val loggedResources = LoggedInfiniticResources(EventListener.logger, this@checkNewServices)

  while (true) {
    // Retrieve the list of services
    loggedResources.getServices().onSuccess { services ->
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
