package top.wecoding.mybatis;

import cn.hutool.extra.spring.SpringUtil;
import lombok.SneakyThrows;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import top.wecoding.config.AsyncConfig;
import top.wecoding.mybatis.domain.BatchDemo;
import top.wecoding.mybatis.mapper.BatchDemoMapper;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class TestMybatisDemo {
    @Resource
    private SqlSessionFactory sqlSessionFactory;
    @Resource
    private BatchDemoMapper batchDemoMapper;

    /**
     * 正常插入1w条数据
     * 耗时：27097
     */
    @Test
    void testInsert0(){
        BatchDemo batchDemo = new BatchDemo();
        long starTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            batchDemo.setId(i);
            batchDemo.setBatchName("name"+i);
            batchDemo.setBatchValue("value"+i);
            batchDemoMapper.insertForMybatis(batchDemo);
        }
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }

    /**
     * 正常插入1w条数据手动控制事务
     * 耗时：11328
     */
    @Test
    @Transactional
    void testInsert1(){
        BatchDemo batchDemo = new BatchDemo();
        long starTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            batchDemo.setId(i);
            batchDemo.setBatchName("name"+i);
            batchDemo.setBatchValue("value"+i);
            batchDemoMapper.insertForMybatis(batchDemo);
        }
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }

    /**
     * 正常插入1w条临时表优化
     * 耗时：10661
     */
    @Test
    void testInsert2(){
        BatchDemo batchDemo = new BatchDemo();
        long starTime = System.currentTimeMillis();
        batchDemoMapper.createTempTable();
        for (int i = 0; i < 10000; i++) {
            batchDemo.setId(i);
            batchDemo.setBatchName("name"+i);
            batchDemo.setBatchValue("value"+i);
            batchDemoMapper.insertTempTableForMybatis(batchDemo);
        }
        batchDemoMapper.insertByTempTableForMybatis();
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }

    /**
     * 正常插入1w条 insert into ** values(),...,()
     * 耗时：903
     */
    @Test
    void testInsert3(){
        List<BatchDemo> batchDemos = new ArrayList<>(10000);
        long starTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            BatchDemo batchDemo = new BatchDemo();
            batchDemo.setId(i);
            batchDemo.setBatchName("name"+i);
            batchDemo.setBatchValue("value"+i);
            batchDemos.add(batchDemo);
        }
        batchDemoMapper.insertListForMybatis(batchDemos);
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }

    /**
     * 使用批处理特性优化1w条数据
     * 耗时：770
     */
    @Test
    void testInsert4(){
        //使用批处理模式
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
        BatchDemoMapper mapper = sqlSession.getMapper(BatchDemoMapper.class);
        long starTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            BatchDemo batchDemo = new BatchDemo();
            batchDemo.setId(i);
            batchDemo.setBatchName("name"+i);
            batchDemo.setBatchValue("value"+i);
            mapper.insertForMybatis(batchDemo);
        }
        sqlSession.commit();
        sqlSession.close();
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }

    /**
     * 使用多线程优化1w条数据插入
     * 耗时：3503
     */
    @Test
    @SneakyThrows
    void testInsert5(){
        ExecutorService executorService = SpringUtil.getBean(AsyncConfig.ExecutorType.WORK_EXECUTOR);
        long starTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            BatchDemo batchDemo = new BatchDemo();
            batchDemo.setId(i);
            batchDemo.setBatchName("name"+i);
            batchDemo.setBatchValue("value"+i);
            executorService.submit(()->{
                batchDemoMapper.insertForMybatis(batchDemo);
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.MINUTES);
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }

    /**
     * 附：批量更新或修改操作
     * 耗时：944
     */
    @Test
    @SneakyThrows
    void testInsert6(){
        List<BatchDemo> batchDemos = new ArrayList<>(10000);
        long starTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            BatchDemo batchDemo = new BatchDemo();
            batchDemo.setId(i);
            batchDemo.setBatchName("name");
            batchDemo.setBatchValue("value");
            batchDemos.add(batchDemo);
        }
        batchDemoMapper.insertOrUpdateListForMybatis(batchDemos);
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }
}
