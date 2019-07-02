package com.dhenry.glia.cassandra.domain.aggregate

import java.util.*

class AggregateHelper {

  enum class Mode {
    KEEP_LATEST, KEEP_OLDEST
  }

  companion object {
    fun <T: AbstractAggregateRoot<T>> with(aggregate: T, mode: Mode = Mode.KEEP_LATEST): AggregateUpdater<T> {
      return AggregateUpdater(aggregate, mode)
    }
  }

  class AggregateUpdater<T: AbstractAggregateRoot<T>> (private val aggregate: T, private val mode: Mode) {
    fun <R> update(olderValue: R, currentPropertyValue: (t: T) -> R, setter: (r: R) -> Unit) {
      when (mode) {
        Mode.KEEP_LATEST ->
          setter(Optional.ofNullable(currentPropertyValue(aggregate)).orElse(olderValue))
        Mode.KEEP_OLDEST ->
          setter(olderValue)
      }
    }

  }
}