package net.incongru.brewt

import kotlin.time.Clock

typealias Logger = (String) -> Unit

val log: (String) -> Unit = { msg ->
    println("${Clock.System.now()} $msg")
}