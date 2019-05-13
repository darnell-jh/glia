package com.dhenry.glia.component.produced

import com.dhenry.glia.annotations.Event

const val ROUTING_KEY = "my.routing.key"

@Event(routingKey = ROUTING_KEY)
class ProducedEvent {
}