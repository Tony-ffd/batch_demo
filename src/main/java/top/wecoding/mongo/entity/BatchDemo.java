package top.wecoding.mongo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Data
public class BatchDemo implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    private Integer id;

    private String batchName;

    private String batchValue;
}
