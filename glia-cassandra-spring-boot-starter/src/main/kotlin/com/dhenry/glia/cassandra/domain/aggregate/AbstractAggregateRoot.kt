package com.dhenry.glia.cassandra.domain.aggregate

import com.dhenry.glia.annotations.Event
import com.dhenry.glia.annotations.EventSourceHandler
import com.dhenry.glia.cassandra.domain.models.AggregatePrimaryKey
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.valueParameters

abstract class AbstractAggregateRoot<A : AbstractAggregateRoot<A>>(
    override val aggregatePrimaryKey: AggregatePrimaryKey
): BaseAbstractAggregateRoot<A>(aggregatePrimaryKey) {

  var latestOnly: Boolean = false
    protected set(enable) {
      field = enable
    }

  var fromOldest: Boolean = false
    protected set(enable) {
      field = enable
    }

  val classToHandler: Map<KClass<*>, KFunction<*>> by lazy {
    val classToHandlerAux = mutableMapOf<KClass<*>, KFunction<*>>()
    (this::class.supertypes[0].arguments[0].type!!.classifier as KClass<*>).memberFunctions
        .forEach { function ->
          val annotation = function.findAnnotation<EventSourceHandler>() ?: return@forEach

          if (annotation.value !== Any::class) {
            classToHandlerAux.putUnique(annotation.value, function)
          } else {
            function.valueParameters
                .map { (it.type.classifier as KClass<*>) }
                .filter { it.findAnnotation<Event>() != null }
                .forEach { classToHandlerAux.putUnique(it, function) }
          }
        }
    classToHandlerAux.toMap()
  }

  private fun <K, V> MutableMap<K, V>.putUnique(key: K, value: V) {
    if (this.putIfAbsent(key, value) != null)
      throw IllegalAccessException("Key $key already exists")
  }

  /**
   * Determines if the aggregate is considered fully populated. Override this when mode is set to LATEST_COMPLETE to
   * stop populating aggregate once all the required properties are populated.
   */
  fun fullyPopulated(): Boolean {
    return false
  }

}