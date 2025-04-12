package cloud.marisa.picturebackend.api.image.imageexpand.entity.request;

import cn.hutool.core.annotation.Alias;
import com.alibaba.fastjson2.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.io.Serializable;

/**
 * @author MarisaDAZE
 * @description 扩图参数
 * @date 2025/4/10
 */
@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ImagePaintingParameters implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * <div>
     *     <p style="color: orange;">旋转图像：必选</p>
     *     <p style="color: #42A5F6;">等比例扩图：可选</p>
     *     <p style="color: #42A5F6;">指定方向扩图：可选</p>
     *     <p style="color: #42A5F6;">指定宽高比：可选</p>
     * </div>
     * <p>逆时针旋转角度。</p>
     * <p>默认值为0，取值范围[0, 359]</p>
     */
    @JSONField(name = "angle")
    @Alias("angle")
    @JsonProperty("angle")
    private Integer angle = 0;

    /**
     * <div>
     *     <p style="color: #42A5F6;">旋转图像：可选</p>
     *     <p style="color: #42A5F6;">等比例扩图：可选</p>
     *     <p style="color: #42A5F6;">指定方向扩图：可选</p>
     *     <p style="color: #42A5F6;">指定宽高比：可选</p>
     * </div>
     * <p>图像宽高比。</p>
     * <p>可选值有["","1:1", "3:4", "4:3", "9:16", "16:9"]。</p>
     * <p>默认为""，表示不设置输出图像的宽高比。</p>
     */
    @JSONField(name = "output_ratio")
    @Alias("output_ratio")
    @JsonProperty("outputRatio")
    private String outputRatio = "";

    /**
     * <div>
     *     <p style="color: #42A5F6;">旋转图像：可选</p>
     *     <p style="color: #42A5F6;">等比例扩图：可选</p>
     *     <p style="color: #42A5F6;">指定方向扩图：可选</p>
     *     <p style="color: #42A5F6;">指定宽高比：可选</p>
     * </div>
     * <p>图像居中，在水平方向上按比例扩展图像。</p>
     * <p>默认值为1.0，取值范围[1.0, 3.0]。</p>
     */
    @JSONField(name = "x_scale")
    @Alias("x_scale")
    @JsonProperty("xScale")
    private float xScale = 1.0F;

    /**
     * <div>
     *     <p style="color: #42A5F6;">旋转图像：可选</p>
     *     <p style="color: #42A5F6;">等比例扩图：可选</p>
     *     <p style="color: #42A5F6;">指定方向扩图：可选</p>
     *     <p style="color: #42A5F6;">指定宽高比：可选</p>
     * </div>
     * <p>图像居中，在垂直方向上按比例扩展图像。</p>
     * <p>默认值为1.0，取值范围[1.0, 3.0]。</p>
     */
    @JSONField(name = "y_scale")
    @Alias("y_scale")
    @JsonProperty("yScale")
    private float yScale = 1.0F;

    /**
     * <div>
     *     <p style="color: #42A5F6;">旋转图像：可选</p>
     *     <p style="color: #42A5F6;">等比例扩图：可选</p>
     *     <p style="color: #42A5F6;">指定方向扩图：可选</p>
     *     <p style="color: #42A5F6;">指定宽高比：可选</p>
     * </div>
     * <p>在图像上方添加像素。</p>
     * <p>默认值为0，取值限制top_offset＋bottom_offset＜3×输入图像高度。</p>
     */
    @JSONField(name = "top_offset")
    @Alias("top_offset")
    @JsonProperty("topOffset")
    private float topOffset = 0;

    /**
     * <div>
     *     <p style="color: #42A5F6;">旋转图像：可选</p>
     *     <p style="color: #42A5F6;">等比例扩图：可选</p>
     *     <p style="color: #42A5F6;">指定方向扩图：可选</p>
     *     <p style="color: #42A5F6;">指定宽高比：可选</p>
     * </div>
     * <p>在图像下方添加像素。</p>
     * <p>默认值为0，取值限制top_offset＋bottom_offset＜3×输入图像高度。</p>
     */
    @JSONField(name = "bottom_offset")
    @Alias("bottom_offset")
    @JsonProperty("bottomOffset")
    private float bottomOffset = 0;

    /**
     * <div>
     *     <p style="color: #42A5F6;">旋转图像：可选</p>
     *     <p style="color: #42A5F6;">等比例扩图：可选</p>
     *     <p style="color: #42A5F6;">指定方向扩图：可选</p>
     *     <p style="color: #42A5F6;">指定宽高比：可选</p>
     * </div>
     * <p>在图像左侧添加像素。</p>
     * <p>默认值为0，扩展限制left_offset＋right_offset＜3×输入图像宽度。</p>
     */
    @JSONField(name = "left_offset")
    @Alias("left_offset")
    @JsonProperty("leftOffset")
    private float leftOffset = 0;

    /**
     * <div>
     *     <p style="color: #42A5F6;">旋转图像：可选</p>
     *     <p style="color: #42A5F6;">等比例扩图：可选</p>
     *     <p style="color: #42A5F6;">指定方向扩图：可选</p>
     *     <p style="color: #42A5F6;">指定宽高比：可选</p>
     * </div>
     * <p>在图像右侧添加像素。</p>
     * <p>默认值为0，扩展限制left_offset＋right_offset＜3×输入图像宽度。</p>
     */
    @JSONField(name = "right_offset")
    @Alias("right_offset")
    @JsonProperty("rightOffset")
    private float rightOffset = 0;

    /**
     * <div>
     *     <p style="color: #42A5F6;">旋转图像：可选</p>
     *     <p style="color: #42A5F6;">等比例扩图：可选</p>
     *     <p style="color: #42A5F6;">指定方向扩图：可选</p>
     *     <p style="color: #42A5F6;">指定宽高比：可选</p>
     * </div>
     * <p>开启图像最佳质量模式。</p>
     * <p>默认值为false，减少图像生成的等待时间。</p>
     */
    @JSONField(name = "best_quality")
    @Alias("best_quality")
    @JsonProperty("bestQuality")
    private boolean bestQuality = false;

    /**
     * <div>
     *     <p style="color: #42A5F6;">旋转图像：可选</p>
     *     <p style="color: #42A5F6;">等比例扩图：可选</p>
     *     <p style="color: #42A5F6;">指定方向扩图：可选</p>
     *     <p style="color: #42A5F6;">指定宽高比：可选</p>
     * </div>
     * <p>限制模型生成的图像文件大小。</p>
     * <p>默认值为true，限制为：</p>
     * <ul>
     *     <li>当输入图像单边长度 ≤ 10000时，输出图像文件大小在5MB以下。</li>
     *     <li>当输入图像单边长度＞10000时，输出图像文件大小在10MB以下。</li>
     * </ul>
     * <p>输出图像的长宽比范围为1:4至4:1。</p>
     * <p>建议设置为true，模型生成的图像需要经过一层绿网安全过滤后才能输出。当前绿网不支持大于10M的图像处理。</p>
     */
    @JSONField(name = "limit_image_size")
    @Alias("limit_image_size")
    @JsonProperty("limitImageSize")
    private boolean limitImageSize = true;

    /**
     * <div>
     *     <p style="color: #42A5F6;">旋转图像：可选</p>
     *     <p style="color: #42A5F6;">等比例扩图：可选</p>
     *     <p style="color: #42A5F6;">指定方向扩图：可选</p>
     *     <p style="color: #42A5F6;">指定宽高比：可选</p>
     * </div>
     * <p>添加Generated by AI水印。</p>
     * <p>默认值为true，在输出图像左下角处添加水印。</p>
     */
    @JSONField(name = "add_watermark")
    @Alias("add_watermark")
    @JsonProperty("addWatermark")
    private boolean addWatermark = true;
}
