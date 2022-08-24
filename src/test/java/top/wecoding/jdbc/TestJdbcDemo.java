package top.wecoding.jdbc;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.*;
import java.util.concurrent.*;

@SpringBootTest
@Slf4j
@SuppressWarnings("all")
class TestJdbcDemo {
    @Resource
    private DataSource dataSource;
    private Connection connection;


    @BeforeEach
    @SneakyThrows
    void before() {
        log.debug("初始化！");
        this.connection = dataSource.getConnection();
    }

    @AfterEach
    @SneakyThrows
    void after() {
        log.debug("结束！");
        this.connection.close();
    }

    /**
     * 测试连通性
     */
    @Test
    @SneakyThrows
    void testConnect() {
        PreparedStatement preparedStatement = connection.prepareStatement("select * from batch_demo where id = ?;");
        preparedStatement.setInt(1, 1);
        ResultSet resultSet = preparedStatement.executeQuery();
    }

    /**
     * 普通插入1w条数据
     * 耗时：38609
     */
    @Test
    @SneakyThrows
    void testSimpleInsertData0() {
        Statement statement = connection.createStatement();
        long starTime = System.currentTimeMillis();
        for (int i = 1; i < 10000; i++) {
            statement.executeUpdate("insert into batch_demo(id, batch_name, batch_value) value (" + i + ",concat('name'," + i + "),concat('value'," + i + "));");
        }
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }

    /**
     * 普通插入1w条数据并使用手动控制事务优化
     * 耗时：3243
     */
    @Test
    @SneakyThrows
    void testSimpleInsertData1() {
        connection.setAutoCommit(false);
        Statement statement = connection.createStatement();
        long starTime = System.currentTimeMillis();
        for (int i = 1; i < 10000; i++) {
            statement.executeUpdate("insert into batch_demo(id, batch_name, batch_value) value (" + i + ",concat('name'," + i + "),concat('value'," + i + "));");
        }
        connection.commit();
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }

    /**
     * 普通插入1w条数据并使用暂存表优化
     * 耗时：2726
     */
    @Test
    @SneakyThrows
    void testSimpleInsertData2() {
        Statement statement = connection.createStatement();
        long starTime = System.currentTimeMillis();
        statement.executeUpdate("create temporary table batch_demo_temp\n" +
                "(\n" +
                "    id          int         not null comment 'id'\n" +
                "        primary key,\n" +
                "    batch_name  varchar(32) null,\n" +
                "    batch_value varchar(32) null\n" +
                ") engine = MEMORY\n" +
                "    comment '批量处理测试表暂存表';");
        for (int i = 0; i < 10000; i++) {
            statement.executeUpdate("insert into batch_demo_temp(id, batch_name, batch_value) value (" + i + ",concat('name'," + i + "),concat('value'," + i + "));");
        }
        statement.executeUpdate("insert into batch_demo select * from batch_demo_temp;");
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }

    /**
     * 普通插入1w条数据并使用批处理优化
     * 注意在jdbc连接中添加rewriteBatchedStatements=true
     * 耗时：29108
     */
    @Test
    @SneakyThrows
    void testSimpleInsertData3() {
        Statement statement = connection.createStatement();
        long starTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            statement.addBatch("insert into batch_demo(id, batch_name, batch_value) value (" + i + ",concat('name'," + i + "),concat('value'," + i + "))");
        }
        statement.executeBatch();
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }

    /**
     * 普通插入1w条数据并使用多线程优化
     * (典型的错误写法，因为连接数只有一个)
     * 注意线程池和数据库连接池的使用限制约束，并需要考虑线程安全问题和事务控制问题
     * 正确实例请参考{@link TestJdbcDemo#testSimpleInsertData8()}
     * 耗时：32806
     */
    @Test
    @SneakyThrows
    void testSimpleInsertData4() {
        Statement statement = connection.createStatement();
        //手动创建线程池
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors() / 2, Runtime.getRuntime().availableProcessors(), 1, TimeUnit.MINUTES, new ArrayBlockingQueue<>(10000), (r, exec) -> {
            throw new RejectedExecutionException("线程池已满");
        });
        long starTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            final int index = i;
            poolExecutor.submit(() -> {
                try {
                    statement.executeUpdate("insert into batch_demo(id, batch_name, batch_value) value (" + index + ",concat('name'," + index + "),concat('value'," + index + "));");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
        }
        poolExecutor.shutdown();
        poolExecutor.awaitTermination(30, TimeUnit.MINUTES);
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }

    /**
     * 使用预处理语句优化
     * 耗时：20507
     */
    @Test
    @SneakyThrows
    void testSimpleInsertData5() {
        PreparedStatement preparedStatement = connection.prepareStatement("insert into batch_demo(id, batch_name, batch_value) value (?,?,?)");
        long starTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            preparedStatement.setInt(1, i);
            preparedStatement.setString(2, "name" + i);
            preparedStatement.setString(3, "value" + i);
            preparedStatement.executeUpdate();
        }
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }

    /**
     * 使用预处理语句手动可控制事务优化
     * 耗时：5621
     */
    @Test
    @SneakyThrows
    void testSimpleInsertData6() {
        connection.setAutoCommit(false);
        PreparedStatement preparedStatement = connection.prepareStatement("insert into batch_demo(id, batch_name, batch_value) value (?,?,?)");
        long starTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            preparedStatement.setInt(1, i);
            preparedStatement.setString(2, "name" + i);
            preparedStatement.setString(3, "value" + i);
            preparedStatement.executeUpdate();
        }
        connection.commit();
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }

    /**
     * 使用预处理语句批处理方式优化
     * 耗时：403
     */
    @Test
    @SneakyThrows
    void testSimpleInsertData7() {
        PreparedStatement preparedStatement = connection.prepareStatement("insert into batch_demo(id, batch_name, batch_value) value (?,?,?)");
        long starTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            preparedStatement.setInt(1, i);
            preparedStatement.setString(2, "name" + i);
            preparedStatement.setString(3, "value" + i);
            preparedStatement.addBatch();
        }
        preparedStatement.executeBatch();
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }

    /**
     * 使用预处理语句多线程优化
     * 耗时：5162
     */
    @Test
    @SneakyThrows
    void testSimpleInsertData8() {
        //手动创建线程池
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors() / 2, Runtime.getRuntime().availableProcessors(), 1, TimeUnit.MINUTES, new ArrayBlockingQueue<>(10000), (r, exec) -> {
            throw new RejectedExecutionException("线程池已满");
        });
        long starTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            final int index = i;
            poolExecutor.submit(() -> {
                try (Connection connection = dataSource.getConnection();
                     final PreparedStatement preparedStatement = connection.prepareStatement("insert into batch_demo(id, batch_name, batch_value) value (?,?,?)");) {
                    preparedStatement.setInt(1, index);
                    preparedStatement.setString(2, "name" + index);
                    preparedStatement.setString(3, "value" + index);
                    preparedStatement.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }

            });
        }
        poolExecutor.shutdown();
        poolExecutor.awaitTermination(30, TimeUnit.MINUTES);
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }

    /**
     * 使用数据库批处理语句优化
     * 耗时：461
     */
    @Test
    @SneakyThrows
    void testSimpleInsertData9() {
        Statement statement = connection.createStatement();
        StringBuilder sb = new StringBuilder("insert into batch_demo(id, batch_name, batch_value) values ");
        long starTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            sb.append(" (" + i + ",'name" + i + "','value" + i + "') ,");
        }
        String sql = sb.substring(0, sb.length() - 1);
        statement.executeUpdate(sql);
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }
}
