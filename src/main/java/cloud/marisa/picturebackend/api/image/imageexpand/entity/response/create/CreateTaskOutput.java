package cloud.marisa.picturebackend.api.image.imageexpand.entity.response.create;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;
import lombok.ToString;

/**
 * @author MarisaDAZE
 * @description 任务输出信息
 * @date 2025/4/10
 */
@Data
@ToString
public class CreateTaskOutput {

    /**
     * <p>任务ID。</p>
     */
    @JSONField(name = "task_id")
    private String taskId;

    /**
     * 当前状态：
     * <ul>
     *     <li>PENDING：任务排队中</li>
     *     <li>RUNNING：任务处理中</li>
     *     <li>SUSPENDED：任务挂起</li>
     *     <li>SUCCEEDED：任务执行成功</li>
     *     <li>FAILED：任务执行失败</li>
     *     <li>UNKNOWN：任务不存在或状态未知</li>
     * </ul>
     */
    @JSONField(name = "task_status")
    private String status;
}
