package cloud.marisa.picturebackend.util.colors;

import cloud.marisa.picturebackend.exception.BusinessException;
import cloud.marisa.picturebackend.exception.ErrorCode;

import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * @author MarisaDAZE
 * @description 颜色工具类
 * @date 2025/4/5
 */
public class ColorUtils {

    private static volatile Random random = null;

    private ColorUtils() {
        // 工具类不需要实例化
    }

    /**
     * 获取随机颜色
     *
     * @return 随机颜色对象
     */
    public static Color getRandomColor() {
        if (random == null) {
            synchronized (ColorUtils.class) {
                if (random == null) {
                    random = new Random();
                }
            }
        }
        int r = random.nextInt(256);
        int g = random.nextInt(256);
        int b = random.nextInt(256);
        return new Color(r, g, b);
    }

    /**
     * 计算两个颜色的相似度
     *
     * @param color1 第一个颜色
     * @param color2 第二个颜色
     * @return 相似度（0到1之间，1为完全相同）
     */
    public static double calculateSimilarity(Color color1, Color color2) {
        int r1 = color1.getRed();
        int g1 = color1.getGreen();
        int b1 = color1.getBlue();

        int r2 = color2.getRed();
        int g2 = color2.getGreen();
        int b2 = color2.getBlue();
        // 计算欧氏距离
        double distance = Math.sqrt(Math.pow(r1 - r2, 2) + Math.pow(g1 - g2, 2) + Math.pow(b1 - b2, 2));
        // 计算相似度
        return 1 - distance / Math.sqrt(3 * Math.pow(255, 2));
    }

    /**
     * 根据十六进制颜色代码计算相似度
     *
     * @param c1 第一个颜色（可以是16进制也可以是(r,g,b)）
     * @param c2 第二个颜色（可以是16进制也可以是(r,g,b)）
     * @return 相似度（0到1之间，1为完全相同）
     */
    public static double calculateSimilarity(String c1, String c2) {
        Color color1 = strToColor(c1);
        Color color2 = strToColor(c2);
        if (color1 == null || color2 == null) {
            throw new RuntimeException("格式错误，应为16进制色或(r,g,b)色，但实际是: c1=" + c1 + " c2=" + c2);
        }
        return calculateSimilarity(color1, color2);
    }

    /**
     * 尝试从字符串中获取颜色对象
     *
     * @param str 颜色字符串(r,g,b)或者 0xFF0000
     * @return 颜色对象
     */
    public static Color strToColor(String str) {
        try {
            return Color.decode(str);
        } catch (Exception ex) {
            // 可能是rgb格式
            try {
                boolean hasBracket = str.startsWith("(") && str.endsWith(")");
                if (hasBracket) {
                    str = str.substring(1, str.length() - 1);
                }
                // r,g,b
                String[] split = str.split(",");
                Integer[] rgbArr = Arrays.stream(split)
                        .map(Integer::valueOf)
                        .toArray(Integer[]::new);
                return new Color(rgbArr[0], rgbArr[1], rgbArr[2]);
            } catch (Exception e) {
                return null;
            }
        }
    }


    /**
     * 根据颜色字符串获取颜色的hsv值
     *
     * @param colorStr 颜色字符串（可以是16进制或者rgb）
     * @return hsv
     */
    public static MrsColorHSV toHSV(String colorStr) {
        Color color = strToColor(colorStr);
        if (color == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "颜色应为16进制色或(r,g,b)色，但实际是: " + colorStr);
        }
        return toHSV(color);
    }

    public static MrsColorHSV toHSV(Color color) {
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;

        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        float hue = 0;
        if (delta != 0) {
            if (max == r) {
                hue = (g - b) / delta;
            } else if (max == g) {
                hue = (b - r) / delta + 2f;
            } else {
                hue = (r - g) / delta + 4f;
            }
            hue *= 60f;
            if (hue < 0) {
                hue += 360f;
            }
        }

        return getResult(color, max, delta, hue);
    }

    private static MrsColorHSV getResult(Color color, float max, float delta, float hue) {
        hue %= 360; // 确保hue在0-360范围内
        float saturation = (max == 0) ? 0 : (delta / max) * 100f;
        float value = max * 100f;

        // 计算分桶
        int hueBucket = (int) Math.floor(hue / 10);
        int saturationBucket = (int) Math.min(9, Math.floor(saturation / 10f));
        int valueBucket = (int) Math.min(9, Math.floor(value / 10f));

        MrsColorHSV result = new MrsColorHSV();
        result.setColor(color);
        result.setHexColor(toHexString(color));
        result.setHue(Math.round(hue * 100) / 100f);
        result.setSaturation(Math.round(saturation * 100) / 100f);
        result.setValue(Math.round(value * 100) / 100f);
        result.setHueBucket(hueBucket);
        result.setSaturationBucket(saturationBucket);
        result.setValueBucket(valueBucket);

        return result;
    }

    /**
     * 将RGB颜色转换为16进制字符串
     *
     * @param color 颜色对象
     * @return 16进制颜色字符串，格式为 r,g,b
     */
    public static String toRGBString(Color color) {
        if (color == null) {
            return null;
        }
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        // 检查输入范围是否合法
        if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) {
            throw new IllegalArgumentException("RGB values must be in the range [0, 255]");
        }
        return String.format("%d,%d,%d", r, g, b);
    }

    /**
     * 将RGB颜色转换为16进制字符串
     *
     * @param color 颜色对象
     * @return 16进制颜色字符串，格式为 #RRGGBB
     */
    public static String toHexString(Color color) {
        if (color == null) {
            return null;
        }
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        // 检查输入范围是否合法
        if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) {
            throw new IllegalArgumentException("RGB values must be in the range [0, 255]");
        }
        // 使用String.format生成16进制字符串
        return String.format("#%02X%02X%02X", r, g, b);
    }

    public static void main(String[] args) {
        Map<?, ?> map = new HashMap<>();
        Map<?, ?> map1 = new HashMap<>(16);
        Map<?, ?> map2 = new HashMap<>(16, 0.5f);
        // 测试颜色
        // double similarity = calculateSimilarity("(100,123,66)", "0xFFF000");
        // System.out.println("颜色相似度为：" + similarity);

        Color color = ColorUtils.getRandomColor();
        MrsColorHSV hsv = ColorUtils.toHSV(color);
        System.out.println(hsv);
    }
}

