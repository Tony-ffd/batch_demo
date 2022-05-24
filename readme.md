# 批量sql处理优化

> 大批量数据的增删改是较为常见的场景，不当的处理带来一些问题，最直接的问题就是批量持久化数据导致`长事务`，从而引起数据库连接池资源耗尽，接口延时等一系列问题

本文以`mysql`数据库为例，批量插入场景为例子叙述个人在该种场景下的优化见解，只针对于优化方向：事务统一提交、内存暂存表、多线程持久化、数据库批处理语句、框架提供的批处理模式。

> 此处对`数据库批处理语句`大致说明：
>
> 插入语句可使用：`insert into table_name values (...),(...),...,(...)`
>
> 更新语句可使用: 
>
> ~~~mysql
> update table_name,(
> (select  1 as id,'1' as name) union
>  (select  2 as id,'2' as name) union
>  (select  3 as id,'3' as name) union
>  (select  4 as id,'4' as name)
>   /* ....more data ... */
>     ) as table_temp
>    set
> table_name.name=table_temp.name
>  where
> table_name.id=table_temp.id
>  ~~~
> 
>插入或更新可使用:
> 
>~~~mysql
> insert into table_name values 
> (id=1,name=1),(...),...,(...)
> on duplicate key update
> name = values(name),
> ~~~
> 
>删除可使用：`delete from table_name where id in (...)`

## 存储过程

### 正常存储过程插入1w条      时间：14s484ms

~~~mysql
create table batch_demo
(
    id          int         not null comment 'id'
        primary key,
    batch_name  varchar(32) null,
    batch_value varchar(32) null
)
    comment '批量处理测试表';

drop procedure if exists gen_data;

delimiter $$

create procedure gen_data(in size int)
begin
    #分页提交大小
    declare page_size int default 1000;
    #下标
    declare i int default 1;
    out_while:while i < size do
        set i = i + 1;
        insert into batch_demo(id, batch_name, batch_value) value (i,concat('name',i),concat('value',i));
    end while ;
end $$
delimiter ;

call gen_data(10000);
~~~

### 存储过程使用内存表优化添加1w条数据      时间： 98ms

~~~mysql
create table batch_demo
(
    id          int         not null comment 'id'
        primary key,
    batch_name  varchar(32) null,
    batch_value varchar(32) null
)
    comment '批量处理测试表';

create temporary table batch_demo_temp
(
    id          int         not null comment 'id'
        primary key,
    batch_name  varchar(32) null,
    batch_value varchar(32) null
) engine = MEMORY
    comment '批量处理测试表暂存表';

drop procedure if exists gen_data;

delimiter $$

create procedure gen_data(in size int)
begin
    #分页提交大小
    declare page_size int default 1000;
    #下标
    declare i int default 1;
    out_while:while i < size do
        set i = i + 1;
        insert into batch_demo_temp(id, batch_name, batch_value) value (i,concat('name',i),concat('value',i));
    end while ;
    insert into batch_demo select * from batch_demo_temp;
end $$
delimiter ;

call gen_data(10000);
~~~

> 注意：暂存表使用会有大小限制，默认为16M，可以调节my.ini文件或者`transaction_alloc_block_size`全局常量改变限制。但是个人不建议这样操作，在批量存储前可以将数据分片再存。

### 事务控制改为手动，并分片控制       时间：346ms

~~~mysql
create table batch_demo
(
    id          int         not null comment 'id'
        primary key,
    batch_name  varchar(32) null,
    batch_value varchar(32) null
)
    comment '批量处理测试表';
    
drop procedure if exists gen_data;
delimiter $$

create procedure gen_data(in size int)
begin
    #分页提交大小
    declare page_size int default 1000;
    #下标
    declare i int default 1;
    #开启事务
    start transaction ;
    out_while:while i < size do
        set i = i + 1;
        if 0 <> i % page_size then
            insert into batch_demo(id, batch_name, batch_value) value (i,concat('name',i),concat('value',i));
        else
            commit ;
            start transaction ;
        end if ;
    end while ;
    commit ;
