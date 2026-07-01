package net.incongru.brewt

actual fun makeEnv(): Env =
    error("makeEnv() is native-only; the JVM target exists for tests — inject a fake Env")

actual fun makeShell(logger: Logger): Shell =
    error("makeShell() is native-only; the JVM target exists for tests — inject a fake Shell")
