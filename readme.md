# 批量sql处理优化

## mysql

### 存储过程

- 正常存储过程插入1w条      时间：14s484ms

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

- 存储过程使用内存表优化添加1w条数据      时间： 98ms

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
  
  - 事务控制改为手动，并分片控制       时间：346ms
  
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

  ### jdbc
    
    > 注意：使用批处理api需要在连接url中配置`rewriteBatchedStatements=true`

### jdbcTemplate
    

