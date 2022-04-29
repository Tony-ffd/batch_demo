package top.wecoding.jpa.entity;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;

/**
 * 批量处理测试表
 * @TableName batch_demo
 */
@Table(name="batch_demo")
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