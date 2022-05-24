package top.wecoding.threadService;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
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
        batchInsertByThreadService.insert1();
    }
}
