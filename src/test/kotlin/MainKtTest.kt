import com.anjo.getConfigProperties
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class MainKtTest {

    @Test
    fun `given invalid arguments when getConfigProperties then throw exception`() {
        //given
        val args = arrayOf<String>()
        val expectedMessage = "Invalid input config file path"

        //when
        val exception = shouldThrow<IllegalArgumentException> { getConfigProperties(args) }

        //then
        exception.message shouldBe expectedMessage
    }

    @Test
    fun `given arguments with non existing file when getConfigProperties then throw exception`() {
        //given
        val args = arrayOf("--config", "invalid")
        val expectedMessage = "Invalid input config file path"

        //when
        val exception = shouldThrow<IllegalArgumentException> { getConfigProperties(args) }

        //then
        exception.message shouldBe expectedMessage
    }
}