package com.dhenry.glia.annotations

@Target(AnnotationTarget.CLASS)
@MustBeDocumented
annotation class Event(val routingKey: String, val version: String = "1")