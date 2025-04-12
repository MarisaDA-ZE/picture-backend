package cloud.marisa.picturebackend.api.image.imageexpand.entity.response.query;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;
import lombok.ToString;

/**
 * @author MarisaDAZE
 * @description 任务查询结果统计信息
 * @date 2025/4/10
 */
@Data
@ToString
public class TaskQueryMetrics {

    /**
     * 总的任务数。
     */
    @JSONField(name = "TOTAL")
    private Integer total;

    /**
     * 任务状态为成功的任务数。
     */
    @JSONField(name = "SUCCEEDED")
    private Integer succeeded;

    /**
     * 任务状态为失败的任务数。
     */
    @JSONField(name = "FAILED")
    private Integer failed;


}
