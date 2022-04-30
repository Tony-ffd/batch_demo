package top.wecoding.mybatis.mapper;

import org.apache.ibatis.annotations.Param;
import top.wecoding.mybatis.domain.BatchDemo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
* @author ffd
* @description 针对表【batch_demo(批量处理测试表)】的数据库操作Mapper
* @createDate 2022-04-29 14:29:46
* @Entity top.wecoding.mybatis.domain.BatchDemo
*/
public interface BatchDemoMapper extends BaseMapper<BatchDemo> {
    /**
     * 创建临时表
     */
    void createTempTable();

    /**
     * 给临时表中添加数据
     * @param batchDemo
     */
    void insertTempTableForMybatis(BatchDemo batchDemo);

    /**
     * 将临时表中的数据转入正式表
     */
    void insertByTempTableForMybatis();

    /**
     * mybatis正常插入数据
     * @param batchDemo
     * @return int
     */
    int insertForMybatis(BatchDemo batchDemo);

    /**
     * mybatis插入数据 insert into ** values(),...,()
     * @param batchDemoList
     * @return int[]
     */
    void insertListForMybatis(@Param("batchDemoList") List<BatchDemo> batchDemoList);

    /**
     * mybatis插入数据 insert into ** values(),...,()
     * @param batchDemoList
     * @return int[]
     */
    void insertOrUpdateListForMybatis(@Param("batchDemoList") List<BatchDemo> batchDemoList);

}




