package top.wecoding.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.wecoding.jpa.pojo.BatchDemo;

/**
* @author ffd
* @description 针对表【batch_demo(批量处理测试表)】的数据库操作Mapper
* @createDate 2022-04-29 14:32:39
* @Entity top.wecoding.jpa.entity.BatchDemo
*/
@Repository
public interface BatchDemoRepository extends JpaRepository<BatchDemo,Integer> {

    @Modifying
    @Transactional
    @Query(nativeQuery = true,value = "insert into batch_demo(id, batch_name, batch_value) VALUE (:id,:batchName,:batchValue)")
    void saveBatchDemo(@Param("id") Integer id, @Param("batchName") String batchName, @Param("batchValue") String batchValue);


}
