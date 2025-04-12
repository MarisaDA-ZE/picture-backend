package cloud.marisa.picturebackend.api.image.imageexpand.entity.response.query;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;
import lombok.ToString;

/**
 * @author MarisaDAZE
 * @description 任务的图像统计信息
 * @date 2025/4/10
 */

@Data
@ToString
public class TaskUsage {

    /**
     * <p>模型生成图片的数量。</p>
     */
    @JSONField(name = "image_count")
    private Integer imageCount;
}
