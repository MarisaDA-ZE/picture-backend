package cloud.marisa.picturebackend.util.colors;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.awt.*;

/**
 * @author MarisaDAZE
 * @description HSV颜色参数
 * @date 2025/4/5
 */
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MrsColorHSV {

    /**
     * 原始的RGB颜色
     */
    private Color color;

    /**
     * 十六进制颜色（0x00FFFF）
     */
    private String hexColor;

    /**
     * 色调
     */
    private float hue;

    /**
     * 饱和度
     */
    private float saturation;

    /**
     * 明度
     */
    private float value;

    /**
     * 色调分桶（0~360°）
     */
    private int hueBucket;

    /**
     * 饱和度分桶（0~100%）
     */
    private int saturationBucket;

    /**
     * 明度分桶（0~100%）
     */
    private int valueBucket;
}
