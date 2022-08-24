package top.wecoding.mongo.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * MongoDB 通用服务接口
 *
 * @author ffd
 */
public interface BaseMongoService<T> {

    /**
     * 插入一条记录 MongoDB
     *
     * @param entity 实体对象
     */
    T save(T entity);

    /**
     * 插入（批量）
     *
     * @param entityList 实体对象集合
     */
    Collection<T> saveBatch(Collection<T> entityList);

    /**
     * 根据 ID 删除
     *
     * @param id 主键ID
     */
    void removeById(Serializable id);

    /**
     * 根据实体(ID)删除
     *
     * @param entity 实体
     */
    void removeById(T entity);

    /**
     * 根据 Query 条件，删除记录
     *
     * @param query 查询包装类 {@link org.springframework.data.mongodb.core.query.Query}
     */
    void remove(Query query);

    /**
     * 根据 entity 属性，删除记录
     *
     * @param entity 实体
     */
    void remove(T entity);

    /**
     * 根据 ID 选择修改
     *
     * @param id     id
     * @param entity 实体对象
     */
    void updateById(Serializable id, T entity);

    /**
     * 根据 updateQuery 条件，更新记录
     *
     * @param updateQuery 查询包装类 {@link org.springframework.data.mongodb.core.query.Query}
     * @param entity      实体对象
     */
    void update(Query updateQuery, T entity);

    /**
     * 通过条件查询更新数据
     *
     * @param updateQuery 查询包装类 {@link org.springframework.data.mongodb.core.query.Query}
     * @param update      更新包装类 {@link org.springframework.data.mongodb.core.query.Update}
     */
    void update(Query updateQuery, Update update);

    /**
     * 根据 ID 查询
     *
     * @param id 主键ID
     */
    T getById(Serializable id);

    /**
     * 查询（根据ID 批量查询）
     *
     * @param idList 主键ID列表
     */
    List<T> listByIds(Collection<? extends Serializable> idList);

    /**
     * 根据 Query 条件，查询一个实体
     *
     * @param query 查询包装类 {@link org.springframework.data.mongodb.core.query.Query}
     */
    T getOne(Query query);

    /**
     * 查询总记录数
     *
     * @see Wrappers#emptyWrapper()
     */
    long count();

    /**
     * 根据 Query 条件，查询总记录数
     *
     * @param query 查询包装类 {@link org.springframework.data.mongodb.core.query.Query}
     */
    long count(Query query);

    /**
     * 查询列表
     *
     * @param query 查询包装类 {@link org.springframework.data.mongodb.core.query.Query}
     */
    List<T> list(Query query);

    /**
     * 查询所有
     */
    List<T> list();

}