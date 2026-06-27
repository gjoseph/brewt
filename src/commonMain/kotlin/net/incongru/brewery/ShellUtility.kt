package net.incongru.brewery

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.refTo
import kotlinx.cinterop.set
import kotlinx.cinterop.value
import kotlinx.serialization.json.Json
import platform.posix.execvp
import platform.posix.exit
import platform.posix.fork
import platform.posix.fread
import platform.posix.pclose
import platform.posix.popen
import platform.posix.system
import platform.posix.waitpid

//import platform.darwin.*

val json: Json by lazy {
    Json {
        ignoreUnknownKeys = true
    }
}

@OptIn(ExperimentalForeignApi::class)
fun String.runCommand(exitOnFail: Boolean = true): Int {
    val ret = system(this)
    if (ret != 0 && exitOnFail) {
        exit(ret)
    }
    return ret
}


@OptIn(ExperimentalForeignApi::class)
inline fun <reified T> String.runCommandAndCaptureOutputAs(): T {
    val outputStr = this.runCommandAndCaptureOutput()
    return json.decodeFromString<T>(outputStr)
}

@OptIn(ExperimentalForeignApi::class)
// Not sure there's a simple variation of this to also capture stderr and exit code like the below
fun String.runCommandAndCaptureOutput(): String {
    val fp = popen(this, "r") ?: return ""
    try {
        return buildString {
            val buffer = ByteArray(4096)
            while (true) {
                val read = fread(buffer.refTo(0), 1u, buffer.size.toULong(), fp)
                if (read == 0uL) break
                append(buffer.decodeToString(0, read.toInt()))
            }
        }
    } finally {
        pclose(fp)
    }
}

data class CommandResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
)

@OptIn(ExperimentalForeignApi::class)
fun executeCommandArray(program: String, args: List<String>): CommandResult? {
    val pid = fork()
    println("pid = ${pid}")
    when {
        pid < 0 -> {
            // Fork failed
            throw Error("What the fork!?")
        }

        pid == 0 -> {
            println("start child process?")
            // Child process
            memScoped {
                val argv = allocArray<CPointerVar<ByteVar>>(args.size + 2)
                argv[0] = program.cstr.ptr
                args.forEachIndexed { i, arg -> argv[i + 1] = arg.cstr.ptr }
                argv[args.size + 1] = null

                execvp(program, argv)
                // If execvp returns, it failed
                println("WHAT THE")
                exit(127)
            }
            return null // Unreachable
        }

        else -> {
            // Parent process
            memScoped {
                val status = alloc<IntVar>()
                waitpid(pid, status.ptr, 0)
                val exitCode = if (WIFEXITED(status.value)) {
                    WEXITSTATUS(status.value)
                } else {
                    -1
                }
                println("exitCode = ${exitCode}")
                return CommandResult(exitCode, "", "")
            }
        }
    }
}


// these are macros that could be import from platform.posix.* or platrform.darwin.* but not both, so we just reimplement
// thank Claude.ai for the splat
private fun WIFEXITED(status: Int): Boolean = (status and 0x7f) == 0
private fun WEXITSTATUS(status: Int): Int = (status shr 8) and 0xff
private fun WIFSIGNALED(status: Int): Boolean = (status and 0x7f) != 0 && (status and 0x7f) != 0x7f
private fun WTERMSIG(status: Int): Int = status and 0x7f
