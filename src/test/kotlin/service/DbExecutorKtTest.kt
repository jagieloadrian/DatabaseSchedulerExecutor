package service

import com.anjo.service.DatabaseConnectionProvider
import com.anjo.service.executeOperation
import com.anjo.service.runSqlStatement
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import kotlin.test.Test

class DbExecutorKtTest {

    @Test
    fun `given properly path and sql statement when run dbExecutor then check if increase result check`() = runTest {
        //given
        val sql = "select * from users;"

        val mockConnectionProvider = mockk<DatabaseConnectionProvider>()
        val mockConnection = mockk<Connection>(relaxed = true)
        val mockStatement = mockk<Statement>(relaxed = true)

        every { mockConnectionProvider.getConnection() } returns mockConnection
        every { mockConnection.createStatement() } returns mockStatement

        //when
        runSqlStatement(sql, mockConnectionProvider, 30)

        //then
        verify { mockConnectionProvider.getConnection() }
        verify { mockConnection.createStatement() }
        verify { mockStatement.queryTimeout = 30 }
        verify { mockStatement.execute(sql) }
        verify { mockStatement.close() }
    }

    @Test
    fun `given connection fails once when run dbExecutor then retries and succeeds`() = runTest {
        //given
        val sql = "select * from users;"

        val mockConnectionProvider = mockk<DatabaseConnectionProvider>()
        val mockConnection = mockk<Connection>(relaxed = true)
        val mockStatement = mockk<Statement>(relaxed = true)

        every { mockConnectionProvider.getConnection() } throws SQLException("connect failed") andThen mockConnection
        every { mockConnection.createStatement() } returns mockStatement

        //when
        runSqlStatement(sql, mockConnectionProvider, 30)

        //then
        verify(exactly = 2) { mockConnectionProvider.getConnection() }
        verify { mockStatement.execute(sql) }
    }

    @Test
    fun `given connection always fails when run dbExecutor then gives up without throwing`() = runTest {
        //given
        val sql = "select * from users;"

        val mockConnectionProvider = mockk<DatabaseConnectionProvider>()
        every { mockConnectionProvider.getConnection() } throws SQLException("connect failed")

        //when/then - must not propagate, scheduler loop has to survive
        runSqlStatement(sql, mockConnectionProvider, 30)

        verify(exactly = 3) { mockConnectionProvider.getConnection() }
    }

    @Test
    fun `given select when executeOperation then return number of rows read`() {
        //given
        val sql = "select * from users;"
        val mockStatement = mockk<Statement>(relaxed = true)
        val mockResultSet = mockk<ResultSet>(relaxed = true)

        every { mockStatement.execute(sql) } returns true
        every { mockStatement.resultSet } returns mockResultSet
        every { mockResultSet.next() } returnsMany listOf(true, true, false)

        //when
        val rows = executeOperation(sql, mockStatement)

        //then
        rows shouldBe 2
        verify { mockResultSet.close() }
    }

    @Test
    fun `given update when executeOperation then return number of rows affected`() {
        //given
        val sql = "update users set name = 'x';"
        val mockStatement = mockk<Statement>(relaxed = true)

        every { mockStatement.execute(sql) } returns false
        every { mockStatement.updateCount } returns 3

        //when
        val rows = executeOperation(sql, mockStatement)

        //then
        rows shouldBe 3
    }

    @Test
    fun `given no query timeout when run dbExecutor then driver default is kept`() = runTest {
        //given
        val sql = "select * from users;"

        val mockConnectionProvider = mockk<DatabaseConnectionProvider>()
        val mockConnection = mockk<Connection>(relaxed = true)
        val mockStatement = mockk<Statement>(relaxed = true)

        every { mockConnectionProvider.getConnection() } returns mockConnection
        every { mockConnection.createStatement() } returns mockStatement

        //when
        runSqlStatement(sql, mockConnectionProvider, null)

        //then
        verify(exactly = 0) { mockStatement.queryTimeout = any() }
        verify { mockStatement.execute(sql) }
    }

    @Test
    fun `given sql error when run dbExecutor then logs and does not throw`() = runTest {
        //given
        val sql = "select * from missing_table;"

        val mockConnectionProvider = mockk<DatabaseConnectionProvider>()
        val mockConnection = mockk<Connection>(relaxed = true)
        val mockStatement = mockk<Statement>(relaxed = true)

        every { mockConnectionProvider.getConnection() } returns mockConnection
        every { mockConnection.createStatement() } returns mockStatement
        every { mockStatement.execute(sql) } throws SQLException("no such table")

        //when/then - must not propagate, scheduler loop has to survive
        runSqlStatement(sql, mockConnectionProvider, 30)

        verify { mockStatement.close() }
        verify { mockConnection.close() }
    }

    @Test
    fun `given run dbExecutor then jdbc runs on IO dispatcher thread`() = runTest {
        //given
        val sql = "select * from users;"
        var jdbcThread = ""

        val mockConnectionProvider = mockk<DatabaseConnectionProvider>()
        val mockConnection = mockk<Connection>(relaxed = true)

        every { mockConnectionProvider.getConnection() } answers {
            jdbcThread = Thread.currentThread().name
            mockConnection
        }

        //when
        runSqlStatement(sql, mockConnectionProvider, 30)

        //then - not the caller's thread (runTest's "Test worker")
        jdbcThread shouldStartWith "DefaultDispatcher-worker"
    }

    @Test
    fun `given cancel during connection retry when run dbExecutor then cancellation propagates`() {
        //given
        val sql = "select * from users;"

        val mockConnectionProvider = mockk<DatabaseConnectionProvider>()
        every { mockConnectionProvider.getConnection() } throws SQLException("connect failed")
        var continuedAfterCancel = false

        //when - cancelled during the 1s retry backoff
        shouldThrow<TimeoutCancellationException> {
            runBlocking {
                withTimeout(200) {
                    runSqlStatement(sql, mockConnectionProvider, 30)
                    continuedAfterCancel = true
                }
            }
        }

        //then - not swallowed as a failed run: the caller stops right there
        continuedAfterCancel shouldBe false
        verify(exactly = 1) { mockConnectionProvider.getConnection() }
    }
}
