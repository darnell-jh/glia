package com.dhenry.glia.cassandra.domain.services

import com.dhenry.glia.annotations.EventSourceHandler
import com.dhenry.glia.cassandra.domain.aggregate.AbstractAggregateRoot
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.javaType

class AggregationService {

    companion object {
        inline fun <reified T: AbstractAggregateRoot<*>> invokeEventListeners(aggregate: T, event: Any) {
            val eventSourceHandlers = aggregate::class.memberFunctions
                .filter { it.findAnnotation<EventSourceHandler>() != null }
            for (handlerFun in eventSourceHandlers) {
                // Find EventSourceHandler functions
                val annotation: EventSourceHandler = handlerFun.findAnnotation()!!
                if (annotation.value === Any::class &&
                        handlerFun.valueParameters[0].type.javaType === event::class.java) {
                    // Call function with the matching payload
                    handlerFun.call(aggregate, event)
                }
            }
        }
    }
}