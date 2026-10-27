import com.anjo.buildConnectionProvider
import com.anjo.config.PropertiesConfig
import com.anjo.getConfigProperties
import com.anjo.resolveQueryTimeoutSeconds
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.io.path.createTempFile
import kotlin.io.path.writeText
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

    @Test
    fun `given network config missing host when buildConnectionProvider then throw exception`() {
        //given
        val props = PropertiesConfig("src/test/resources/invalidNetworkApp.properties")
        val expectedMessage = "Property 'host' required for dbtype=POSTGRESQL"

        //when
        val exception = shouldThrow<IllegalArgumentException> { buildConnectionProvider(props) }

        //then
        exception.message shouldBe expectedMessage
    }

    @Test
    fun `given network config with blank host when buildConnectionProvider then throw exception`() {
        //given
        val file = createTempFile(suffix = ".properties")
        file.writeText("dbtype=postgresql\nhost=\ndatabase=mydb\ndbuser=postgres\n")
        val props = PropertiesConfig(file.toString())
        val expectedMessage = "Property 'host' required for dbtype=POSTGRESQL"

        //when
        val exception = shouldThrow<IllegalArgumentException> { buildConnectionProvider(props) }

        //then
        exception.message shouldBe expectedMessage
    }

    @Test
    fun `given network db without querytimeout when resolveQueryTimeoutSeconds then return default`() {
        //given
        val props = PropertiesConfig("src/test/resources/networkApp.properties")

        //when + then
        resolveQueryTimeoutSeconds(props) shouldBe 3600
    }

    @Test
    fun `given sqlite without querytimeout when resolveQueryTimeoutSeconds then keep driver default`() {
        //given
        val props = PropertiesConfig("src/test/resources/app.properties")

        //when + then
        resolveQueryTimeoutSeconds(props) shouldBe null
    }
}
