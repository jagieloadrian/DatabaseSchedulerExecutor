package util

import com.anjo.config.PropertiesConfig
import com.anjo.util.validateArgs
import com.anjo.util.validatePath
import com.anjo.util.validateProperties
import com.anjo.util.validateSql
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.Test

class ValidatorKtTest {

    @ParameterizedTest
    @MethodSource("provideArgsAndExpected")
    fun `given args when validateArgs then return expected value`(args: Array<String>, expected: Boolean) {
        //when
        val actual = validateArgs(args)

        //then
        actual shouldBe expected
    }

    @ParameterizedTest
    @MethodSource("providePathAndExpected")
    fun `given path when validatePath then return expected value`(path: String, expected: Boolean) {
        //when
        val actual = validatePath(path)

        //then
        actual shouldBe expected
    }

    @ParameterizedTest
    @MethodSource("provideSqlAndExpected")
    fun `given sql when validateSql then return expected value`(sql: String, expected: Boolean) {
        //when
        val actual = validateSql(sql)

        //then
        actual shouldBe expected
    }

    @Test
    fun `given invalid config properties when validateProperties then throw exception`() {
        //given
        val props = PropertiesConfig("src/test/resources/invalidApp.properties")
        val expectedMessage = "DB path or statement must be properly specified"

        //when
        val exception = shouldThrow<IllegalArgumentException> { validateProperties(props) }

        //then
        exception.message shouldBe expectedMessage
    }

    @Test
    fun `given valid network db config when validateProperties then does not throw`() {
        //given
        val props = PropertiesConfig("src/test/resources/networkApp.properties")

        //when + then
        validateProperties(props)
    }

    @Test
    fun `given network config missing host when validateProperties then throw exception`() {
        //given
        val props = PropertiesConfig("src/test/resources/invalidNetworkApp.properties")
        val expectedMessage = "DB path or statement must be properly specified"

        //when
        val exception = shouldThrow<IllegalArgumentException> { validateProperties(props) }

        //then
        exception.message shouldBe expectedMessage
    }

    companion object {
        @JvmStatic
        fun providePathAndExpected(): List<Arguments> {
            return listOf(
                    Arguments.of("src/test/resources/app.properties", true),
                    Arguments.of("invalidpath", false),
                    Arguments.of("", false),
                    Arguments.of("src/test/resources/invalidfile.txt", false)
            )
        }

        @JvmStatic
        fun provideArgsAndExpected(): List<Arguments> {
            return listOf(
                    Arguments.of(arrayOf("--config", "test"), true),
                    Arguments.of(arrayOf<String>(), false)
            )
        }

        @JvmStatic
        fun provideSqlAndExpected(): List<Arguments> {
            return listOf(
                    Arguments.of("Select * from people;", true),
                    Arguments.of("Select * from people;1=1;", false),
                    Arguments.of("Select;", false),
                    Arguments.of("", false),

                    Arguments.of("SELECT 1; DELETE FROM users", false),
                    Arguments.of("SELECT * FROM people; DROP TABLE people;", false),

                    Arguments.of("SELECT * FROM people WHERE name = 'x' OR 1=1--", false),
                    Arguments.of("SELECT * FROM people WHERE name = 'x' OR '1'='1'", false),

                    Arguments.of("SELECT * FROM people WHERE id = 1 # comment", false),
                    Arguments.of("SELECT * FROM people /* comment */ WHERE id = 1", false),

                    Arguments.of("SELECT * FROM people WHERE status = 'A' OR status = 'B'", true)
            )
        }
    }
}