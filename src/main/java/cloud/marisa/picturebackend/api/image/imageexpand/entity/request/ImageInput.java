package cloud.marisa.picturebackend.api.image.imageexpand.entity.request;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author MarisaDAZE
 * @description 图片地址
 * @date 2025/4/10
 */
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ImageInput {

    /**
     * <p style="color: orange;">必选</p>
     * <p>图像URL地址或者图像base64数据。</p>
     * <p>图像限制：</p>
     * <ul>
     *     <li>图像格式：JPG、JPEG、PNG、HEIF、WEBP。</li>
     *     <li>图像大小：不超过10MB。</li>
     *     <li>图像分辨率：不低于512×512像素且不超过4096×4096像素。</li>
     *     <li>图像单边长度范围：[512, 4096]，单位像素。</li>
     * </ul>
     */
    @JSONField(name = "image_url")
    private String image;
}
