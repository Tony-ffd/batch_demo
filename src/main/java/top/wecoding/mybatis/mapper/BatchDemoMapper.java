package top.wecoding.mybatis.mapper;

import top.wecoding.mybatis.domain.BatchDemo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
* @author ffd
* @description 针对表【batch_demo(批量处理测试表)】的数据库操作Mapper
* @createDate 2022-04-29 14:29:46
* @Entity top.wecoding.mybatis.domain.BatchDemo
*/
public interface BatchDemoMapper extends BaseMapper<BatchDemo> {
    int deleteByPrimaryKey(Long id);

    int insert(BatchDemo record);

    int insertSelective(BatchDemo record);

    BatchDemo selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(BatchDemo record);

    int updateByPrimaryKey(BatchDemo record);
}




