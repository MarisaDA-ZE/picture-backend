package cloud.marisa.picturebackend.api.image.imageexpand.entity.request;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.*;

import java.io.Serializable;

/**
 * @author MarisaDAZE
 * @description 图像处理请求参数
 * @date 2025/4/10
 */
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ImageRequestParam implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * <p style="color: orange;">必选</p>
     * <p>模型名称。示例值：image-out-painting。</p>
     */
    private String model = "image-out-painting";

    /**
     * <p style="color: orange;">必选</p>
     * <p>输入图像的基本信息，比如图像URL地址。</p>
     */
    private ImageInput input;

    /**
     * <p style="color: orange;">必选</p>
     * <p>图像处理参数。</p>
     */
    private ImagePaintingParameters parameters;

    public ImageRequestParam(String url, ImagePaintingParameters parameters) {
        this.input = new ImageInput(url);
        this.parameters = parameters;
    }

    public ImageRequestParam(ImageInput input, ImagePaintingParameters parameters) {
        this.input = input;
        this.parameters = parameters;
    }

    public ImageRequestParam(String model, String url, ImagePaintingParameters parameters) {
        this.model = model;
        this.input = new ImageInput(url);
        this.parameters = parameters;
    }
}
