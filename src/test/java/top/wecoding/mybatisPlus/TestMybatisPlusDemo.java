package top.wecoding.mybatisPlus;

import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import top.wecoding.mybatis.domain.BatchDemo;
import top.wecoding.mybatis.service.BatchDemoService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class TestMybatisPlusDemo {
    @Resource
    private BatchDemoService batchDemoService;

    /**
     * 普通添加1w条数据
     * 耗时：27220
     */
    @Test
    void testInsert0(){
        long starTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            BatchDemo batchDemo = new BatchDemo();
            batchDemo.setId(i);
            batchDemo.setBatchName("name"+i);
            batchDemo.setBatchValue("value"+i);
            batchDemoService.save(batchDemo);
        }
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }

    /**
     * 正常插入1w条数据手动控制事务
     * 耗时：11418
     */
    @Test
    @Transactional
    void testInsert1(){
        long starTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            BatchDemo batchDemo = new BatchDemo();
            batchDemo.setId(i);
            batchDemo.setBatchName("name"+i);
            batchDemo.setBatchValue("value"+i);
            batchDemoService.save(batchDemo);
        }
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }

    /**
     * 正常插入1w条批处理api
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
        batchDemoService.saveBatch(batchDemos);
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }
}
