package com.dhenry.glia.service

import com.dhenry.glia.data.aggregate.AbstractAggregateRoot
import java.time.Instant

interface EventMediator {

  fun load(routingKey: String, payloadJson: String): Any
  fun publish(routingKey: String, payload: Any, aggregate: AbstractAggregateRoot<*, *>, event: Any, timestamp: Instant)

}