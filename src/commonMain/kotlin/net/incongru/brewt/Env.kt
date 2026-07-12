package net.incongru.brewt

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv
import platform.posix.getuid

/**
 * Tiny wrapper around some environment variables/values we need
 */
data class Env(
    val selfBinaryPath: String,
    val userId: String,
    val userName: String,
    val userHome: String,
) {
    companion object {
        @OptIn(ExperimentalForeignApi::class)
        fun make(): Env {
            return Env(
                selfBinaryPath = selfBinaryPath(),
                userId = getuid().toString(),
                userName = env("USER"),
                userHome = env("HOME"),
            )
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun env(name: String): String {
    val value = getenv(name)?.toKString()
        require(!value.isNullOrBlank()) { "$name environment variable not available" }
    return value
}

private fun selfBinaryPath(): String {
    val res = currentBinaryPath()
    require(!res.isNullOrBlank()) { "Can't resolve path to current executable" }
    return res
}

expect fun currentBinaryPath(): String?
