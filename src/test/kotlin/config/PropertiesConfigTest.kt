package config

import com.anjo.config.PropertiesConfig
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.io.path.createTempFile
import kotlin.io.path.writeText
import kotlin.test.Test

class PropertiesConfigTest {

    @Test
    fun `given no querytimeout when getQueryTimeoutSeconds then return null`() {
        //given
        val props = PropertiesConfig("src/test/resources/app.properties")

        //when + then
        props.getQueryTimeoutSeconds() shouldBe null
    }

    @Test
    fun `given querytimeout when getQueryTimeoutSeconds then return it`() {
        //given
        val props = propertiesOf("querytimeout=0")

        //when + then
        props.getQueryTimeoutSeconds() shouldBe 0
    }

    @Test
    fun `given invalid querytimeout when getQueryTimeoutSeconds then throw exception`() {
        //given
        val props = propertiesOf("querytimeout=-5")
        val expectedMessage = "Property 'querytimeout' must be a number of seconds >= 0, got: -5"

        //when
        val exception = shouldThrow<IllegalArgumentException> { props.getQueryTimeoutSeconds() }

        //then
        exception.message shouldBe expectedMessage
    }

    private fun propertiesOf(content: String): PropertiesConfig {
        val file = createTempFile(suffix = ".properties")
        file.writeText(content)
        return PropertiesConfig(file.toString())
    }
}
