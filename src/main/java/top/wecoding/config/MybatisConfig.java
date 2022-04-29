package top.wecoding.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(value = "top.wecoding.mybatis.mapper")
public class MybatisConfig {
}
