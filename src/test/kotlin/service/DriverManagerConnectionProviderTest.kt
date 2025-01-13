package service

import com.anjo.service.DriverManagerConnectionProvider
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlin.test.Test

class DriverManagerConnectionProviderTest {

    @Test
    fun `given url when create connection then check is not null`() {
        //given
        val url = ":memory:"
        //when
        val connection = DriverManagerConnectionProvider(url)

        //then
        connection.shouldNotBeNull()
    }
}