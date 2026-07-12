package net.incongru.brewt

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.findOrSetObject
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import kotlinx.cinterop.ExperimentalForeignApi

class BrewtCLI(val brewt: Brewt) : CliktCommand() {
    override val invokeWithoutSubcommand = true

    val cfg by findOrSetObject { brewt.readConfig() }

    override fun run() {
        if (currentContext.invokedSubcommand == null || currentContext.invokedSubcommand is UpdateAllTheThings) {
            doTheThing(brewt, cfg)
        }
    }
}

// Just a placeholder which we might as well remove, since we let the root Brewt cmd do the job
class UpdateAllTheThings : NoOpCliktCommand() {}

class Schedule(val brewt: Brewt) : CliktCommand() {
    private val hour by argument()
    private val minute by argument()
    override fun help(context: Context): String {
        return "Schedule updates"
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun run() {
        Scheduler(brewt).enable(hour, minute)
        brewt.log.info("schedule done")
    }
}

fun main(args: Array<String>) {
    val brewt = Brewt(makeLogger(LoggerMode.VERBOSE))
    BrewtCLI(brewt).subcommands(
        UpdateAllTheThings(), Schedule(brewt)
    ).main(args)
}