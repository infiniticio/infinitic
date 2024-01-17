package io.infinitic.common.events

import io.cloudevents.CloudEvent

interface CloudEventListener {

  fun onCloudEvent(event: CloudEvent)

}
