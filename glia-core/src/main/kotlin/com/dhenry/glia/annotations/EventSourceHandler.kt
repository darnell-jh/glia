package com.dhenry.glia.annotations

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
annotation class EventSourceHandler(val value: KClass<*> = Any::class)