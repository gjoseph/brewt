package net.incongru.brewt

import okio.FileSystem

fun main(args: Array<String>) {

    // TODO might have to review this when we want logger to be --quiet or --verbose
    val log = makeLogger(LoggerMode.VERBOSE)
    val env = makeEnv()
    val sh: Shell = ShellImpl(log)
    val brewt = Brewt(log, env, sh, FileSystem.SYSTEM)

    startCLI(brewt, args)
}
