package top.wecoding.mongo.dao;

import org.springframework.data.mongodb.repository.MongoRepository;
import top.wecoding.mongo.entity.BatchDemo;

public interface BatchDemoMongoRepository extends MongoRepository<BatchDemo,Integer> {
}
