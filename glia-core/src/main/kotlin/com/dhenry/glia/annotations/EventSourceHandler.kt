package com.dhenry.domain.annotations

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
annotation class EventSourceHandler(val value: KClass<*> = Any::class)