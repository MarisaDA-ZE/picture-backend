package cloud.marisa.picturebackend.util;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.compress.utils.IOUtils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author MarisaDAZE
 * @description 图片工具类
 * @date 2025/4/3
 */
public class ImageUtil {
    /**
     * 默认压缩的图片格式
     */
    private static final String DEFAULT_SUFFIX = "webp";

    /**
     * 压缩图片并返回输入流（内存处理，适用于小文件）
     * <p style="color: red;">对大文件可能有内存泄露的风险</p>
     *
     * @param inputStream 输入流
     * @param quality     压缩质量
     * @return 压缩后的输入流
     * @throws IOException .
     */
    public static InputStream compressImage(InputStream inputStream, float quality) throws IOException {
        return compressImage(inputStream, quality, DEFAULT_SUFFIX);
    }

    /**
     * 压缩图片并返回输入流（内存处理，适用于小文件）
     * <p style="color: red;">对大文件可能有内存泄露的风险</p>
     *
     * @param inputStream 输入流
     * @param quality     压缩质量
     * @param suffix      输出文件格式
     * @return 压缩后的输入流
     * @throws IOException .
     */
    public static InputStream compressImage(InputStream inputStream, float quality, String suffix) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Thumbnails.Builder<? extends InputStream> builder = Thumbnails.of(inputStream)
                .scale(1.0)
                .outputQuality(quality);
        // 输出为指定格式
        if (suffix != null) {
            suffix = suffix.startsWith(".") ? suffix.substring(1) : suffix;
            builder.outputFormat(suffix);
        }
        builder.toOutputStream(outputStream);

        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    /**
     * 压缩图片到输出流（通用方法）
     */
    public static void compressImage(InputStream inputStream, OutputStream outputStream, float quality) throws IOException {
        Thumbnails.of(inputStream)
                .scale(1.0)
                .outputQuality(quality)
                .toOutputStream(outputStream);
    }