end $$
delimiter ;
~~~

## Jdbc

代码位置：https://gitee.com/tonyffd/batch_demo/blob/master/src/test/java/top/wecoding/jdbc/TestJdbcDemo.java

> jdbc有Statement和prepareStatement两种处理sql的方式，个人推荐使用prepareStatement

### 正常循环执行  耗时：20507

~~~java
@Test
@SneakyThrows
void testSimpleInsertData5(){
    PreparedStatement preparedStatement = connection.prepareStatement("insert into batch_demo(id, batch_name, batch_value) value (?,?,?)");
    long starTime = System.currentTimeMillis();
    for (int i = 0; i < 10000; i++) {
        preparedStatement.setInt(1,i);
        preparedStatement.setString(2,"name"+i);
        preparedStatement.setString(3,"value"+i);
        preparedStatement.executeUpdate();
    }
    System.out.println("耗时："+String.valueOf(System.currentTimeMillis()-starTime));
}
~~~

### 事务统一提交  耗时：5621

~~~Java
@Test
@SneakyThrows
void testSimpleInsertData6(){
    connection.setAutoCommit(false);
    PreparedStatement preparedStatement = connection.prepareStatement("insert into batch_demo(id, batch_name, batch_value) value (?,?,?)");
    long starTime = System.currentTimeMillis();
    for (int i = 0; i < 10000; i++) {
        preparedStatement.setInt(1,i);
        preparedStatement.setString(2,"name"+i);
        preparedStatement.setString(3,"value"+i);
        preparedStatement.executeUpdate();
    }
    connection.commit();
    System.out.println("耗时："+String.valueOf(System.currentTimeMillis()-starTime));
}
~~~

### 多线程持久化  耗时：4353

> 注意线程池和数据库连接池的使用限制约束，并需要考虑线程安全问题和事务控制问题

~~~java
@Test
@SneakyThrows
void testSimpleInsertData8() {
    //手动创建线程池
    ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors() / 2, Runtime.getRuntime().availableProcessors(), 1, TimeUnit.MINUTES, new ArrayBlockingQueue<>(10000), (r, exec) -> {
        throw new RejectedExecutionException("线程池已满");
    });
    long starTime = System.currentTimeMillis();
    for (int i = 0; i < 10000; i++) {
        final int index = i;
        poolExecutor.submit(() -> {
            try (Connection connection = dataSource.getConnection();
                 final PreparedStatement preparedStatement = connection.prepareStatement("insert into batch_demo(id, batch_name, batch_value) value (?,?,?)");) {
                preparedStatement.setInt(1, index);
                preparedStatement.setString(2, "name" + index);
                preparedStatement.setString(3, "value" + index);
                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }

        });
    }
    poolExecutor.shutdown();
    poolExecutor.awaitTermination(30, TimeUnit.MINUTES);
    System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
}
~~~

### 内存暂存表优化  耗时：5316

~~~Java
@Test
@SneakyThrows
void testSimpleInsertData2() {
    Statement statement = connection.createStatement();
    long starTime = System.currentTimeMillis();
    statement.executeUpdate("create temporary table batch_demo_temp\n" +
                            "(\n" +
                            "    id          int         not null comment 'id'\n" +
                            "        primary key,\n" +
                            "    batch_name  varchar(32) null,\n" +
                            "    batch_value varchar(32) null\n" +
                            ") engine = MEMORY\n" +
                            "    comment '批量处理测试表暂存表';");
    for (int i = 0; i < 10000; i++) {
        statement.executeUpdate("insert into batch_demo_temp(id, batch_name, batch_value) value (" + i + ",concat('name'," + i + "),concat('value'," + i + "));");
    }
    statement.executeUpdate("insert into batch_demo select * from batch_demo_temp;");
    System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
}
~~~

### 使用批处理模式  耗时：281

> 注意在jdbc连接配置url中添加`rewriteBatchedStatements=true`参数，否则可能不生效

