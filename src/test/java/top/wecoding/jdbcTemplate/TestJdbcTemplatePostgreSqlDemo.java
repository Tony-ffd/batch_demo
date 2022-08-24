package top.wecoding.jdbcTemplate;

import cn.hutool.extra.spring.SpringUtil;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.object.BatchSqlUpdate;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@SuppressWarnings("all")
public class TestJdbcTemplatePostgreSqlDemo {
    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 测试连通性
     */
    @Test
    @SneakyThrows
    void testConnect() {
        Integer integer = jdbcTemplate.queryForObject("select 1 ", Integer.TYPE);
        System.out.println(integer);
    }

    /**
     * 正常添加1w条数据
     * 耗时：8701
     */
    @Test
    void testInsert0() {
        long starTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            jdbcTemplate.update("insert into batch_demo(id, batch_name, batch_value) values (?,?,?)",
                    new Object[]{i, "name" + i, "value" + i});
        }
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }

    /**
     * 添加1w条数据手动控制事务（编程式）
     * 耗时：3119
     */
    @Test
    void testInsert2() {
        TransactionTemplate template = SpringUtil.getBean(TransactionTemplate.class);
        long starTime = System.currentTimeMillis();
        template.execute(status -> {
            for (int i = 0; i < 10000; i++) {
                jdbcTemplate.update("insert into batch_demo(id, batch_name, batch_value) values (?,?,?)",
                        new Object[]{i, "name" + i, "value" + i});
            }
            return null;
        });
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }

    /**
     * 添加1w条数据临时表优化
     * 耗时：3085
     */
    @Test
    void testInsert3() {
        long starTime = System.currentTimeMillis();
        jdbcTemplate.execute("CREATE TEMPORARY TABLE batch_demo_temp (\n" +
                "   id          integer not null\n" +
                "        primary key,\n" +
                "    batch_name  varchar(255),\n" +
                "    batch_value varchar(255)\n" +
                ")ON COMMIT PRESERVE ROWS;");
        for (int i = 0; i < 10000; i++) {
            jdbcTemplate.update("insert into batch_demo_temp(id, batch_name, batch_value) values (?,?,?)",
                    new Object[]{i, "name" + i, "value" + i});
        }
        jdbcTemplate.execute("insert into batch_demo select * from batch_demo_temp;");
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }

    /**
     * 添加1w条数据批处理api优化 （通用）
     * 耗时：1752
     */
    @Test
    void testInsert5() {
        long starTime = System.currentTimeMillis();
        jdbcTemplate.batchUpdate("insert into batch_demo(id, batch_name, batch_value) values (?,?,?)", new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setInt(1,i);
                ps.setString(2,"name"+i);
                ps.setString(3,"value"+i);
            }

            @Override
            public int getBatchSize() {
                return 10000;
            }
        });
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }

    /**
     * 添加1w条数据批处理BatchSqlUpdate优化 （适合大批量数据处理，分批次处理）
     * 耗时：1413
     */
    @Test
    void testInsert6() {
        long starTime = System.currentTimeMillis();
        BatchSqlUpdate batchSqlUpdate = new BatchSqlUpdate(jdbcTemplate.getDataSource(),"insert into batch_demo(id, batch_name, batch_value) values (?,?,?)");
        int[] types = {Types.INTEGER,Types.VARCHAR,Types.VARCHAR};
        batchSqlUpdate.setTypes(types);
        // 分批处理大小
        batchSqlUpdate.setBatchSize(1000);
        for (int i = 0; i < 10000; i++) {
            batchSqlUpdate.update(new Object[]{i,"name"+i,"value"+i});
        }
        batchSqlUpdate.flush();
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }

    /**
     * 添加1w条数据 insert into ** values(), ... , ()
     * 适用于批量插入
     * 耗时：249
     */
    @Test
    void testInsert7() {
        long starTime = System.currentTimeMillis();
        StringBuilder builder = new StringBuilder("insert into batch_demo(id, batch_name, batch_value) values ");
        for (int i = 0; i < 10000; i++) {
            builder.append(" ("+i+",'name"+i+"','value"+i+"'),");
        }
        String sql = builder.substring(0, builder.length() - 1);
        jdbcTemplate.update(sql);
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }

    /**
     * 添加1w条数据多线程优化
     * 耗时：1585
     */
    @Test
    @SneakyThrows
    void testInsert8() {
        long starTime = System.currentTimeMillis();
        //手动创建线程池
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors()/2, Runtime.getRuntime().availableProcessors(), 1, TimeUnit.MINUTES, new ArrayBlockingQueue<>(10000), (r, exec) -> {
            throw new RejectedExecutionException("线程池已满");
        });
        for (int i = 0; i < 10000; i++) {
            final int index = i;
            poolExecutor.submit(()->{
                jdbcTemplate.update("insert into batch_demo(id, batch_name, batch_value) values (?,?,?)",new Object[]{index,"name"+index,"value"+index});
            });
        }
        poolExecutor.shutdown();
        poolExecutor.awaitTermination(30,TimeUnit.MINUTES);
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }
}
