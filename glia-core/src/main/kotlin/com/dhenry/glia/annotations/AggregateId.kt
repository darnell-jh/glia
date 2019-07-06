package com.dhenry.glia.annotations

/**
 * Denotes an aggregate ID
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY)
@MustBeDocumented
annotation class AggregateId {
}