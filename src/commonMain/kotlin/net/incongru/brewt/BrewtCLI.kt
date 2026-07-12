package net.incongru.brewt

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.findOrSetObject
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

fun startCLI(brewt: Brewt, args: Array<String>) {
    BrewtCLI(brewt).subcommands(
        UpdateAllCmd(),
        ScheduleCmd(brewt)
    ).main(args)
}

private class BrewtCLI(val brewt: Brewt) : CliktCommand() {
    override val invokeWithoutSubcommand = true

    val cfg by findOrSetObject { brewt.readConfig() }

    override fun run() {
        brewt.log.debug("Env: ${brewt.env}")
        brewt.log.debug("Configuration: $cfg")

        // No subcommand (or UpdateAllCmd) specified, run update:
        if (currentContext.invokedSubcommand == null || currentContext.invokedSubcommand is UpdateAllCmd) {
            updateAll(BrewWrapper(brewt), ApplescriptHelper(brewt.sh), brewt.log, cfg)
        }
    }
}

// Just a placeholder which we might as well remove, since we let the root Brewt cmd do the job
class UpdateAllCmd : NoOpCliktCommand("update-all") {}

class ScheduleCmd(val brewt: Brewt) : CliktCommand("schedule") {
    // todo: subcommands: add, replace, remove-all

    // these could actually be sub-commands too
    val schedule: Schedule? by mutuallyExclusiveOptions<Schedule>(
        option("--daily").convert { DailySchedule(parseTimeArg(it)) },
        option("--weekly").convert { parseWeekly(it) },
        option("--monthly").convert { parseMonthly(it) }
    ).required()

    private fun parseMonthly(s: String): MonthlySchedule {
        TODO("Not yet implemented")
    }

    fun parseWeekly(s: String): WeeklySchedule {
        val match = Regex("^(?<day>[A-Za-z]{3,9})?@(?<time>\\d{1,2}(?::\\d{1,2}]))$").matchEntire(s)
        val dayStr = match?.groups?.get("day")?.value ?: "MoN"
            .also { println("day : " + it) }
            .uppercase().substring(0, 3)

        val day = when (dayStr) {
            "MON" -> DayOfWeek.MONDAY
            "TUE" -> DayOfWeek.TUESDAY
            "WED" -> DayOfWeek.WEDNESDAY
            "THU" -> DayOfWeek.THURSDAY
            "FRI" -> DayOfWeek.FRIDAY
            "SAT" -> DayOfWeek.SATURDAY
            "SUN" -> DayOfWeek.SUNDAY
            else -> throw IllegalArgumentException("Invalid day of week: $dayStr")
        }
        val timeStr = match?.groups?.get("time")?.value?:"07:00"

        return WeeklySchedule(day, parseTimeArg(timeStr))
    }

    // TODO parses 04:56 successfully but not 4:56
    private fun parseTimeArg(string: String): LocalTime = LocalTime.parse(string)

    /**

    --daily <optional time HH:MM, default to 7:00>
    --weekly <optional day of week and time, Mon|Tue|Wed|Thu|Fri|Sat|Sun@HH:MM>, default to sun 7:00
    --monthly <optional day of month and time, D@HH:MM default to 1 7:00

    OR

    --freq daily|weekly|monthly
    --at <optional, depends on above>

     */

    override fun help(context: Context): String {
        return "Schedule updates"
    }

    override fun run() {
        if (schedule == null) {
            error("no schedule specified")
        }
        val schedule = schedule!! // capture a local copy of the delegate property
        Scheduler(brewt).enable(schedule)
        brewt.log.info("Scheduling done for ${schedule}")
        // TODO print current schedule(s)
    }
}
