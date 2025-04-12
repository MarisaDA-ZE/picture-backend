package cloud.marisa.picturebackend.util.colors;

/**
 * @author MarisaDAZE
 * @description DominantColorExtractor.类
 * @date 2025/4/12
 */

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import net.coobird.thumbnailator.Thumbnails;

public class DominantColorExtractor {

    private static final int TARGET_SIZE = 150;
    private static final int H_BUCKETS = 36;  // 色调分桶数（每10度一个）
    private static final int S_BUCKETS = 8;   // 饱和度分桶数
    private static final int L_BUCKETS = 8;   // 亮度分桶数
    private static final float MIN_SATURATION = 0.4f;
    private static final float MIN_LIGHTNESS = 0.2f;
    private static final float MAX_LIGHTNESS = 0.8f;

    public static Color extractDominantColor(InputStream imageStream) throws IOException {
        // 1. 读取并压缩图片
        BufferedImage image = ImageIO.read(imageStream);
        BufferedImage resizedImage = Thumbnails.of(image)
                .size(TARGET_SIZE, TARGET_SIZE)
                .asBufferedImage();

        // 2. 创建颜色分桶统计
        Map<Integer, ColorBucket> colorBuckets = new HashMap<>();
        int width = resizedImage.getWidth();
        int height = resizedImage.getHeight();

        // 3. 遍历所有像素进行分桶统计
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                processPixel(resizedImage.getRGB(x, y), colorBuckets);
            }
        }

        // 4. 过滤并排序候选颜色
        List<ColorBucket> candidates = colorBuckets.values().stream()
                .filter(DominantColorExtractor::isValidColor)
                .sorted(Comparator.comparingInt((ColorBucket b) -> b.count).reversed())
                .collect(Collectors.toList());

        // 5. 处理无候选颜色情况
        if (candidates.isEmpty()) {
            return getFallbackColor(colorBuckets);
        }

        // 6. 加权混合主要颜色
        return calculateDominantColor(candidates);
    }

    private static void processPixel(int rgb, Map<Integer, ColorBucket> buckets) {
        float[] hsl = rgbToHsl(rgb);

        // 计算分桶索引
        int hIdx = (int) (hsl[0] * H_BUCKETS) % H_BUCKETS;
        int sIdx = (int) (hsl[1] * S_BUCKETS);
        int lIdx = (int) (hsl[2] * L_BUCKETS);
        int bucketKey = (hIdx << 16) | (sIdx << 8) | lIdx;

        // 更新分桶统计
        buckets.compute(bucketKey, (k, v) -> {
            if (v == null) {
                return new ColorBucket(rgb, hsl);
            } else {
                v.addColor(rgb, hsl);
                return v;
            }
        });
    }

    private static boolean isValidColor(ColorBucket bucket) {
        return bucket.avgS >= MIN_SATURATION &&
                bucket.avgL >= MIN_LIGHTNESS &&
                bucket.avgL <= MAX_LIGHTNESS;
    }

    private static Color getFallbackColor(Map<Integer, ColorBucket> buckets) {
        return buckets.values().stream()
                .max(Comparator.comparingInt(b -> b.count))
                .map(b -> new Color((int) b.avgR, (int) b.avgG, (int) b.avgB)
                )
                .orElse(Color.BLACK);
    }

    private static Color calculateDominantColor(List<ColorBucket> candidates) {
        float totalWeight = 0f;
        float weightedH = 0f, weightedS = 0f, weightedL = 0f;

        // 取前5个候选颜色（避免颜色突变）
        List<ColorBucket> topCandidates = candidates.subList(0, Math.min(5, candidates.size()));

        for (ColorBucket bucket : topCandidates) {
            // 计算权重因子：出现次数 * 饱和度 * 亮度适宜度
            float brightnessBias = 1 - Math.abs(bucket.avgL - 0.5f);
            float weight = bucket.count * bucket.avgS * brightnessBias;

            weightedH += bucket.avgH * weight;
            weightedS += bucket.avgS * weight;
            weightedL += bucket.avgL * weight;
            totalWeight += weight;
        }

        // 计算加权平均值
        float finalH = weightedH / totalWeight;
        float finalS = Math.min(1f, weightedS / totalWeight);
        float finalL = Math.min(1f, weightedL / totalWeight);

        // 转换回RGB并应用伽马校正
        int rgb = Color.HSBtoRGB(finalH, finalS, finalL);
        int r = gammaCorrect((rgb >> 16) & 0xFF);
        int g = gammaCorrect((rgb >> 8) & 0xFF);
        int b = gammaCorrect(rgb & 0xFF);

        return new Color(r, g, b);
    }

    private static float[] rgbToHsl(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        float[] hsl = new float[3];
        Color.RGBtoHSB(r, g, b, hsl);
        return hsl;
    }

    private static int gammaCorrect(int channel) {
        float normalized = channel / 255f;
        float corrected = normalized <= 0.04045f ?
                normalized / 12.92f :
                (float) Math.pow((normalized + 0.055f) / 1.055f, 2.4);
        return Math.round(corrected * 255);
    }

    private static String rgbToHex(Color color) {
        return rgbToHex(color.getRed(), color.getGreen(), color.getBlue());
    }

    private static String rgbToHex(int r, int g, int b) {
        return String.format("#%02x%02x%02x", r, g, b);
    }

    private static class ColorBucket {
        int count = 0;
        int sumR = 0;
        int sumG = 0;
        int sumB = 0;
        float sumH = 0f;
        float sumS = 0f;
        float sumL = 0f;
        float avgR, avgG, avgB;
        float avgH, avgS, avgL;

        public ColorBucket(int rgb, float[] hsl) {
            addColor(rgb, hsl);
        }

        public void addColor(int rgb, float[] hsl) {
            sumR += (rgb >> 16) & 0xFF;
            sumG += (rgb >> 8) & 0xFF;
            sumB += rgb & 0xFF;
            sumH += hsl[0];
            sumS += hsl[1];
            sumL += hsl[2];
            count++;

            avgR = sumR / (float) count;
            avgG = sumG / (float) count;
            avgB = sumB / (float) count;
            avgH = sumH / count;
            avgS = sumS / count;
            avgL = sumL / count;
        }
    }

    public static void main(String[] args) {
        String path = "C:\\Users\\Marisa\\Desktop\\thems\\bg.png";
//        String path = "C:\\Users\\Marisa\\Desktop\\thems\\126638656_p0.png";
//         String path = "C:\\Users\\Marisa\\Desktop\\thems\\test.webp";
        File file = new File(path);
        try (InputStream fis = new FileInputStream(file)) {
            long current = System.currentTimeMillis();
            byte[] bytes = fis.readAllBytes();  // 原图
            Color color = extractDominantColor(new ByteArrayInputStream(bytes));
            String hexColor = rgbToHex(color);
            System.out.println("主色调: " + color + ",hex: " + hexColor);
            System.out.println("拾色耗时：" + (System.currentTimeMillis() - current));
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
}