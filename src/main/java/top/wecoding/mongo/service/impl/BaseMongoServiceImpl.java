package top.wecoding.mongo.service.impl;

import cn.hutool.core.util.TypeUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import top.wecoding.mongo.service.BaseMongoService;

import javax.annotation.Resource;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.List;

/**
 * Mongo 基类服务实现
 *
 * @author ffd
 */
@SuppressWarnings("all")
public class BaseMongoServiceImpl<T> implements BaseMongoService<T> {

    @Resource
    @Qualifier("mongoTemplate")
    protected MongoTemplate mongoTemplate;

    @Override
    public T save(T entity) {
        mongoTemplate.save(entity);
        return entity;
    }

    @Override
    public Collection<T> saveBatch(Collection<T> entityList) {
        return mongoTemplate.insertAll(entityList);
    }

    @Override
    public void removeById(Serializable id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("id").is(id));
        mongoTemplate.remove(query);
    }

    @Override
    public void removeById(T entity) {
        mongoTemplate.remove(entity);
    }

    @Override
    public void remove(Query query) {
        mongoTemplate.remove(query, TypeUtil.getClass(getClass()));
    }

    @Override
    public void remove(T entity) {
        mongoTemplate.remove(entity);
    }

    @Override
    public void updateById(Serializable id, T entity) {
        Query query = new Query();
        query.addCriteria(Criteria.where("id").is(id));
        Update update = buildBaseUpdate(entity);
        update(query, update);
    }

    @Override
    public void update(Query updateQuery, T entity) {
        Update update = buildBaseUpdate(entity);
        update(updateQuery, update);
    }

    @Override
    public void update(Query updateQuery, Update update) {
        mongoTemplate.updateMulti(updateQuery, update, TypeUtil.getClass(getClass()));
    }

    @Override
    public T getById(Serializable id) {
        return mongoTemplate.findById(id, this.getEntityClass());
    }

    @Override
    public List<T> listByIds(Collection<? extends Serializable> idList) {
        Query query = new Query();
        query.addCriteria(Criteria.where("id").in(idList));
        return mongoTemplate.find(query, this.getEntityClass());
    }

    @Override
    public T getOne(Query query) {
        return mongoTemplate.findOne(query, this.getEntityClass());
    }

    @Override
    public long count() {
        return mongoTemplate.count(new Query(), this.getEntityClass());
    }

    @Override
    public long count(Query query) {
        return mongoTemplate.count(query, this.getEntityClass());
    }

    @Override
    public List<T> list(Query query) {
        return mongoTemplate.find(query, this.getEntityClass());
    }

    @Override
    public List<T> list() {
        return mongoTemplate.findAll(this.getEntityClass());
    }



    private Update buildBaseUpdate(T t) {
        Update update = new Update();

        Field[] fields = t.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(t);
                if (value != null) {
                    update.set(field.getName(), value);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return update;
    }


    private Class<T> getEntityClass() {
        return ((Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0]);
    }

}