~~~java
@Test
@SneakyThrows
void testSimpleInsertData7() {
    PreparedStatement preparedStatement = connection.prepareStatement("insert into batch_demo(id, batch_name, batch_value) value (?,?,?)");
    long starTime = System.currentTimeMillis();
    for (int i = 0; i < 10000; i++) {
        preparedStatement.setInt(1, i);
        preparedStatement.setString(2, "name" + i);
        preparedStatement.setString(3, "value" + i);
        preparedStatement.addBatch();
    }
    preparedStatement.executeBatch();
    System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
}
~~~

### 数据库批处理语句  耗时：269

~~~java
@Test
@SneakyThrows
void testSimpleInsertData9() {
    Statement statement = connection.createStatement();
    StringBuilder sb = new StringBuilder("insert into batch_demo(id, batch_name, batch_value) values ");
    long starTime = System.currentTimeMillis();
    for (int i = 0; i < 10000; i++) {
        sb.append(" ("+i+",'name"+i+"','value"+i+"') ,");
    }
    String sql = sb.substring(0, sb.length() - 1);
    statement.executeUpdate(sql);
    System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
}
~~~

### 结论

- 可以看出使用框架和数据库提供的批处理方式为最优解
- 个人不建议这种场景直接使用多线程持久化，理由如下：很多情况下我们都需要获取当前登录用户信息作为填充字段，然而很多框架在多线程下无法获取当前登录用户信息，并且多线程是Spring事务失效的经典场景之一
- 即使没有其它选择只能使用多线程进行进行优化，也不要对线程池的线程数量定义太多，定义为当前服务器cpu核数的2倍较好

## Spring Jdbc

代码位置：https://gitee.com/tonyffd/batch_demo/blob/master/src/test/java/top/wecoding/jdbcTemplate/TestJdbcTemplateDemo.java

### 正常循环执行  耗时：20822

~~~Java
@Test
void testInsert0() {
    long starTime = System.currentTimeMillis();
    for (int i = 0; i < 10000; i++) {
        jdbcTemplate.update("insert into batch_demo(id, batch_name, batch_value) value (?,?,?)",
                            new Object[]{i, "name" + i, "value" + i});
    }
    System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
}
~~~

### 事务统一提交  耗时：5774

- @Transactional事务注解

~~~Java
@Test
@Transactional
void testInsert1() {
    long starTime = System.currentTimeMillis();
    for (int i = 0; i < 10000; i++) {
        jdbcTemplate.update("insert into batch_demo(id, batch_name, batch_value) value (?,?,?)",
                            new Object[]{i, "name" + i, "value" + i});
    }
    System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
}
~~~

- 声明式事务

~~~java
@Test
void testInsert2() {
    TransactionTemplate template = SpringUtil.getBean(TransactionTemplate.class);
    long starTime = System.currentTimeMillis();
    template.execute(status -> {
        for (int i = 0; i < 10000; i++) {
            jdbcTemplate.update("insert into batch_demo(id, batch_name, batch_value) value (?,?,?)",
                                new Object[]{i, "name" + i, "value" + i});
        }
        return null;
    });
    System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
}
~~~

### 多线程持久化  耗时：4370

~~~java
@Test
@SneakyThrows
void testInsert8() {
    long starTime = System.currentTimeMillis();
    //手动创建线程池
    ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors()/2, Runtime.getRuntime().availableProcessors(), 1, TimeUnit.MINUTES, new ArrayBlockingQueue<>(10000), (r, exec) -> {
        throw new RejectedExecutionException("线程池已满");
    });
    for (int i = 0; i < 10000; i++) {
        final int index = i;
        poolExecutor.submit(()->{
            jdbcTemplate.update("insert into batch_demo(id, batch_name, batch_value) value (?,?,?)",new Object[]{index,"name"+index,"value"+index});
        });
    }
    poolExecutor.shutdown();
    poolExecutor.awaitTermination(30,TimeUnit.MINUTES);
    System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
}
~~~

### 内存暂存表优化  耗时：5359

