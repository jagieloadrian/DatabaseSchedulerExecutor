package service

import com.anjo.service.DatabaseConnectionProvider
import com.anjo.service.runSqlStatement
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.sql.Connection
import java.sql.SQLException
import java.sql.Statement
import kotlin.test.Test

class DbExecutorKtTest {

    @Test
    fun `given properly path and sql statement when run dbExecutor then check if increase result check`() {
        //given
        val sql = "select * from users;"

        val mockConnectionProvider = mockk<DatabaseConnectionProvider>()
        val mockConnection = mockk<Connection>(relaxed = true)
        val mockStatement = mockk<Statement>(relaxed = true)

        every { mockConnectionProvider.getConnection() } returns mockConnection
        every { mockConnection.createStatement() } returns mockStatement

        //when
        runSqlStatement(sql, mockConnectionProvider)

        //then
        verify { mockConnectionProvider.getConnection() }
        verify { mockConnection.createStatement() }
        verify { mockStatement.executeQuery(sql) }
        verify { mockStatement.close() }
    }

    @Test
    fun `given connection fails once when run dbExecutor then retries and succeeds`() {
        //given
        val sql = "select * from users;"

        val mockConnectionProvider = mockk<DatabaseConnectionProvider>()
        val mockConnection = mockk<Connection>(relaxed = true)
        val mockStatement = mockk<Statement>(relaxed = true)

        every { mockConnectionProvider.getConnection() } throws SQLException("connect failed") andThen mockConnection
        every { mockConnection.createStatement() } returns mockStatement

        //when
        runSqlStatement(sql, mockConnectionProvider)

        //then
        verify(exactly = 2) { mockConnectionProvider.getConnection() }
        verify { mockStatement.executeQuery(sql) }
    }

    @Test
    fun `given connection always fails when run dbExecutor then gives up without throwing`() {
        //given
        val sql = "select * from users;"

        val mockConnectionProvider = mockk<DatabaseConnectionProvider>()
        every { mockConnectionProvider.getConnection() } throws SQLException("connect failed")

        //when/then - must not propagate, scheduler loop has to survive
        runSqlStatement(sql, mockConnectionProvider)

        verify(exactly = 3) { mockConnectionProvider.getConnection() }
    }
}