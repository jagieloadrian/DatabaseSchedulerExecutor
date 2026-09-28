package service

import com.anjo.service.DbType
import com.anjo.service.DriverManagerConnectionProvider
import com.anjo.service.buildJdbcUrl
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class DriverManagerConnectionProviderTest {

    @Test
    fun `given sqlite in-memory path when create connection then check is not null`() {
        //given
        val filePath = ":memory:"
        //when
        val provider = DriverManagerConnectionProvider.sqlite(filePath)
        val connection = provider.getConnection()

        //then
        connection.shouldNotBeNull()
        connection.close()
    }

    @Test
    fun `given dbtype and coordinates when buildJdbcUrl then return correct url`() {
        buildJdbcUrl(DbType.POSTGRESQL, "localhost", 5432, "mydb") shouldBe "jdbc:postgresql://localhost:5432/mydb"
        buildJdbcUrl(DbType.MYSQL, "localhost", 3306, "mydb") shouldBe "jdbc:mysql://localhost:3306/mydb"
        buildJdbcUrl(DbType.MARIADB, "localhost", 3306, "mydb") shouldBe "jdbc:mariadb://localhost:3306/mydb"
        buildJdbcUrl(DbType.MSSQL, "localhost", 1433, "mydb") shouldBe "jdbc:sqlserver://localhost:1433;databaseName=mydb"
        buildJdbcUrl(DbType.ORACLE, "localhost", 1521, "mydb") shouldBe "jdbc:oracle:thin:@localhost:1521:mydb"
    }

    @Test
    fun `given sqlite dbtype when buildJdbcUrl then throw`() {
        shouldThrow<IllegalStateException> { buildJdbcUrl(DbType.SQLITE, "localhost", 0, "mydb") }
    }

    @Test
    fun `given known dbtype string when fromStringOrSqlite then return matching enum`() {
        DbType.fromStringOrSqlite("postgresql") shouldBe DbType.POSTGRESQL
        DbType.fromStringOrSqlite("MYSQL") shouldBe DbType.MYSQL
        DbType.fromStringOrSqlite(null) shouldBe DbType.SQLITE
        DbType.fromStringOrSqlite("") shouldBe DbType.SQLITE
    }

    @Test
    fun `given unknown dbtype string when fromStringOrSqlite then throw`() {
        shouldThrow<IllegalArgumentException> { DbType.fromStringOrSqlite("db2") }
    }
}
