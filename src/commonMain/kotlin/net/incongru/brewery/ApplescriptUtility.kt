package net.incongru.brewery

 fun notif(message: String) {
    "display notification \"$message\" with title \"🥜Brew upgrade\" sound name \"Frog\"".runAsAppleScript()
}
fun confirm(message: String): Boolean {
    // if we have a cancel button, we can use exit code of the process since it yields an error
    return "display alert \"🥜 Brew upgrade\" message \"$message\" buttons {\"No\", \"🥜 Yes\"} default button 2 cancel button 1"
        .runAsAppleScript(exitOnFail = false, noOutput = true) == 0
}

private fun String.runAsAppleScript(exitOnFail: Boolean = true, noOutput: Boolean = false): Int {
    // -s e : print errors on stderr (default)
    // -s o : print errors on stdout
    // -s h : print result in human-readable form (default).
    // -s s : print result in parseable form
    // If using "noOutput", we'll use -sh to avoid `""` in the logs; otherwise we'll use -ss which is vaguely more parseable.
    // `tell result to return` would mean "return nothing"; with -ss that would still print a `""`.
    // `tell result to return button returned` or `tell result to return someVar` -- this explicitly makes the script return nothing
    val script = this // if (noOutput) "$this\ntell result to return" else this
    val flags = if (noOutput) "-s h > /dev/null" else "-s s"
    return "echo '$script' | osascript -s o ${flags}".runCommand(exitOnFail)
}
