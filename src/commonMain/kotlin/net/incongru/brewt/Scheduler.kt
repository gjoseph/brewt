package net.incongru.brewt

import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath

private const val APP_ID = "net.incongru.brewt"

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
        brewt.log.info("Boostrapping $plistPath ...")
        brewt.sh("launchctl bootstrap gui/${brewt.env.userId} $plistPath")

        brewt.log.info("Enabling $plistPath (to persist across logins)")
        brewt.sh("launchctl enable gui/${brewt.env.userId}/$APP_ID")
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
        FileSystem.SYSTEM.delete(plistPath, false)
    }

    @Throws(IOException::class)
    private fun writeFile(path: Path, contents: String) {
        // TODO inject FileSystem to testability ?
        FileSystem.SYSTEM.write(path) {
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