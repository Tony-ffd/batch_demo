package top.wecoding.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * 事务配置
 * @author ffd
 */
@Configuration
public class TransactionConfig {

    /**
     * 排除jpa的事务管理器，使用正常状态的事务管理器
     * @param dataSource 数据源
     * @return DataSourceTransactionManager
     */
//    @Bean("transactionManager")
//    @Primary
//    public PlatformTransactionManager transactionManager(DataSource dataSource) {
//        return new DataSourceTransactionManager(dataSource);
//    }
}
