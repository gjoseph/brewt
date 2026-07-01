package net.incongru.brewt

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.findOrSetObject
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument

fun startCLI(brewt: Brewt, args: Array<String>) {
    BrewtCLI(brewt).subcommands(
        UpdateAllTheThings(),
        ScheduleCmd(brewt)
    ).main(args)
}

private class BrewtCLI(val brewt: Brewt) : CliktCommand() {
    override val invokeWithoutSubcommand = true

    val cfg by findOrSetObject { brewt.readConfig() }

    override fun run() {
        brewt.log.debug("Env: ${brewt.env}")
        brewt.log.debug("Configuration: $cfg")

        // No subcommand (or UpdateAllTheThings) specified, run update:
        if (currentContext.invokedSubcommand == null || currentContext.invokedSubcommand is UpdateAllCmd) {
            doTheThing(brewt, cfg)
        }
    }
}

// Just a placeholder which we might as well remove, since we let the root Brewt cmd do the job
class UpdateAllCmd : NoOpCliktCommand("update-all") {}

class ScheduleCmd(val brewt: Brewt) : CliktCommand("schedule") {
    private val hour by argument()
    private val minute by argument()
    override fun help(context: Context): String {
        return "Schedule updates"
    }

    override fun run() {
        Scheduler(brewt).enable(hour, minute)
        brewt.log.info("schedule done")
    }
}
