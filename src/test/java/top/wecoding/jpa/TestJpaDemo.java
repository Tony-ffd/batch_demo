package top.wecoding.jpa;

import cn.hutool.extra.spring.SpringUtil;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import top.wecoding.config.AsyncConfig;
import top.wecoding.jpa.pojo.BatchDemo;
import top.wecoding.jpa.repository.BatchDemoRepository;
import top.wecoding.jpa.service.BatchDemoServiceByJpa;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class TestJpaDemo {
    @Resource
    private BatchDemoRepository batchDemoRepository;
    @Resource
    private BatchDemoServiceByJpa batchDemoServiceByJpa;
    @Resource
    private EntityManager entityManager;

    /**
     * 正常插入1w条数据 save
     * 耗时：44241
     */
    @Test
    @Transactional
    void testInsert0(){
        BatchDemo batchDemo = new BatchDemo();
        long starTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            batchDemo.setId(i);
            batchDemo.setBatchName("name"+i);
            batchDemo.setBatchValue("value"+i);
            batchDemoRepository.save(batchDemo);
        }
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }

    /**
     * 正常插入1w条数据 saveAll （统一事务提交）
     * 耗时：12363
     */
    @Test
    void testInsert1(){
        long starTime = System.currentTimeMillis();
        List<BatchDemo> batchDemos = new ArrayList<>(10000);
        for (int i = 0; i < 10000; i++) {
            BatchDemo batchDemo = new BatchDemo();
            batchDemo.setId(i);
            batchDemo.setBatchName("name"+i);
            batchDemo.setBatchValue("value"+i);
            batchDemos.add(batchDemo);
        }
        batchDemoRepository.saveAll(batchDemos);
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }

    /**
     * 不查询，强制执行hql插入
     * 耗时：36168
     */
    @Test
    @Transactional(propagation = Propagation.SUPPORTS)
    void testInsert2() {
        long starTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            BatchDemo batchDemo = new BatchDemo();
            batchDemo.setId(i);
            batchDemo.setBatchName("name" + i);
            batchDemo.setBatchValue("value" + i);
            batchDemoRepository.saveBatchDemo(batchDemo.getId(), batchDemo.getBatchName(), batchDemo.getBatchValue());
        }
        batchDemoRepository.flush();
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }

    /**
     * 不查询，强制执行hql插入 (统一事务提交)
     * 耗时: 6549
     */
    @Test
    void testInsert3() {
        batchDemoServiceByJpa.insert3();
    }

    /**
     * 使用entityManager.persist方法（统一事务提交）
     * 耗时：6251
     */
    @Test
    void testInsert4() {
        batchDemoServiceByJpa.insert4();
    }

    /**
     * 多谢线程优化
     * 耗时：4412
     */
    @Test
    @SneakyThrows
    void testInsert5() {
        ExecutorService executorService = SpringUtil.getBean(AsyncConfig.ExecutorType.WORK_EXECUTOR);
        long starTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            final Integer index = i;
            executorService.submit(()->{
                batchDemoRepository.saveBatchDemo(index,"name"+index,"value"+index);
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.MINUTES);
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }

    /**
     * 内存暂存表优化
     * 耗时：6173
     */
    @Test
    void testInsert6() {
        batchDemoServiceByJpa.insert6();
    }

    /**
     * 数据库批处理语句
     * 耗时：386
     */
    @Test
    void testInsert7() {
        batchDemoServiceByJpa.insert7();
    }

    /**
     * 使用批处理模式
     * 耗时：523
     */
    @Test
    void testInsert8() {
        batchDemoServiceByJpa.insert8();
    }

}
