package cloud.marisa.picturebackend.entity.dto.picture;

import cloud.marisa.picturebackend.api.image.imageexpand.entity.request.ImagePaintingParameters;
import com.alibaba.fastjson2.annotation.JSONField;
import lombok.*;

import java.io.Serializable;

/**
 * @author MarisaDAZE
 * @description AI扩图参数请求类
 * @date 2025/4/10
 */
@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PictureOutPaintingTaskRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 图片ID
     */
    private Long pictureId;

    /**
     * 使用原图进行扩图
     * <p>默认使用压缩后的图进行扩图，这样不会超过图片的大小限制</p>
     */
    private Boolean useOriginal = false;

    /**
     * AI扩图参数
     */
    private ImagePaintingParameters parameters;
}
