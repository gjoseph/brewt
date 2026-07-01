package net.incongru.brewt

expect fun makeShell(logger: Logger): Shell

/**
 * Functional wrapper around #runCommand which throws on exit code !=0, streams output to stdout and doesn't return anything.
 */
interface Shell {
    operator fun invoke(command: String): ShellResult
    fun withoutThrowing(command: String): ShellResult
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

abstract class AbstractShellImpl : Shell {
    override operator fun invoke(command: String): ShellResult {
        return invoke(command, true)
    }

    override fun withoutThrowing(command: String): ShellResult {
        return invoke(command, false)
    }

    private fun invoke(command: String, orThrow: Boolean): ShellResult {
        val res = runCommand(command, ::println, mergeStderr = true)
        // fake a default value of true since functional interfaces can't specify default values
        return if (orThrow) res.orThrow() else res;
    }

    abstract fun runCommand(
        command: String,
        // TODO merge with Logger
        onOutput: ((String) -> Unit)? = ::println,
        mergeStderr: Boolean = true,
    ): ShellResult

}
