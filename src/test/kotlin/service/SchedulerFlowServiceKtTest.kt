package service

import com.anjo.service.runSchedulerFlow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds

class SchedulerFlowServiceKtTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `given functions when run scheduler then check properly runs`() = runTest {
        //given
        var calls = 0
        val timeDuration = 5
        val testScope = this

        //when
        val job = launch(testScope.coroutineContext) {
            runSchedulerFlow({ 100.milliseconds }) { calls++;println("Executed $calls times") }
        }

        advanceTimeBy(500)
        //then
        calls shouldBe timeDuration
        job.cancel()
    }
}