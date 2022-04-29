package top.wecoding.mybatis;

import org.springframework.boot.test.context.SpringBootTest;
import top.wecoding.mybatis.domain.BatchDemo;
import top.wecoding.mybatis.service.BatchDemoService;

import javax.annotation.Resource;

@SpringBootTest
public class TestMybatisDemo {
    @Resource
    private BatchDemoService batchDemoService;


}