    /**
     * 处理大文件的压缩（使用临时文件，确保关闭时删除）
     */
    public static InputStream compressLargeImage(InputStream inputStream, float quality) throws IOException {
        Path tempFile = Files.createTempFile("compress", ".tmp");
        try (FileOutputStream fos = new FileOutputStream(tempFile.toFile())) {
            Thumbnails.of(inputStream).scale(1.0).outputQuality(quality).toOutputStream(fos);
        }
        return new FileInputStream(tempFile.toFile()) {
            private boolean closed = false;

            @Override
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    if (!closed) {
                        Files.deleteIfExists(tempFile);
                        closed = true;
                    }
                }
            }
        };
    }

    /**
     * 生成缩略图
     * <p>比例自动计算</p>
     *
     * @param inputStream 图片的输入流
     * @param maxSize     缩略图的最大边长
     * @return 缩略图输入流
     */
    public static InputStream createThumbnail(InputStream inputStream, float maxSize) {
        return createThumbnail(inputStream, maxSize, DEFAULT_SUFFIX);
    }

    /**
     * 生成缩略图
     * <p>比例自动计算</p>
     *
     * @param inputStream 图片的输入流
     * @param maxSize     缩略图的最大边长
     * @param suffix      输出的文件格式
     * @return 缩略图输入流
     */
    public static InputStream createThumbnail(InputStream inputStream, float maxSize, String suffix) {
        try {
            // 100KB以下的图不压缩
            int minThumbSize = 100 * 1024;
            if (inputStream.available() < minThumbSize) {
                return inputStream;
            }
            // 压缩
            byte[] imageData = IOUtils.toByteArray(inputStream);
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            int width = image.getWidth();
            int height = image.getHeight();
            float scale = Math.min((maxSize / width), (maxSize / height));
            if (scale > 1) scale = 1;
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            Thumbnails.Builder<? extends InputStream> builder = Thumbnails.of(new ByteArrayInputStream(imageData))
                    .scale(scale);
            if (suffix != null) {
                suffix = suffix.startsWith(".") ? suffix.substring(1) : suffix;
                builder.outputFormat(suffix);
            }
            builder.toOutputStream(os);
            return new ByteArrayInputStream(os.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取图片的原始格式（如 "jpg", "png", "webp"）
     */
    public static String getImageFormat(InputStream inputStream) throws IOException {
        // 将输入流转换为可重复读取的字节数组
        byte[] imageData = inputStream.readAllBytes();
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageData))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                throw new IOException("Unsupported image format");
            }
            ImageReader reader = readers.next();
            reader.setInput(iis);
            // 返回格式名（如 "jpeg" -> "jpg"）
            return reader.getFormatName().toLowerCase();
        }
    }


    // 缩小图片尺寸加速处理（保持宽高比）
    private static final int TARGET_SIZE = 50;

    public static int[] getDominantColor(InputStream input) throws IOException {
        BufferedImage image = ImageIO.read(input);
        if (image == null) {
            throw new IOException("Unsupported image format or corrupted file");
        }
        // 缩小图片尺寸
        BufferedImage resizedImage = resizeImage(image);
        // 统计颜色出现频率
        Map<Integer, Integer> colorCount = new HashMap<>();
        int width = resizedImage.getWidth();
        int height = resizedImage.getHeight();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int rgb = resizedImage.getRGB(x, y);
                // 忽略透明像素
                if ((rgb >> 24) == 0x00) {
                    continue;
                }
                // 简化颜色精度（按4位精度分组）
                int simplifiedRGB = simplifyColor(rgb);
                colorCount.put(simplifiedRGB, colorCount.getOrDefault(simplifiedRGB, 0) + 1);
            }
        }
        // 找到最高频颜色
        Map.Entry<Integer, Integer> maxEntry = null;
        for (Map.Entry<Integer, Integer> entry : colorCount.entrySet()) {
            if (maxEntry == null || entry.getValue() > maxEntry.getValue()) {
                maxEntry = entry;
            }
        }
        if (maxEntry == null) {
            return new int[]{0, 0, 0}; // 默认黑色
        }

        return unpackRGB(maxEntry.getKey());
    }

    // 简化颜色精度（降低计算复杂度）
    private static int simplifyColor(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        // 按4位精度分组（0-255 -> 0-15）
        r = r >> 4;
        g = g >> 4;
        b = b >> 4;
        return (r << 8) | (g << 4) | b;
    }

    // 解包为RGB数组
    private static int[] unpackRGB(int simplified) {
        int r = (simplified >> 8) & 0xF;
        int g = (simplified >> 4) & 0xF;
        int b = simplified & 0xF;
        // 还原到0-255范围（取中值）
        return new int[]{
                (r << 4) + 8,
                (g << 4) + 8,
                (b << 4) + 8
        };
    }

    // 保持宽高比缩小图片
    private static BufferedImage resizeImage(BufferedImage original) {
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();
        int newWidth = TARGET_SIZE;
        int newHeight = TARGET_SIZE;

        // 计算保持宽高比的尺寸
        if (originalWidth > originalHeight) {
            newHeight = (int) (TARGET_SIZE * ((double) originalHeight / originalWidth));
        } else {
            newWidth = (int) (TARGET_SIZE * ((double) originalWidth / originalHeight));
        }
        Image scaled = original.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(scaled, 0, 0, null);
        g2d.dispose();
        return resized;
    }

    // ==========================================
    public static String getDominantColor2(InputStream imageStream) throws IOException {
        BufferedImage image = ImageIO.read(imageStream);
        BufferedImage resizedImage = Thumbnails.of(image).size(100, 100).asBufferedImage();

        // 改用HSL颜色空间分桶
        Map<Integer, ColorStats> colorStatsMap = new HashMap<>();
        int width = resizedImage.getWidth();
        int height = resizedImage.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = resizedImage.getRGB(x, y);
                float[] hsl = rgbToHsl(rgb);

                // 分桶策略调整：H(色调)细分为36级，S(饱和度)/L(亮度)各8级
                int hBucket = (int) (hsl[0] * 36) % 36;
                int sBucket = (int) (hsl[1] * 8);
                int lBucket = (int) (hsl[2] * 8);
                int key = (hBucket << 16) | (sBucket << 8) | lBucket;

                colorStatsMap.compute(key, (k, v) -> {
                    if (v == null) {
                        return new ColorStats(rgb);
                    } else {
                        v.addColor(rgb);
                        return v;
                    }
                });
            }
        }

        // 筛选高饱和度、中等亮度的颜色（排除过暗/过亮）
        List<ColorStats> candidates = colorStatsMap.values().stream()
                .filter(cs -> cs.avgL() > 0.2 && cs.avgL() < 0.8)
                .filter(cs -> cs.avgS() > 0.4)
                .sorted((a, b) -> Integer.compare(b.count, a.count))
                .collect(Collectors.toList());

