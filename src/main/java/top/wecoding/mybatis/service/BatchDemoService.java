package top.wecoding.mybatis.service;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import top.wecoding.mybatis.domain.BatchDemo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author ffd
* @description 针对表【batch_demo(批量处理测试表)】的数据库操作Service
* @createDate 2022-04-29 14:29:46
*/
public interface BatchDemoService extends IService<BatchDemo>  {
    /**
     * 多线程批量存储
     * @param batchDemoList 批量处理的list
     * @param transactionManager 事务管理器
     * @param transactionStatuses 多线程状态列表
     */
    void saveBatchByThread(List<BatchDemo> batchDemoList, PlatformTransactionManager transactionManager, List<TransactionStatus> transactionStatuses);
}
