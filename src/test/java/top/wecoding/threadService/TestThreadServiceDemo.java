package top.wecoding.threadService;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import top.wecoding.mybatis.domain.BatchDemo;

import javax.annotation.Resource;
import java.util.ArrayList;

@SpringBootTest
@SuppressWarnings("all")
public class TestThreadServiceDemo {
    @Resource
    private BatchInsertByThreadService batchInsertByThreadService;

    /**
     * 存入199条数据，缺少第100条数据
     */
    @Test
    void insert0(){
        batchInsertByThreadService.insert0();
    }

    @Test
    void insert1(){
        ArrayList<BatchDemo> batchDemos = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            BatchDemo batchDemo = new BatchDemo();
            batchDemo.setId(i);
            batchDemo.setBatchName("name"+i);
            batchDemo.setBatchValue("value"+i);
            batchDemos.add(batchDemo);
        }
        batchInsertByThreadService.insertBy2PC(batchDemos,5);
    }
}