~~~java
@Test
void testInsert3() {
    long starTime = System.currentTimeMillis();
    jdbcTemplate.execute("create temporary table batch_demo_temp\n" +
                         "(\n" +
                         "    id          int         not null comment 'id'\n" +
                         "        primary key,\n" +
                         "    batch_name  varchar(32) null,\n" +
                         "    batch_value varchar(32) null\n" +
                         ") engine = MEMORY\n" +
                         "    comment '批量处理测试表暂存表';");
    for (int i = 0; i < 10000; i++) {
        jdbcTemplate.update("insert into batch_demo_temp(id, batch_name, batch_value) value (?,?,?)",
                            new Object[]{i, "name" + i, "value" + i});
    }
    jdbcTemplate.execute("insert into batch_demo select * from batch_demo_temp;");
    System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
}
~~~

### 使用批处理模式  耗时：343

- `jdbcTemplate.batchUpdate` 通用 耗时：343

~~~java
@Test
void testInsert5() {
    long starTime = System.currentTimeMillis();
    jdbcTemplate.batchUpdate("insert into batch_demo(id, batch_name, batch_value) value (?,?,?)", new BatchPreparedStatementSetter() {
        @Override
        public void setValues(PreparedStatement ps, int i) throws SQLException {
            ps.setInt(1,i);
            ps.setString(2,"name"+i);
            ps.setString(3,"value"+i);
        }

        @Override
        public int getBatchSize() {
            return 10000;
        }
    });
    System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
}
~~~

- `BatchSqlUpdate`适合大批量数据处理，分批次处理  耗时：400

~~~java
@Test
void testInsert6() {
    long starTime = System.currentTimeMillis();
    BatchSqlUpdate batchSqlUpdate = new BatchSqlUpdate(jdbcTemplate.getDataSource(),"insert into batch_demo(id, batch_name, batch_value) value (?,?,?)");
    int[] types = {Types.INTEGER,Types.VARCHAR,Types.VARCHAR};
    batchSqlUpdate.setTypes(types);
    // 分批处理大小
    batchSqlUpdate.setBatchSize(1000);
    for (int i = 0; i < 10000; i++) {
        batchSqlUpdate.update(new Object[]{i,"name"+i,"value"+i});
    }
    batchSqlUpdate.flush();
    System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
}
~~~

### 数据库批处理语句  耗时: 281

~~~java
@Test
void testInsert7() {
    long starTime = System.currentTimeMillis();
    StringBuilder builder = new StringBuilder("insert into batch_demo(id, batch_name, batch_value) values ");
    for (int i = 0; i < 10000; i++) {
        builder.append(" ("+i+",'name"+i+"','value"+i+"'),");
    }
    String sql = builder.substring(0, builder.length() - 1);
    jdbcTemplate.update(sql);
    System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
}
~~~

## Mybatis

代码位置: https://gitee.com/tonyffd/batch_demo/blob/master/src/test/java/top/wecoding/mybatis/TestMybatisDemo.java

> 为简化文档，此处只演示调用代码，具体mapper中的sql操作下载演示demo查看

### 正常循环执行  耗时：27097

~~~java
@Test
void testInsert0(){
    BatchDemo batchDemo = new BatchDemo();
    long starTime = System.currentTimeMillis();
    for (int i = 0; i < 10000; i++) {
        batchDemo.setId(i);
        batchDemo.setBatchName("name"+i);
        batchDemo.setBatchValue("value"+i);
        batchDemoMapper.insertForMybatis(batchDemo);
    }
    System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
}
~~~

### 事务统一提交  耗时：11328

~~~java
@Test
@Transactional
void testInsert1(){
    BatchDemo batchDemo = new BatchDemo();
    long starTime = System.currentTimeMillis();
    for (int i = 0; i < 10000; i++) {
        batchDemo.setId(i);
        batchDemo.setBatchName("name"+i);
        batchDemo.setBatchValue("value"+i);
        batchDemoMapper.insertForMybatis(batchDemo);
    }
    System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
}
~~~

### 多线程持久化  耗时：3503

> 此处的线程池是使用SpringBoot项目中配置好的线程池，可自己定义

