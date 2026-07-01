package net.incongru.brewt

import io.kotest.core.spec.style.FunSpec

class SchedulerTest : FunSpec({
    context("schedule-to-plist") {
        test("daily, hour and minute") {
            Schedule(minute = 23, hour = 1).toDictXML() shouldBePlistLike """
                |<dict>
                        |<key>Hour</key>
                        |<integer>1</integer>
                        |<key>Minute</key>
                        |<integer>23</integer>
                        |</dict>""".trimMargin()
        }
    }
})
