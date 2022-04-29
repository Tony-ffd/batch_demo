package top.wecoding.mybatis.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * 批量处理测试表
 * @TableName batch_demo
 */
@TableName(value ="batch_demo")
@Data
public class BatchDemo implements Serializable {
    /**
     * id
     */
    @TableId
    private Integer id;

    /**
     * 
     */
    private String batchName;

    /**
     * 
     */
    private String batchValue;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}