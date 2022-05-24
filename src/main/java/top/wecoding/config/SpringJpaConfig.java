package top.wecoding.config;

import org.hibernate.SessionFactory;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.hibernate5.HibernateTemplate;

/**
 * jpa配置
 * @author ffd
 */
@SpringBootConfiguration
@EnableJpaRepositories(basePackages = {"top.wecoding.jpa.repository"})
public class SpringJpaConfig {

    @Bean
    @Order(1)
    public HibernateTemplate hibernateTemplate(SessionFactory sessionFactory){
        return new HibernateTemplate(sessionFactory);
    }
}
