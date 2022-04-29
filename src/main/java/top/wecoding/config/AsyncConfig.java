package top.wecoding.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * @author ffd
 * @create 2022-01-20
 * @Description 异步线程池配置
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig implements SchedulingConfigurer {

    public interface ExecutorType {
        String DEFAULT_EXECUTOR = "threadPoolTaskExecutor";
        String WORK_EXECUTOR = "workStealingPool";
        String SCHEDULED_EXECUTOR = "scheduledThreadPool";
    }

    /**
     * 默认使用的cpu密集型线程池，适用于系统中的普通异步处理
     */
    @Lazy
    @Primary
    @Bean(name = "threadPoolTaskExecutor", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(corePoolSize + 5);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("MyAsync-");
        executor.setRejectedExecutionHandler((r, exec) -> {
            throw new RejectedExecutionException("线程池已满");
        });
        executor.initialize();
        return executor;
    }

    /**
     * 默认使用cpu空闲数量来执行任务，适用与大数据量业务优化（如批量数据库处理）
     *
     */
    @Lazy
    @Bean(name = "workStealingPool", destroyMethod = "shutdown")
    public ExecutorService workStealingPool() {
        return Executors.newWorkStealingPool();
    }

    /**
     * 周期性异步线程池，防止异步任务丢失
     */
    @Lazy
    @Bean(name = "scheduledThreadPool", destroyMethod = "shutdown")
    public ExecutorService scheduledThreadPool() {
        return Executors.newScheduledThreadPool(3);
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar scheduledTaskRegistrar) {
        scheduledTaskRegistrar.setScheduler(scheduledThreadPool());
    }

}