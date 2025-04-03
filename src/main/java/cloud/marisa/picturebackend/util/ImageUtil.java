package cloud.marisa.picturebackend.util;

import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.compress.utils.IOUtils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

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


    public static void main(String[] args) {
        String path = "C:\\Users\\Marisa\\Desktop\\thems\\126638656_p0.png";
        // String path = "C:\\Users\\Marisa\\Desktop\\thems\\test.webp";
        File file = new File(path);
        try (InputStream fis = new FileInputStream(file)) {
            long current = System.currentTimeMillis();
            InputStream thumbnail = createThumbnail(fis, 1000);
            System.out.println("压缩耗时：" + (System.currentTimeMillis() - current));
            thumbnail.close();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
}