package net.incongru.brewt

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import platform.posix.fread
import platform.posix.pclose
import platform.posix.popen

actual fun makeShell(logger: Logger): Shell = ShellImpl(logger)

class ShellImpl(val log: Logger) : AbstractShellImpl() {
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
     *
     * Note ! : avoid using directly, prefer the operator function, or withoutThrowing()
     */
    @OptIn(ExperimentalForeignApi::class)
    override fun runCommand(
        command: String,
        // TODO merge with Logger
        onOutput: ((String) -> Unit)?,
        mergeStderr: Boolean,
    ): ShellResult {
        val shellCommand = if (mergeStderr) "{ $command ; } 2>&1" else command
        this.log.debug("Executing $shellCommand:")
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
}
