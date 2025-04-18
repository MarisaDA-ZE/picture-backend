package cloud.marisa.picturebackend.config.aliyun.green;

import com.aliyun.green20220302.models.DescribeImageModerationResultResponseBody;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

/**
 * @author MarisaDAZE
 * @description MrsImageModeration.类
 * @date 2025/4/17
 */
@Data
@Builder
@ToString
public class MrsImageModeration {
    /**
     * 返回状态码
     */
    private Integer code;

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 提示信息
     */
    private String msg;

    /**
     * 是否成功状态
     */
    private boolean isSuccess;

    /**
     * 是否是等待状态
     */
    private boolean isProcessing;

    /**
     * 数据
     */
    private DescribeImageModerationResultResponseBody.DescribeImageModerationResultResponseBodyData data;
}