~~~java
@Test
@SneakyThrows
void testInsert5(){
    ExecutorService executorService = SpringUtil.getBean(AsyncConfig.ExecutorType.WORK_EXECUTOR);
    long starTime = System.currentTimeMillis();
    for (int i = 0; i < 10000; i++) {
        BatchDemo batchDemo = new BatchDemo();
        batchDemo.setId(i);
        batchDemo.setBatchName("name"+i);
        batchDemo.setBatchValue("value"+i);
        executorService.submit(()->{
            batchDemoMapper.insertForMybatis(batchDemo);
        });
    }
    executorService.shutdown();
    executorService.awaitTermination(30, TimeUnit.MINUTES);
    System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
}
~~~

### 内存暂存表优化  耗时：10661

~~~java
@Test
void testInsert2(){
    BatchDemo batchDemo = new BatchDemo();
    long starTime = System.currentTimeMillis();
    //创建暂存表
    batchDemoMapper.createTempTable();
    for (int i = 0; i < 10000; i++) {
        batchDemo.setId(i);
        batchDemo.setBatchName("name"+i);
        batchDemo.setBatchValue("value"+i);
        //给暂存表添加数据
        batchDemoMapper.insertTempTableForMybatis(batchDemo);
    }
    //将暂存表数据导入正式表
    batchDemoMapper.insertByTempTableForMybatis();
    System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
}
~~~

### 使用批处理模式  耗时：770

> 使用`sqlSessionFactory.openSession(ExecutorType.BATCH);`切换sqlSession模式,三种模式分别为
>
> ```java
> SIMPLE, REUSE, BATCH
> ```

~~~java
@Test
void testInsert4(){
    //使用批处理模式
    SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
    BatchDemoMapper mapper = sqlSession.getMapper(BatchDemoMapper.class);
    long starTime = System.currentTimeMillis();
    for (int i = 0; i < 10000; i++) {
        BatchDemo batchDemo = new BatchDemo();
        batchDemo.setId(i);
        batchDemo.setBatchName("name"+i);
        batchDemo.setBatchValue("value"+i);
        mapper.insertForMybatis(batchDemo);
    }
    sqlSession.commit();
    sqlSession.close();
    System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
}
~~~

### 数据库批处理语句  耗时：903

>对应的*Mapper.xml中使用foreach遍历拼接sql语句
>
>~~~xml
><insert id="insertListForMybatis" keyColumn="id" keyProperty="id" parameterType="list" useGeneratedKeys="true">
>    insert into batch_demo values
>    <foreach collection="batchDemoList" index="index" item="batchDemo" separator=",">
>        ( #{batchDemo.id,jdbcType=INTEGER},
>        #{batchDemo.batchName,jdbcType=VARCHAR},
>        #{batchDemo.batchValue,jdbcType=VARCHAR}
>        )
>    </foreach>
></insert>
>~~~
>
>

~~~java
@Test
void testInsert3(){
    List<BatchDemo> batchDemos = new ArrayList<>(10000);
    long starTime = System.currentTimeMillis();
    for (int i = 0; i < 10000; i++) {
        BatchDemo batchDemo = new BatchDemo();
        batchDemo.setId(i);
        batchDemo.setBatchName("name"+i);
        batchDemo.setBatchValue("value"+i);
        batchDemos.add(batchDemo);
    }
    batchDemoMapper.insertListForMybatis(batchDemos);
    System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
}
~~~

## Mybatis Plus

代码位置: https://gitee.com/tonyffd/batch_demo/blob/master/src/test/java/top/wecoding/mybatisPlus/TestMybatisPlusDemo.java

>  作为mybatis的拓展，拥有mybatis的所有功能，直接使用Mybatis的用法完全OK，当然也一些自己的api可以使用，该测试只使用了`ServiceImpl`中的方法，且有些方法于Mybatis一致，省去不写。

### 使用批处理模式  耗时：903

