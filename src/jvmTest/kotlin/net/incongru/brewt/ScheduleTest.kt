package net.incongru.brewt

import io.kotest.core.spec.style.FunSpec
import kotlinx.datetime.LocalTime

class ScheduleTest : FunSpec({
    context("schedule-to-plist") {
        test("daily, hour and minute") {
            DailySchedule(LocalTime.parse("1:23") ).toDictXML() shouldBePlistLike """
                |<dict>
                        |<key>Hour</key>
                        |<integer>1</integer>
                        |<key>Minute</key>
                        |<integer>23</integer>
                        |</dict>""".trimMargin()
        }
    }
})
