package service

import com.anjo.service.DbType
import com.anjo.service.DriverManagerConnectionProvider
import com.anjo.service.executeOperation
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
class NetworkDatabaseE2ETest {

    companion object {
        @Container
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")

        @Container
        @JvmStatic
        val mysql: MySQLContainer<*> = MySQLContainer("mysql:8.4")
    }

    @Test
    fun `given real postgres container when connect via DbType POSTGRESQL and select then returns row count`() {
        //given
        seedPeopleTable(postgres.host, postgres.firstMappedPort, postgres.databaseName, postgres.username, postgres.password, "postgresql")
        val provider = DriverManagerConnectionProvider.network(
            DbType.POSTGRESQL, postgres.host, postgres.firstMappedPort, postgres.databaseName, postgres.username, postgres.password
        )

        //when
        val result = provider.getConnection().use { connection ->
            connection.createStatement().use { statement -> executeOperation("select * from people;", statement) }
        }

        //then
        result shouldBe "1"
    }

    @Test
    fun `given real mysql container when connect via DbType MYSQL and select then returns row count`() {
        //given
        seedPeopleTable(mysql.host, mysql.firstMappedPort, mysql.databaseName, mysql.username, mysql.password, "mysql")
        val provider = DriverManagerConnectionProvider.network(
            DbType.MYSQL, mysql.host, mysql.firstMappedPort, mysql.databaseName, mysql.username, mysql.password
        )

        //when
        val result = provider.getConnection().use { connection ->
            connection.createStatement().use { statement -> executeOperation("select * from people;", statement) }
        }

        //then
        result shouldBe "1"
    }

    private fun seedPeopleTable(host: String, port: Int, database: String, user: String, password: String, vendor: String) {
        val url = when (vendor) {
            "postgresql" -> "jdbc:postgresql://$host:$port/$database"
            else         -> "jdbc:mysql://$host:$port/$database"
        }
        java.sql.DriverManager.getConnection(url, user, password).use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("CREATE TABLE people(id INT, name VARCHAR(50))")
                statement.execute("INSERT INTO people VALUES (1, 'Alice')")
            }
        }
    }
}