//        // 加权混合逻辑（加入亮度权重）
//        // ...
//        // 关键分桶参数调整
//        int hBucket = (int) (hsl[0] * 36) % 36;  // 色调细分为36级（每10度一档）
//        int sBucket = (int) (hsl[1] * 8);       // 饱和度8级
//        int lBucket = (int) (hsl[2] * 8);       // 亮度8级
        return null;
    }

    // RGB转HSL工具方法
    private static float[] rgbToHsl(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        float[] hsl = new float[3];
        Color.RGBtoHSB(r, g, b, hsl);
        return hsl;
    }

    // 统计类调整为存储HSL信息
    @Data
    @ToString
    public static class ColorStats {
        private int count;
        private float sumH, sumS, sumL;
        private int sumR, sumG, sumB;

        public ColorStats(int rgb) {
            addColor(rgb);
        }

        public void addColor(int rgb) {
            float[] hsl = rgbToHsl(rgb);
            sumH += hsl[0];
            sumS += hsl[1];
            sumL += hsl[2];

            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            sumR += r;
            sumG += g;
            sumB += b;
            count++;
        }

        public float avgH() { return sumH / count; }
        public float avgS() { return sumS / count; }
        public float avgL() { return sumL / count; }
    }


    public static void main(String[] args) {
        String path = "C:\\Users\\Marisa\\Desktop\\thems\\bg.png";
//        String path = "C:\\Users\\Marisa\\Desktop\\thems\\126638656_p0.png";
//         String path = "C:\\Users\\Marisa\\Desktop\\thems\\test.webp";
//        File file = new File(path);
//        try (InputStream fis = new FileInputStream(file)) {
//            long current = System.currentTimeMillis();
//            // InputStream thumbnail = createThumbnail(fis, 1000);
//            byte[] bytes = fis.readAllBytes();  // 原图
//            System.out.println("压缩耗时：" + (System.currentTimeMillis() - current));
//            System.out.println("===================");
//            current = System.currentTimeMillis();
//            Color color2 = Color.decode(getDominantColor2(new ByteArrayInputStream(bytes)));
//
//            ByteArrayOutputStream os = new ByteArrayOutputStream();
//            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
//            Thumbnails.of(image).size(100, 100).toOutputStream(os);
//            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(os.toByteArray());
//            File file1 = new File("C:\\Users\\Marisa\\Desktop\\thems\\output.webp");
//            try(FileOutputStream fos = new FileOutputStream(file1)) {
//                fos.write(byteArrayInputStream.readAllBytes());
//                fos.flush();
//            }
//
//            System.out.println("主色调 "+ color2.toString());
//            System.out.println("拾色耗时：" + (System.currentTimeMillis() - current));
//        } catch (IOException ex) {
//            System.out.println(ex.getMessage());
//        }
    }
}