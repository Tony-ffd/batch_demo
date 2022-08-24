package top.wecoding.mybatis.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import top.wecoding.mybatis.domain.BatchDemo;
import top.wecoding.mybatis.mapper.BatchDemoMapper;
import top.wecoding.mybatis.service.BatchDemoService;

import java.util.List;

/**
 * @author ffd
 * @description 针对表【batch_demo(批量处理测试表)】的数据库操作Service实现
 * @createDate 2022-04-29 14:29:46
 */
@Service
public class BatchDemoServiceImpl extends ServiceImpl<BatchDemoMapper, BatchDemo>
        implements BatchDemoService {

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = {Exception.class})
    @Override
    public void saveBatchByThread(List<BatchDemo> batchDemoList, PlatformTransactionManager transactionManager, List<TransactionStatus> transactionStatuses) {
//        if (batchDemoList.get(0).getId().equals(0)) {
//            throw new RuntimeException("手动抛错");
//        }
        //创建一个新的事务状态
        DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
        transactionDefinition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        TransactionStatus status = transactionManager.getTransaction(transactionDefinition);
        transactionStatuses.add(status);
        saveBatch(batchDemoList);
    }
}




