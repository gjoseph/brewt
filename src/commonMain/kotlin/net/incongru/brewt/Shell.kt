package net.incongru.brewt

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import kotlinx.serialization.json.Json
import platform.posix.fread
import platform.posix.pclose
import platform.posix.popen

val json: Json by lazy {
    Json {
        ignoreUnknownKeys = true
    }
}

/**
 * Simple wrapper around #runCommand which throws on exit code !=0, streams output to stdout and doesn't return anything.
 */
fun String.runCommand() {
    runCommand(this, ::println, mergeStderr = true).orThrow()
}

data class ShellResult(val exitCode: Int, val output: String) {
    val ok: Boolean get() = exitCode == 0
    fun orThrow(): ShellResult {
        return if (ok) this else error("Command failed (exit $exitCode): $output")
    }

    inline fun <reified T> jsonTo(): T {
        return json.decodeFromString<T>(output)
    }
}

/**
 * Runs [command] through the shell, returning its output and exit code.
 *
 * Output is always captured into the returned [ShellResult] for post-processing.
 * If [onOutput] is supplied, each chunk is also handed to it as it arrives — pass
 * `::print` to stream live to stdout, or a logger lambda to stream into your logs.
 *
 * stderr is folded into stdout by default. Set [mergeStderr] = false to capture stdout
 * only (e.g. parsing JSON); stderr then passes through to the terminal. The merge wraps
 * the whole command in a group — `{ cmd ; } 2>&1` — so it works for pipes and compound
 * commands too, not just the last element of the line.
 *
 * Note: output is read in raw byte chunks, so [onOutput] receives arbitrary fragments
 * (not whole lines) and a multi-byte UTF-8 char straddling a chunk boundary may be
 * garbled. Fine for typical brew/CLI text.
 */
@OptIn(ExperimentalForeignApi::class)
fun runCommand(
    command: String,
    // TODO replace with a function that knows about --quiet/--verbose
    onOutput: ((String) -> Unit)? = ::println,
    mergeStderr: Boolean = true,
): ShellResult {
    val shellCommand = if (mergeStderr) "{ $command ; } 2>&1" else command
    val fp = popen(shellCommand, "r") ?: return ShellResult(-1, "Failed to start: $command")
    val captured = buildString {
        val buffer = ByteArray(4096)
        while (true) {
            val n = fread(buffer.refTo(0), 1u, buffer.size.toULong(), fp)
            if (n == 0uL) break
            val chunk = buffer.decodeToString(0, n.toInt())
            append(chunk)
            onOutput?.invoke(chunk)
        }
    }
    return ShellResult((pclose(fp) shr 8) and 0xff, captured.trimEnd())
}
