package cloud.marisa.picturebackend.util;

import cloud.marisa.picturebackend.entity.dto.file.UploadPictureResult;
import cloud.marisa.picturebackend.exception.BusinessException;
import cloud.marisa.picturebackend.exception.ErrorCode;
import cloud.marisa.picturebackend.upload.picture.CountingInputStream;
import cn.hutool.core.util.NumberUtil;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.tasks.UnsupportedFormatException;
import org.apache.commons.compress.utils.IOUtils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.awt.*;
import java.nio.charset.StandardCharsets;
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
@Log4j2
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
        try {
            builder.toOutputStream(outputStream);
        } catch (Exception e) {
            log.error("压缩图片时出错", e);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "解析图片时出错");
        }
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
     * 获取图片信息
     *
     * @param is 输入流
     */
    public static String getPictureType(InputStream is) {
        // 获取图片的长宽比等信息
        try (CountingInputStream cis = new CountingInputStream(is)) {
            // 读取前 8 个字节用于检测文件类型
            cis.mark(8);
            byte[] header = new byte[8];
            int bytesRead = cis.read(header);
            cis.reset(); // 重置流供后续读取
            // 从字节数组中获取图片类型
            return MrsStreamUtil.determineFileType(header, bytesRead);
        } catch (IOException e) {
            log.error("文件读取失败: ", e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "文件读取失败");
        }
    }

    // -------------获取文件格式------------------
    public static String getImageTypePlus(InputStream is) {
        try (is) {
            byte[] header = new byte[16];
            int bytesRead = is.read(header);
            if (bytesRead < 2) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件头过短");
            }
            return determineImageType(header, bytesRead);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "文件读取失败");
        }
    }

    private static String determineImageType(byte[] header, int bytesRead) {
        // 按检测速度排序：高频格式在前
        if (isJPEG(header)) return "jpeg";
        if (isPNG(header, bytesRead)) return "png";
        if (isGIF(header, bytesRead)) return "gif";
        if (isBMP(header)) return "bmp";
        if (isWebP(header, bytesRead)) return "webp";
        if (isTIFF(header)) return "tiff";
        if (isICO(header)) return "ico";
        if (isPSD(header)) return "psd";
        if (isHEIC(header, bytesRead)) return "heic";
        if (isAVIF(header, bytesRead)) return "avif";

        throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的图片格式");
    }

    // 以下是各格式的检测方法（16进制值用字节直接比较）
    private static boolean isJPEG(byte[] header) {
        return (header[0] & 0xFF) == 0xFF &&
                (header[1] & 0xFF) == 0xD8;
    }

    private static boolean isPNG(byte[] header, int bytesRead) {
        return bytesRead >= 8 &&
                header[0] == (byte) 0x89 &&
                header[1] == 0x50 &&  // P
                header[2] == 0x4E &&  // N
                header[3] == 0x47 &&  // G
                header[4] == 0x0D &&
                header[5] == 0x0A &&
                header[6] == 0x1A &&
                header[7] == 0x0A;
    }

    private static boolean isGIF(byte[] header, int bytesRead) {
        return bytesRead >= 6 &&
                header[0] == 0x47 &&  // G
                header[1] == 0x49 &&  // I
                header[2] == 0x46 &&  // F
                header[3] == 0x38 &&  // 8
                (header[4] == 0x37 || header[4] == 0x39) && // 7/9
                header[5] == 0x61;    // a
    }

    private static boolean isBMP(byte[] header) {
        return header[0] == 0x42 &&  // B
                header[1] == 0x4D;    // M
    }

    private static boolean isWebP(byte[] header, int bytesRead) {
        return bytesRead >= 12 &&
                header[0] == 0x52 &&  // R
                header[1] == 0x49 &&  // I
                header[2] == 0x46 &&  // F
                header[3] == 0x46 &&  // F
                header[8] == 0x57 &&  // W
                header[9] == 0x45 &&  // E
                header[10] == 0x42 && // B
                header[11] == 0x50;  // P
    }

    private static boolean isTIFF(byte[] header) {
        return (header[0] == 0x49 && header[1] == 0x49 &&
                header[2] == 0x2A && header[3] == 0x00) ||  // II*
                (header[0] == 0x4D && header[1] == 0x4D &&
                        header[2] == 0x00 && header[3] == 0x2A);    // MM*
    }

    private static boolean isICO(byte[] header) {
        return header[0] == 0x00 &&
                header[1] == 0x00 &&
                header[2] == 0x01 &&
                header[3] == 0x00;
    }

    private static boolean isPSD(byte[] header) {
        return header[0] == 0x38 &&  // 8
                header[1] == 0x42 &&  // B
                header[2] == 0x50 &&  // P
                header[3] == 0x53;    // S
    }

    private static boolean isHEIC(byte[] header, int bytesRead) {
        return bytesRead >= 12 &&
                header[4] == 0x66 &&  // f
                header[5] == 0x74 &&  // t
                header[6] == 0x79 &&  // y
                header[7] == 0x70 &&  // p
                new String(header, 8, 4, StandardCharsets.US_ASCII)
                        .matches("heic|heix|hevc|hevx");
    }

    private static boolean isAVIF(byte[] header, int bytesRead) {
        return bytesRead >= 12 &&
                header[4] == 0x66 &&  // f
                header[5] == 0x74 &&  // t
                header[6] == 0x79 &&  // y
                header[7] == 0x70 &&  // p
                new String(header, 8, 4, StandardCharsets.US_ASCII)
                        .matches("avif|mif1");
    }
    // -------------获取文件格式------------------

    /**
     * 获取图片信息
     *
     * @param is 输入流
     * @return 图片格式（jpg、png、bmp...）
     */
    public static String getImageFormat(InputStream is) {
        // 获取图片格式
        try (is) {
            byte[] header = new byte[8];
            int bytesRead = is.read(header);
            // 从字节数组中获取图片类型
            return determineFileType(header, bytesRead);
        } catch (IOException e) {
            log.error("文件读取失败: ", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "文件读取失败");
        }
    }


    /**
     * 根据文件头字节判断常见图片类型
     */
    public static String determineFileType(byte[] header, int bytesRead) {
        if (bytesRead < 2) return null;
        // 检查 BMP（"BM"）
        if (matchBytes(header, new byte[]{0x42, 0x4D})) { // "BM" 的十六进制
            return "bmp";
        }
        // 检查 JPEG（FF D8 FF）
        if (bytesRead >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        // 检查 PNG（89 50 4E 47 0D 0A 1A 0A）
        if (bytesRead >= 8
                && header[0] == (byte) 0x89
                && header[1] == 0x50
                && header[2] == 0x4E
                && header[3] == 0x47
                && header[4] == 0x0D
                && header[5] == 0x0A
                && header[6] == 0x1A
                && header[7] == 0x0A) {
            return "png";
        }
        // 检查 GIF（"GIF87a" 或 "GIF89a"）
        if (bytesRead >= 6
                && header[0] == 'G'
                && header[1] == 'I'
                && header[2] == 'F'
                && header[3] == '8'
                && (header[4] == '7' || header[4] == '9')
                && header[5] == 'a') {
            return "gif";
        }
        return null;
    }

    /**
     * 检查字节数组是否匹配指定模式
     */
    private static boolean matchBytes(byte[] actual, byte[] expected) {
        if (actual.length < expected.length) return false;
        for (int i = 0; i < expected.length; i++) {
            if (actual[i] != expected[i]) return false;
        }
        return true;
    }


    /**
     * 获取图片信息
     *
     * @param is 输入流
     */
    public static UploadPictureResult getPictureInfo(InputStream is) {
        UploadPictureResult result = new UploadPictureResult();
        // 获取图片的长宽比等信息
        try (CountingInputStream cis = new CountingInputStream(is)) {
            // 读取前 8 个字节用于检测文件类型
            cis.mark(8);
            byte[] header = new byte[8];
            int bytesRead = cis.read(header);
            cis.reset(); // 重置流供后续读取
            // 从字节数组中获取图片类型
            String pictureType = MrsStreamUtil.determineFileType(header, bytesRead);
            BufferedImage bufferedImage = ImageIO.read(cis);
            if (bufferedImage == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件读取失败");
            }
            // 获取图像大小（-8是前面指针重置有点问题，导致文件大小会少1B）
            // 但又不影响正常使用
            long picSize = cis.getSize() - 8;
            int width = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();
            double scale = NumberUtil.round(width * 1.0 / height, 2).doubleValue();
            result.setPicWidth(width);
            result.setPicHeight(height);
            result.setPicScale(scale);
            result.setPicSize(picSize);
            result.setPicFormat(pictureType);
            log.info("图片信息: {}", result);
        } catch (IOException e) {
            log.error("文件读取失败: ", e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "文件读取失败");
        }
        return result;
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

        public float avgH() {
            return sumH / count;
        }

        public float avgS() {
            return sumS / count;
        }

        public float avgL() {
            return sumL / count;
        }
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