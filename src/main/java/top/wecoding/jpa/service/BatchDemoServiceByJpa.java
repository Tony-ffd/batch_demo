package top.wecoding.jpa.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.wecoding.jpa.pojo.BatchDemo;
import top.wecoding.jpa.repository.BatchDemoRepository;

import javax.persistence.EntityManager;
import javax.persistence.Query;

/**
 * jpa的service实现
 *
 * @author ffd
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("all")
public class BatchDemoServiceByJpa {
    private final BatchDemoRepository batchDemoRepository;
    private final EntityManager entityManager;


    /**
     * 不查询，强制执行hql插入 (统一事务提交)
     */
    @Transactional(rollbackFor = Exception.class)
    public void insert3() {
        long starTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            BatchDemo batchDemo = new BatchDemo();
            batchDemo.setId(i);
            batchDemo.setBatchName("name" + i);
            batchDemo.setBatchValue("value" + i);
            batchDemoRepository.saveBatchDemo(batchDemo.getId(), batchDemo.getBatchName(), batchDemo.getBatchValue());
        }
        batchDemoRepository.flush();
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }

    /**
     * 使用entityManager.persist方法（统一事务提交）
     */
    @Transactional(rollbackFor = Exception.class)
    public void insert4() {
        long starTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            BatchDemo batchDemo = new BatchDemo();
            batchDemo.setId(i);
            batchDemo.setBatchName("name" + i);
            batchDemo.setBatchValue("value" + i);
            entityManager.persist(batchDemo);
        }
        entityManager.flush();
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }

    /**
     * 使用内存暂存表
     */
    @Transactional(rollbackFor = Exception.class)
    public void insert6() {
        long starTime = System.currentTimeMillis();
        Query createTable = entityManager.createNativeQuery("create temporary table batch_demo_temp\n" +
                "(\n" +
                "    id          int         not null comment 'id'\n" +
                "        primary key,\n" +
                "    batch_name  varchar(32) null,\n" +
                "    batch_value varchar(32) null\n" +
                ") engine = MEMORY\n" +
                "    comment '批量处理测试表暂存表';");
        createTable.executeUpdate();
        for (int i = 0; i < 10000; i++) {
            entityManager.createNativeQuery("insert into batch_demo_temp(id, batch_name, batch_value) value (" + i + ",'name" + i + "','value" + i + "')").executeUpdate();
        }
        entityManager.createNativeQuery("insert into batch_demo select * from batch_demo_temp;").executeUpdate();
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }

    @Transactional(rollbackFor = Exception.class)
    public void insert7() {
        long starTime = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder("insert into batch_demo(id, batch_name, batch_value) values ");
        for (int i = 0; i < 10000; i++) {
            sb.append(" (" + i + ",'name" + i + "','value" + i + "') ,");
        }
        String sql = sb.substring(0, sb.length() - 1);
        entityManager.createNativeQuery(sql).executeUpdate();
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }

    @Transactional(rollbackFor = Exception.class)
    public void insert8() {
        long starTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            BatchDemo batchDemo = new BatchDemo();
            batchDemo.setId(i);
            batchDemo.setBatchName("name" + i);
            batchDemo.setBatchValue("value" + i);
            entityManager.persist(batchDemo);
            if (i % 500 == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        entityManager.flush();
        System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
    }

}
