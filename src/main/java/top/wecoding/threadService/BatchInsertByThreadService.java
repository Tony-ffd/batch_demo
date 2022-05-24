package top.wecoding.threadService;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import top.wecoding.config.AsyncConfig;
import top.wecoding.mybatis.domain.BatchDemo;
import top.wecoding.mybatis.service.BatchDemoService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 使用多线程持久化的时候对事务进行控制
 * @author ffd
 */
@Service
@RequiredArgsConstructor
public class BatchInsertByThreadService {
    private final BatchDemoService batchDemoService;
    private final PlatformTransactionManager transactionManager;
    private List<TransactionStatus> transactionStatuses = Collections.synchronizedList(new ArrayList<>());

    /**
     * 事务失效，插入199条数据
     */
    @Transactional(rollbackFor = Exception.class)
    @SneakyThrows
    public void insert0(){
        ExecutorService executorService = SpringUtil.getBean(AsyncConfig.ExecutorType.WORK_EXECUTOR);
        long starTime = System.currentTimeMillis();
        for (int i = 0; i < 200; i++) {
            BatchDemo batchDemo = new BatchDemo();
            batchDemo.setId(i);
            batchDemo.setBatchName("name"+i);
            batchDemo.setBatchValue("value"+i);
            executorService.execute(()->{
                if (batchDemo.getId()==100){
                    throw new RuntimeException("dashdkjashdk");
                }
                batchDemoService.save(batchDemo);
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.MINUTES);
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }

    /**
     * 使用事务集合进行控制事务,注意如果事务数组大于数据库连接池的最大连接数就会报错，因为这条事务是基于数据库连接的
     * 可以提前对保存数据分批处理在用多线程持久化
     * todo 暂未成功
     * @link {https://blog.csdn.net/qq273766764/article/details/119972911}
     */
    @Transactional(rollbackFor = Exception.class)
    @SneakyThrows
    public void insert1(){
        //线程数量
        Integer threadCount = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        AtomicBoolean isError = new AtomicBoolean(false);
        boolean awaitTermination = false;
        try {
            for (int i = 0; i < 5; i++) {
                BatchDemo batchDemo = new BatchDemo();
                batchDemo.setId(i);
                batchDemo.setBatchName("name"+i);
                batchDemo.setBatchValue("value"+i);
                executorService.execute(()->{
                    try {
                        //创建一个新的事务状态
                        DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
                        transactionDefinition.setPropagationBehavior(Propagation.REQUIRED.value());
                        TransactionStatus status = transactionManager.getTransaction(transactionDefinition);
                        transactionStatuses.add(status);
//                        if (batchDemo.getId()==2){
//                            throw new RuntimeException("dashdkjashdk");
//                        }
                        batchDemoService.save(batchDemo);
                    } catch (Exception e) {
                        e.printStackTrace();
                        isError.set(true);
                    }
                });
            }
            executorService.shutdown();
            awaitTermination = executorService.awaitTermination(30, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
            isError.set(true);
        }
        if (!awaitTermination){
            isError.set(true);
        }
        if (ObjectUtil.isNotEmpty(transactionStatuses)){
            if (isError.get()){
                transactionStatuses.forEach(status->transactionManager.rollback(status));
            }else {
                transactionStatuses.forEach(status->transactionManager.commit(status));
            }
        }
    }

}
