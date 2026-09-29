package service

import com.anjo.service.DbType
import com.anjo.service.DriverManagerConnectionProvider
import com.anjo.service.executeOperation
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.testcontainers.containers.MSSQLServerContainer
import org.testcontainers.containers.MariaDBContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.oracle.OracleContainer

@Testcontainers
class NetworkDatabaseE2ETest {

    companion object {
        @Container
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")

        @Container
        @JvmStatic
        val mysql: MySQLContainer<*> = MySQLContainer("mysql:8.4")

        @Container
        @JvmStatic
        val mariadb: MariaDBContainer<*> = MariaDBContainer("mariadb:11")

        @Container
        @JvmStatic
        val mssql: MSSQLServerContainer<*> = MSSQLServerContainer("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense()

        @Container
        @JvmStatic
        val oracle: OracleContainer = OracleContainer("gvenzl/oracle-free:23-slim-faststart")
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

    @Test
    fun `given real mariadb container when connect via DbType MARIADB and select then returns row count`() {
        //given
        seedPeopleTable(mariadb.host, mariadb.firstMappedPort, mariadb.databaseName, mariadb.username, mariadb.password, "mariadb")
        val provider = DriverManagerConnectionProvider.network(
            DbType.MARIADB, mariadb.host, mariadb.firstMappedPort, mariadb.databaseName, mariadb.username, mariadb.password
        )

        //when
        val result = provider.getConnection().use { connection ->
            connection.createStatement().use { statement -> executeOperation("select * from people;", statement) }
        }

        //then
        result shouldBe "1"
    }

    @Test
    fun `given real mssql container when connect via DbType MSSQL and select then returns row count`() {
        //given
        // MSSQLServerContainer.getDatabaseName() is unsupported by testcontainers - "master" is the
        // default DB the container exposes, no custom database creation needed for this test.
        val port = mssql.getMappedPort(1433)
        val database = "master"
        seedPeopleTable(mssql.host, port, database, mssql.username, mssql.password, "mssql")
        val provider = DriverManagerConnectionProvider.network(
            DbType.MSSQL, mssql.host, port, database, mssql.username, mssql.password
        )

        //when
        val result = provider.getConnection().use { connection ->
            connection.createStatement().use { statement -> executeOperation("select * from people;", statement) }
        }

        //then
        result shouldBe "1"
    }

    @Test
    fun `given real oracle container when connect via DbType ORACLE and select then returns row count`() {
        //given
        val port = oracle.getMappedPort(1521)
        seedPeopleTable(oracle.host, port, oracle.databaseName, oracle.username, oracle.password, "oracle")
        val provider = DriverManagerConnectionProvider.network(
            DbType.ORACLE, oracle.host, port, oracle.databaseName, oracle.username, oracle.password
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
            "mariadb"    -> "jdbc:mariadb://$host:$port/$database"
            "mssql"      -> "jdbc:sqlserver://$host:$port;databaseName=$database;trustServerCertificate=true"
            "oracle"     -> "jdbc:oracle:thin:@//$host:$port/$database"
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
