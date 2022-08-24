package top.wecoding.mongo;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import top.wecoding.mongo.dao.BatchDemoMongoRepository;
import top.wecoding.mongo.entity.BatchDemo;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@SuppressWarnings("all")
class TestMongoDemo {
    @Resource
    private BatchDemoMongoRepository batchDemoMongoRepository;

    @Resource
    private MongoTemplate mongoTemplate;

    /**
     * 正常添加1w条数据
     * 耗时：4011
     */
    @Test
    void testInsert0() {
        long starTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            BatchDemo batchDemo = new BatchDemo()
                    .setId(i)
                    .setBatchName("name" + i)
                    .setBatchValue("value" + i);
            batchDemoMongoRepository.save(batchDemo);
        }
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }

    /**
     * 批量添加
     * 耗时：409
     */
    @Test
    void testInsert1() {
        long starTime = System.currentTimeMillis();
        List<BatchDemo> batchDemos = new ArrayList<>(10000);
        for (int i = 0; i < 10000; i++) {
            BatchDemo batchDemo = new BatchDemo()
                    .setId(i)
                    .setBatchName("name" + i)
                    .setBatchValue("value" + i);
            batchDemos.add(batchDemo);
        }
        mongoTemplate.insertAll(batchDemos);
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }

    /**
     * 添加1w条数据多线程优化
     * 耗时：1137
     */
    @Test
    @SneakyThrows
    void testInsert8() {
        long starTime = System.currentTimeMillis();
        //手动创建线程池
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors() / 2, Runtime.getRuntime().availableProcessors(), 1, TimeUnit.MINUTES, new ArrayBlockingQueue<>(10000), (r, exec) -> {
            throw new RejectedExecutionException("线程池已满");
        });
        for (int i = 0; i < 10000; i++) {
            final int index = i;
            poolExecutor.submit(() -> {
                BatchDemo batchDemo = new BatchDemo()
                        .setId(index)
                        .setBatchName("name" + index)
                        .setBatchValue("value" + index);
                batchDemoMongoRepository.save(batchDemo);
            });
        }
        poolExecutor.shutdown();
        poolExecutor.awaitTermination(30, TimeUnit.MINUTES);
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }
}
