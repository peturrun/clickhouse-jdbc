package ru.yandex.clickhouse.integration;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import ru.yandex.clickhouse.ClickHouseArray;
import ru.yandex.clickhouse.ClickHouseDataSource;
import ru.yandex.clickhouse.settings.ClickHouseProperties;

import java.math.BigInteger;
import java.sql.*;

public class ClickHousePreparedStatementTest {
    private ClickHouseDataSource dataSource;
    private Connection connection;

    @BeforeTest
    public void setUp() throws Exception {
        ClickHouseProperties properties = new ClickHouseProperties();
        dataSource = new ClickHouseDataSource("jdbc:clickhouse://localhost:8123", properties);
        connection = dataSource.getConnection();
        connection.createStatement().execute("CREATE DATABASE IF NOT EXISTS test");
    }

    @AfterTest
    public void tearDown() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    public void testArrayTest() throws Exception {

        connection.createStatement().execute("DROP TABLE IF EXISTS test.array_test");
        connection.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS test.array_test (i Int32, a Array(Int32)) ENGINE = TinyLog"
        );

        PreparedStatement statement = connection.prepareStatement("INSERT INTO test.array_test (i, a) VALUES (?, ?)");

        statement.setInt(1, 1);
        statement.setArray(2, new ClickHouseArray(Types.INTEGER, new int[]{1, 2, 3}));
        statement.addBatch();

        statement.setInt(1, 2);
        statement.setArray(2, new ClickHouseArray(Types.INTEGER, new int[]{2, 3, 4, 5}));
        statement.addBatch();
        statement.executeBatch();

        ResultSet rs = connection.createStatement().executeQuery("SELECT count() as cnt from test.array_test");
        rs.next();

        Assert.assertEquals(rs.getInt("cnt"), 2);
        Assert.assertFalse(rs.next());
    }

    @Test
    public void testInsertUInt() throws SQLException {
        connection.createStatement().execute("DROP TABLE IF EXISTS test.unsigned_insert");
        connection.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS test.unsigned_insert (ui32 UInt32, ui64 UInt64) ENGINE = TinyLog"
        );
        PreparedStatement stmt = connection.prepareStatement("insert into test.unsigned_insert (ui32, ui64) values (?, ?)");
        stmt.setObject(1, 4294967286L);
        stmt.setObject(2, new BigInteger("18446744073709551606"));
        stmt.execute();
        Statement select = connection.createStatement();
        ResultSet rs = select.executeQuery("select ui32, ui64 from test.unsigned_insert");
        rs.next();
        Object bigUInt32 = rs.getObject(1);
        Assert.assertTrue(bigUInt32 instanceof Long);
        Assert.assertEquals(((Long)bigUInt32).longValue(), 4294967286L);
        Object bigUInt64 = rs.getObject(2);
        Assert.assertTrue(bigUInt64 instanceof BigInteger);
        Assert.assertEquals(bigUInt64, new BigInteger("18446744073709551606"));
    }
}