~~~java
@Test
void testInsert3(){
    List<BatchDemo> batchDemos = new ArrayList<>(10000);
    long starTime = System.currentTimeMillis();
    for (int i = 0; i < 10000; i++) {
        BatchDemo batchDemo = new BatchDemo();
        batchDemo.setId(i);
        batchDemo.setBatchName("name"+i);
        batchDemo.setBatchValue("value"+i);
        batchDemos.add(batchDemo);
    }
    batchDemoService.saveBatch(batchDemos);
    System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
}
~~~

> 可以直接使用`SqlHelper`中的批处理api，实现`ServiceImpl`后调用的api较为简单，并且也是使用`SqlHelper`中的api实现的

## Spring Data Jpa

代码位置: https://gitee.com/tonyffd/batch_demo/blob/master/src/test/java/top/wecoding/jpa/TestJpaDemo.java

> Spring Data Jpa中包含`hibernate`，`hibernate`也是jpa的一种实现，故直接测试Jpa，不再测试`hibernate`

### 正常循环执行  耗时：44241

- 使用`JpaRepository`中的`save`方法，较为耗时，因为它会先从数据库查询一遍，判断持久化数据是否为新数据  耗时：44241

~~~java
@Test
@Transactional
void testInsert0(){
    BatchDemo batchDemo = new BatchDemo();
    long starTime = System.currentTimeMillis();
    for (int i = 0; i < 10000; i++) {
        batchDemo.setId(i);
        batchDemo.setBatchName("name"+i);
        batchDemo.setBatchValue("value"+i);
        batchDemoRepository.save(batchDemo);
    }
    System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
}
~~~

- 强制执行insert语句，在Repository中定义一下方法  耗时：耗时：36168

~~~java
@Modifying
@Transactional
@Query(nativeQuery = true,value = "insert into batch_demo(id, batch_name, batch_value) VALUE (:id,:batchName,:batchValue)")
void saveBatchDemo(@Param("id") Integer id, @Param("batchName") String batchName, @Param("batchValue") String batchValue);
~~~

~~~java
@Test
@Transactional(propagation = Propagation.SUPPORTS)
void testInsert2() {
    long starTime = System.currentTimeMillis();
    for (int i = 0; i < 10000; i++) {
        BatchDemo batchDemo = new BatchDemo();
        batchDemo.setId(i);
        batchDemo.setBatchName("name"+i);
        batchDemo.setBatchValue("value"+i);
        batchDemoRepository.saveBatchDemo(batchDemo.getId(),batchDemo.getBatchName(),batchDemo.getBatchValue());
    }
    batchDemoRepository.flush();
    System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
}
~~~

### 事务统一提交  耗时：12363

- 使用`JpaRepository`中的`saveAll`方法  耗时：12363

~~~java
@Test
void testInsert1(){
    long starTime = System.currentTimeMillis();
    List<BatchDemo> batchDemos = new ArrayList<>(10000);
    for (int i = 0; i < 10000; i++) {
        BatchDemo batchDemo = new BatchDemo();
        batchDemo.setId(i);
        batchDemo.setBatchName("name"+i);
        batchDemo.setBatchValue("value"+i);
        batchDemos.add(batchDemo);
    }
    batchDemoRepository.saveAll(batchDemos);
    System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
}
~~~

- 强制性执行`insert`语句  耗时：6549

~~~java
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
~~~

- 使用`entityManager.persist`直接插入  耗时: 6251

~~~java
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
~~~

### 多线程持久化  耗时：4412

~~~java
@Test
@SneakyThrows
void testInsert5() {
    ExecutorService executorService = SpringUtil.getBean(AsyncConfig.ExecutorType.WORK_EXECUTOR);
    long starTime = System.currentTimeMillis();
    for (int i = 0; i < 10000; i++) {
        final Integer index = i;
        executorService.submit(()->{
            batchDemoRepository.saveBatchDemo(index,"name"+index,"value"+index);
        });
    }
    executorService.shutdown();
    executorService.awaitTermination(30, TimeUnit.MINUTES);
    System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
}
~~~

### 内存暂存表优化  耗时：6173

