package cloud.marisa.picturebackend.api.image.imageexpand.entity.response.query;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;
import lombok.ToString;

/**
 * @author MarisaDAZE
 * @description AI扩图任务（查询方法）响应结果
 * @date 2025/4/10
 */
@Data
@ToString
public class TaskQueryResponse {

    /**
     * <p>请求唯一标识。可用于请求明细溯源和问题排查。</p>
     */
    @JSONField(name = "request_id")
    private String requestId;

    /**
     * <p>任务输出信息。</p>
     */
    @JSONField(name = "output")
    private TaskQueryOutput output;

    /**
     * <p>图像统计信息。</p>
     */
    @JSONField(name = "usage")
    private TaskUsage usage;
}
