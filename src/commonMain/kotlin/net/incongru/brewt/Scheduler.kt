package net.incongru.brewt

import okio.IOException
import okio.Path
import okio.Path.Companion.toPath

private const val APP_ID = "net.incongru.brewt"

data class Schedule(
    val minute: Int?, val hour: Int?,
    val day: Int?, val weekday: Int?, val month: Int?
) {

    // Minute <integer> The minute (0-59) on which this job will be run.
    // Hour <integer> The hour (0-23) on which this job will be run.
    // Day <integer> The day of the month (1-31) on which this job will be run.
    // Weekday <integer> The weekday on which this job will be run (0 and 7 are Sunday).
    // If both Day and Weekday are specificed [sic], then the job will be started if either one matches the current date.
    // Month <integer> The month (1-12) on which this job will be run.
    fun toDictXML() {
        error("not implemented yet")
    }
}

class Scheduler(val brewt: Brewt) {

    private val plistPath: Path
        get() {
            return brewt.env.userHome.toPath().resolve("Library/LaunchAgents/$APP_ID.plist")
        }

    fun enable(hour: String, minutes: String) {
        disable()
        val plistContents = genPlist(
            brewt.env.selfBinaryPath, hour, minutes, brewt.env.userHome // TODO use appdirs.logFolder ?
        )
        writeFile(plistPath, plistContents)

        brewt.log.info("Enabling $plistPath (to persist across logins)")
        brewt.sh("launchctl enable gui/${brewt.env.userId}/$APP_ID")

        brewt.log.info("Boostrapping $plistPath ...")
        brewt.sh("launchctl bootstrap gui/${brewt.env.userId} $plistPath")

        check()
    }

    enum class Status {
        NOT_FOUND,
        FOUND
    }

    fun check(): Status {
        // Verify it's registered
        val res = brewt.sh.withoutThrowing("launchctl print gui/${brewt.env.userId}/$APP_ID")
        // man launchctl tells us this isn't API but what am i gonna do...
        if (res.output.contains("Could not find service")) {
            return Status.NOT_FOUND
        }
        if (res.ok) {
            return Status.FOUND
        }
        error("can't handle launchctl output, see logs for details")

        // # Run it right now to test, without waiting for 13:23
        // launchctl kickstart -k gui/$UID/net.incongru.brewt

        /**


        la_status() {  # usage: la_status <label> uid
        local label="$1" uid="${2:-$(id -u)}"
        local db="/var/db/com.apple.xpc.launchd/disabled.$uid.plist"

        [ -f "$HOME/Library/LaunchAgents/$label.plist" ] \
        && echo "file:         present" || echo "file:         absent"

        launchctl print "gui/$uid/$label" >/dev/null 2>&1 \
        && echo "bootstrapped: yes" || echo "bootstrapped: no"

        local d
        d=$(plutil -convert json -o - "$db" 2>/dev/null | jq -r --arg l "$label" '. [ $ l ] // false')
        [ "$d" = "true" ] && echo "disabled:     yes" || echo "disabled:     no"
        }
        # la_status net.incongru.brewt

        (Using jq --arg l … '. [ $ l ]' sidesteps the dotted-key problem entirely — no escaping needed, unlike plutil
        -extract.)

        Caveats

        - Exit codes aren't formally documented API either — but they're far more stable than scraping output,
        and 113 (could not find) has been consistent for years. Confidence: high, though not contractually
        guaranteed.
        - The disabled-db plist reflects persistent state, not live memory. If something was disabled/enabled via
        direct file edit this session (like our earlier cleanup), the file and launchd's in-memory view diverge
        until reboot. After a normal launchctl enable/disable they stay in sync. So this check answers "what will
        it be after reboot / what's on disk," which is usually what you want.
        - "Currently running" (has a PID) is the one thing with no clean signal — the PID only appears in
        print/list text output, which is non-API. For a RunAtLoad=false scheduled agent that's momentary anyway;
        if you truly need it, parse launchctl list <label>'s "PID" key and accept the fragility.
         */
    }

    fun disable() {
        if (check() == Status.FOUND) {
            brewt.log.info("Un-boostrapping $plistPath ...")
            // Boot-out failed: 3: No such process
            brewt.sh("launchctl bootout gui/${brewt.env.userId} $plistPath")
        }
        brewt.log.info("Disabling $APP_ID (to persist across logins)")
        // seems to succeed always ?
        brewt.sh("launchctl disable gui/${brewt.env.userId}/$APP_ID")

        brewt.log.info("Removing $plistPath ...")
        brewt.fileSystem.delete(plistPath, false)
    }

    @Throws(IOException::class)
    private fun writeFile(path: Path, contents: String) {
        brewt.log.info("Writing to $path ...")
        brewt.fileSystem.write(path) {
            writeUtf8(contents)
        }
    }

    private fun genPlist(
        binaryLocation: String, hour: String, minutes: String, logFolder: String
    ): String {
        return """
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>net.incongru.brewt</string>
    <key>ProgramArguments</key>
    <array>
        <string>${binaryLocation}</string>
    </array>
    <key>StartCalendarInterval</key>
    <array>
        <dict>
            <key>Hour</key>
            <integer>${hour}</integer>
            <key>Minute</key>
            <integer>${minutes}</integer>
        </dict>
    </array>
    <!--
    <key>KeepAlive</key>
    <false/>
    -->
    <key>RunAtLoad</key>
    <false/>
    <key>StandardOutPath</key>
    <string>${logFolder}/.brewt.out.log</string>
    <key>StandardErrorPath</key>
    <string>${logFolder}/.brewt.err.log</string>
</dict>
</plist>
            """.trimIndent()
    }

}