~~~java
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
        entityManager.createNativeQuery("insert into batch_demo_temp(id, batch_name, batch_value) value ("+i+",'name"+i+"','value"+i+"')").executeUpdate();
    }
    entityManager.createNativeQuery("insert into batch_demo select * from batch_demo_temp;").executeUpdate();
    System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
}
~~~

### 使用批处理模式  耗时：523

- 配置hibernate的批处理大小

~~~yaml
spring:
	jpa:
        properties:
              hibernate:
                jdbc:
                  batch_size: 500
                order_inserts: true
                order_updates: true
~~~

- 分批次提交

~~~java
@Transactional(rollbackFor = Exception.class)
public void insert8() {
    long starTime = System.currentTimeMillis();
    for (int i = 0; i < 10000; i++) {
        BatchDemo batchDemo = new BatchDemo();
        batchDemo.setId(i);
        batchDemo.setBatchName("name" + i);
        batchDemo.setBatchValue("value" + i);
        entityManager.persist(batchDemo);
        if (i%500==0){
            entityManager.flush();
            entityManager.clear();
        }
    }
    entityManager.flush();
    System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
}
~~~



### 数据库批处理语句  耗时：386

~~~java
@Transactional(rollbackFor = Exception.class)
public void insert7() {
    long starTime = System.currentTimeMillis();
    StringBuilder sb = new StringBuilder("insert into batch_demo(id, batch_name, batch_value) values ");
    for (int i = 0; i < 10000; i++) {
        sb.append(" ("+i+",'name"+i+"','value"+i+"') ,");
    }
    String sql = sb.substring(0, sb.length() - 1);
    entityManager.createNativeQuery(sql).executeUpdate();
    System.out.println("耗时：" + String.valueOf(System.currentTimeMillis() - starTime));
}
~~~



## 总结

上述的各种持久化框架中，均只使用了单个方向进行优化，在实际业务场景结合使用效果可能会更好。

- 从这几个优化方向来说使用`数据库批处理语句`和持久化框架的`批处理模式`明显是效果最好的

- `内存暂存表`一般只适用于插入场景，并且这种内存暂存表是一次数据库连接状态下存在的，随着数据库连接的中断，暂存表也会没了，所以我们可以使用内存暂存表和`多线程`结合使用，效果应该会更好

- `事务统一提交`和`多线程持久化`是两个较为需要考虑使用场景的点，对于`事务统一提交`来说，一般情况下我们都会使用`Transactional`注解进行管理，或者有的系统中把业务层全部使用切面声明事务，所以一般情况下是统一提交的，但是这也正是长事务的来源之一，`多线程持久化`必然会遇到多线程事务控制，异常处理等问题，下面将展开详细说明

### @Transactional的引发的问题

  这个注解正常情况下会使用在业务层，业务层的逻辑如果在逻辑较为复杂或者处理数据量大的情况下，就会造成单条业务的事务时间很长,可以在这张表中查看`information_schema.INNODB_TRX`当前当前事务有哪些并筛选时间较长的事务。

  > 解决方法: 1、使用声明式事务进行控制，将事务粒度缩小 （推荐）
  >
  > 2、将业务逻辑处理和持久化操作处理进行拆分

  上面的解决方法2中又可能会引发一种问题，如果把业务处理逻辑和持久化处理拆分为一个业务类中的两个方法，这种时候直接用`this.saveXXX（）`待用的话就会引发事务失效，因为添加事务注解后会在生成bean的时候使用动态代理进行代理，this调用的不是动态代理类，所以事务生效不了。

  > 解决方法：
  >
  > 1、添加`manage`层，将持久化操作下沉
  >
  > 2、在本类中注入自己的代理类，有三级缓存，不会造成循环依赖问题
  >
  > ~~~java
  > @service
  > public class AServiceImpl implements AService{
  >     @Resource
  >     private AService aService;
  >     
  >     public void save(){
  >         aService.doSave();
  >     }
  >     ...
  >     public void doSave(){}
  > }
  > ~~~
  >
  > 3、使用`AopContext.currentProxy()`获取当前类的代理类进行调用，注意需要配置`@EnableAspectJAutoProxy(exposeProxy = true)`

### 多线程处理
