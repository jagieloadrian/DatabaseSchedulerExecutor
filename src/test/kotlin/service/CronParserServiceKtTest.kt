package service

import com.anjo.service.calculateNextExecutionDuration
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

class CronParserServiceKtTest {

    @Test
    fun `given cron when calculateNextExecutionDuration then return properly next exec time`() {
        //given
        val cron = "* * * * *"
        val expected = 60.seconds

        //when
        val actual = calculateNextExecutionDuration(cron)

        //then
        actual shouldBeLessThan expected
    }

    @Test
    fun `given invalid cron when calculateNextExecutionDuration then throw exception`() {
        //given
        val cron = "*****"
        val expectedMessage = "Cron expression contains 1 parts but we expect one of [5]"

        //when
        val exception = shouldThrow<IllegalArgumentException> { calculateNextExecutionDuration(cron) }

        //then
        exception.message shouldBe expectedMessage
    }

}