package net.incongru.brewt

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.Foundation.NSBundle
import platform.posix.getenv
import platform.posix.getuid

@OptIn(ExperimentalForeignApi::class)
fun makeEnv(): Env {
    return Env(
        selfBinaryPath = selfBinaryPath(),
        userId = getuid().toString(),
        userName = env("USER"),
        userHome = env("HOME"),
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun env(name: String): String {
    val value = getenv(name)?.toKString()
    require(!value.isNullOrBlank()) { "$name environment variable not available" }
    return value
}

private fun selfBinaryPath(): String {
    val res = NSBundle.mainBundle.executablePath
    require(!res.isNullOrBlank()) { "Can't resolve path to current executable" }
    return res
}
