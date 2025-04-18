package cloud.marisa.picturebackend.api.picgreen;

import com.aliyun.green20220302.models.DescribeImageModerationResultResponseBody;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * @author MarisaDAZE
 * @description AI图片审核结果实体类
 * @date 2025/4/17
 */
@Data
@Builder
@ToString
public class MrsPictureIllegal implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 是否合规
     * <p>true-合规；false-不合规</p>
     */
    private boolean legal;

    /**
     * 风险等级（none-无风险；high-高风险）
     */
    private String riskLevel;

    /**
     * 原因（一般是不合规的原因）
     */
    private String reason;

    /**
     * 原因列表
     */
    private List<String> reasons;

    /**
     * 详细原因列表
     */
    private List<DescribeImageModerationResultResponseBody.DescribeImageModerationResultResponseBodyDataResult> result;

}
