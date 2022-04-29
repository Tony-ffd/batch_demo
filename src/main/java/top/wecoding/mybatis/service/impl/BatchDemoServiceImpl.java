package top.wecoding.mybatis.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import top.wecoding.mybatis.domain.BatchDemo;
import top.wecoding.mybatis.service.BatchDemoService;
import top.wecoding.mybatis.mapper.BatchDemoMapper;
import org.springframework.stereotype.Service;

/**
* @author ffd
* @description 针对表【batch_demo(批量处理测试表)】的数据库操作Service实现
* @createDate 2022-04-29 14:29:46
*/
@Service
public class BatchDemoServiceImpl extends ServiceImpl<BatchDemoMapper, BatchDemo>
    implements BatchDemoService{

}




