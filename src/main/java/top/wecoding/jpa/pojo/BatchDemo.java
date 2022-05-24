package top.wecoding.jpa.pojo;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;

/**
 * 批量处理测试表
 * @TableName batch_demo
 */
@Entity(name="batch_demo")
@Data
public class BatchDemo implements Serializable {
    /**
     * id
     */
    @Id
    private Integer id;

    /**
     * 
     */
    @Column(name = "batch_name")
    private String batchName;

    /**
     * 
     */
    @Column(name = "batch_value")
    private String batchValue;

    private static final long serialVersionUID = 1L;
}