package net.incongru.brewt

import kotlin.time.Clock

enum class LoggerMode { VERBOSE, NORMAL, QUIET }
enum class LogLevel { DEBUG, INFO, WARN, ERROR }

fun makeLogger(mode: LoggerMode): Logger = LoggerImpl(mode)

interface Logger {
    operator fun invoke(level: LogLevel, message: String)

    fun debug(message: String) {
        invoke(LogLevel.DEBUG, message)
    }

    fun info(message: String) {
        invoke(LogLevel.INFO, message)
    }

    fun warn(message: String) {
        invoke(LogLevel.WARN, message)
    }

    fun error(message: String) {
        invoke(LogLevel.ERROR, message)
    }
}

private class LoggerImpl(val mode: LoggerMode) : Logger {
    override fun invoke(level: LogLevel, message: String) {
        when (level) {
            LogLevel.DEBUG -> if (this.mode == LoggerMode.VERBOSE) this.logIt(level, message)
            LogLevel.INFO -> if (this.mode >= LoggerMode.NORMAL) this.logIt(level, message)
            LogLevel.WARN -> if (this.mode > LoggerMode.QUIET) this.logIt(level, message)
            LogLevel.ERROR -> if (this.mode > LoggerMode.QUIET) this.logIt(level, message)
        }
    }

    private fun logIt(level: LogLevel, message: String) {
        println("${Clock.System.now()} $level $message")
    }
}
