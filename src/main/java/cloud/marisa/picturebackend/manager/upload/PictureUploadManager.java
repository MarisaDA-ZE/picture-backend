package cloud.marisa.picturebackend.manager.upload;

import cloud.marisa.picturebackend.config.PictureConfig;
import cloud.marisa.picturebackend.config.aliyun.oss.AliyunOssConfigProperties;
import cloud.marisa.picturebackend.entity.dao.Picture;
import cloud.marisa.picturebackend.entity.dto.file.UploadPictureResult;
import cloud.marisa.picturebackend.util.ImageUtil;
import cloud.marisa.picturebackend.util.colors.ColorUtils;
import cloud.marisa.picturebackend.util.colors.DominantColorExtractor;
import cloud.marisa.picturebackend.util.colors.MrsColorHSV;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.crypto.digest.MD5;
import com.alibaba.fastjson2.JSONObject;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.OSSObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Date;

import static cloud.marisa.picturebackend.common.Constants.THUMB_MAX_SIZE;

/**
 * @author MarisaDAZE
 * @description 图片上传管理器
 * @date 2025/4/18
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class PictureUploadManager {

    /**
     * 上传的图片信息是否要从本地流中获取
     * <p>如果不是，则会尝试从OSS获取，但会增加一次请求</p>
     */
    @Value("${mrs.file.picture.is-use-oss:false}")
    private Boolean useOss;

    /**
     * 对象存储客户端
     */
    private final OSS ossClient;

    /**
     * 对象存储配置信息
     */
    private final AliyunOssConfigProperties properties;

    /**
     * 图片配置信息
     */
    private final PictureConfig pictureConfig;


    /**
     * 上传图片并获取图片相关参数
     * <p>通过这个方式上传的图片，你只能拿到原图URL和图片名称</p>
     * <p>其它参数会在一分钟内陆续同步到数据库</p>
     *
     * @param fileName 当前文件名（example.jpg）
     * @param savePath 保存路径（path/to/...）
     * @param is       文件内容输入流（原图的流）
     */
    public UploadPictureResult uploadPictureInputStream(String fileName, String savePath, InputStream is) {
        String bucketName = properties.getBucketName();
        String compressSuffix = pictureConfig.getCompressImageType();
        UploadPictureResult result = new UploadPictureResult();
        try {
            Path tempFile = null;
            try {
                tempFile = Files.createTempFile("copy", ".tmp");
                Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
                // 有的方式获取到的文件名没有后缀
                if (fileName.lastIndexOf(".") == -1) {
                    String fType = ImageUtil.getPictureType(Files.newInputStream(tempFile));
                    log.info("图片格式为空，开始获取图片格式 {}", fType);
                    fType = fType.startsWith(".") ? fType : "." + fType;
                    fileName = fileName + fType;
                }

                // 三种格式的图片保存路径
                String thumbName = createFileName(savePath + "thumb-" + fileName, compressSuffix);// .../xxx.webp
                String defaultName = createFileName(savePath + "default-" + fileName);  // path/to/<time_md5>.jpg
                String originalName = createFileName(savePath + "original-" + fileName); // path/to/<time_md5>.jpg
                log.info("拇指图保存地址 {}", thumbName);
                log.info("默认图保存地址 {}", defaultName);
                log.info("原始图保存地址 {}", originalName);


                // 上传原图（必须在获取图像信息之前上传，因为图像信息有可能通过OSS解析）
                ossClient.putObject(bucketName, originalName, Files.newInputStream(tempFile));
                // 处理默认图，可能要压缩
                float rate = pictureConfig.getCompressRate();
                InputStream defaultPictureStream = Files.newInputStream(tempFile);
                if (rate < 1) {
                    defaultName = createFileName(savePath + "default-" + fileName, compressSuffix);
                    defaultPictureStream = ImageUtil.compressImage(defaultPictureStream, rate, compressSuffix);
                }
                // 处理缩略图
                InputStream thumbnailStream = ImageUtil.createThumbnail(Files.newInputStream(tempFile), THUMB_MAX_SIZE, compressSuffix);
                byte[] thumbnailBytes = new byte[]{};
                String rgbColor;
                // 根据配置选择是本地解析图片信息还是云解析图片信息
                if (useOss == null || !useOss) {
                    // 本地解析图片信息，比较消耗性能（图片宽高、主要颜色都是通过流的方式获取）
                    result = getPictureInfoLocal(is);
                    thumbnailBytes = thumbnailStream.readAllBytes();
                    Color color = DominantColorExtractor.extractDominantColor(new ByteArrayInputStream(thumbnailBytes));
                    rgbColor = ColorUtils.toRGBString(color);
                } else {
                    // 从对象存储服务器读取图片信息，会有额外一次网络请求
                    // 可以获取图像宽高、格式、尺寸等信息
                    // Thread.sleep(2000);
                    result = getPictureInfoOSS(originalName);
                    // 图片主色调（从OSS获取）
                    rgbColor = getPictureMainColor(originalName);
                    result.setPicColor(rgbColor);
                }
                result.setPicName(fileName);
                // 图片指纹
                String hex = MD5.create().digestHex(Files.newInputStream(tempFile));
                result.setMd5(hex);
                // 主颜色的hsv分量
                MrsColorHSV colorHSV = ColorUtils.toHSV(rgbColor);
                result.setMColorHue(colorHSV.getHue());
                result.setMColorSaturation(colorHSV.getSaturation());
                result.setMColorValue(colorHSV.getValue());
                // 主要颜色的色调、饱和度、明度桶号
                result.setMHueBucket(colorHSV.getHueBucket());
                result.setMSaturationBucket(colorHSV.getSaturationBucket());
                result.setMValueBucket(colorHSV.getValueBucket());
                // 上传缩略图
                if (useOss == null || !useOss) {
                    // 本地解析图片色调时已经读过一遍流了，所以这里得new一个字节流
                    ossClient.putObject(bucketName, thumbName, new ByteArrayInputStream(thumbnailBytes));
                } else {
                    ossClient.putObject(bucketName, thumbName, thumbnailStream);
                }
                // 上传默认图
                ossClient.putObject(bucketName, defaultName, defaultPictureStream);
                // 文件保存地址
                result.setThumbPath(thumbName);
                result.setSavedPath(defaultName);
                result.setOriginalPath(originalName);
                // 图片地址都是固定的，可以通过properties的参数拼接出来
                String endpoint = trimURL(properties.getEndpoint());
                String urlPrefix = String.format("https://%s.%s/", bucketName, endpoint);
                result.setThumbnailUrl(urlPrefix + thumbName);
                result.setUrl(urlPrefix + defaultName);
                result.setOriginalUrl(urlPrefix + originalName);
            } finally {
                if (tempFile != null) Files.deleteIfExists(tempFile);
            }
        } catch (Exception e) {
            log.error("读取图片信息失败", e);
        }
        log.info("图片信息: {}", result);
        return result;
    }


    /**
     * 上传图片并获取图片相关参数
     * <p>通过这个方式上传的图片，你只能拿到原图URL和图片名称</p>
     * <p>其它参数会在一分钟内陆续同步到数据库</p>
     *
     * @param fileName 文件全路径名（/path/to/example.txt）
     * @param is       文件内容输入流（原图的流）
     */
    public UploadPictureResult uploadPictureInputStream2(String fileName, InputStream is) {
        String bucketName = properties.getBucketName();
        UploadPictureResult result = new UploadPictureResult();
        // 上传图片
        ossClient.putObject(bucketName, fileName, is);
        // 获取图片URL（原图URL 5分钟后过期）
        Date expiration = new Date(new Date().getTime() + 300 * 1000L);
        URL url = ossClient.generatePresignedUrl(bucketName, fileName, expiration);
        result.setOriginalPath(fileName);
        result.setOriginalUrl(url.toString());
        // 保存入库的时候去发起异步任务
        return result;
        /*
        TODO: 二期工程再说吧
         step.1 上传
         马上就要：（上传成功时同步获取）
         - 原图URL（在编辑那里展示，也可能用于AI扩图或在线编辑）
         - 图片名称

         不是那么急，可以等几秒再拿到的：
         - 拇指图URL、默认图URL
         - 图片大小、宽高、比例、颜色、格式
         - hsv分桶等信息
         */
    }


    /**
     * 异步同步图片信息到数据库
     *
     * <p>文件名是 /path/to/＜time_md5＞.jpg 的格式</p>
     *
     * @param picture 不完整的图片对象
     * @param is      文件内容输入流（原图的流）
     */
    public UploadPictureResult uploadPictureInputStreamAsync(Picture picture, InputStream is) {
        String bucketName = properties.getBucketName();
        String pictureName = picture.getName(); // xxx（图片名）
        String originalPath = picture.getOriginalPath(); // 原图路径 /path/to/<time_md5>.jpg
        String path = originalPath.substring(originalPath.lastIndexOf("/"));    // /path/to/
        String sfx = originalPath.substring(originalPath.lastIndexOf(".") + 1);    // jpg
        String uploadName = "";
        UploadPictureResult result = new UploadPictureResult();
        try {
            Path tempFile = null;
            try {
                tempFile = Files.createTempFile("copy", ".tmp");
                Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
                // 处理默认图，可能要压缩
                String defaultName = createFileName("default-" + uploadName); // path/to/<time_md5>.jpg
                float rate = pictureConfig.getCompressRate();
                String compressSuffix = pictureConfig.getCompressImageType();
                InputStream defaultPictureStream = Files.newInputStream(tempFile);
                if (rate < 1) {
                    // path/to/<time_md5>.webp
                    defaultName = createFileName("default-" + uploadName, compressSuffix);
                    defaultPictureStream = ImageUtil.compressImage(defaultPictureStream, rate, compressSuffix);
                }
                // 处理缩略图
                // path/to/<time_md5>.webp
                String thumbName = createFileName("thumb-" + uploadName, compressSuffix);
                InputStream thumbnailStream = ImageUtil.createThumbnail(Files.newInputStream(tempFile), THUMB_MAX_SIZE, compressSuffix);

                // 根据配置选择是本地解析图片信息还是云解析图片信息
                if (useOss == null || useOss) {
                    // 本地读取图片信息，比较消耗性能
                    result = getPictureInfoLocal(is);
                } else {
                    // 从对象存储服务器读取图片信息，会有额外一次网络请求
                    result = getPictureInfoOSS(uploadName);
                }
                // 图片指纹
                String hex = MD5.create().digestHex(Files.newInputStream(tempFile));
                result.setMd5(hex);
                // 图片主色调（从OSS获取）
                // TODo
                String rgbColor = getPictureMainColor("fileName");
                result.setPicColor(rgbColor);
                // 主颜色的hsv分量
                MrsColorHSV colorHSV = ColorUtils.toHSV(rgbColor);
                result.setMColorHue(colorHSV.getHue());
                result.setMColorSaturation(colorHSV.getSaturation());
                result.setMColorValue(colorHSV.getValue());
                // 主要颜色的色调、饱和度、明度桶号
                result.setMHueBucket(colorHSV.getHueBucket());
                result.setMSaturationBucket(colorHSV.getSaturationBucket());
                result.setMValueBucket(colorHSV.getValueBucket());

                // 上传默认图
                ossClient.putObject(bucketName, defaultName, defaultPictureStream);
                // 上传缩略图
                ossClient.putObject(bucketName, thumbName, thumbnailStream);
                // 没有缩略图URL和默认图URL，它们都是受保护的
                // 在使用OSS模式时，访问图片需要从OSS服务器获取

                /*
                 step.1 上传
                 马上就要：（上传成功时同步获取）
                 - 原图URL（在编辑那里展示，也可能用于AI扩图或在线编辑）
                 - 图片名称

                 不是那么急，可以等几秒再拿到的：
                 - 拇指图URL、默认图URL
                 - 图片大小、宽高、比例、颜色、格式
                 - hsv分桶等信息
                * */
            } finally {
                if (tempFile != null) Files.deleteIfExists(tempFile);
            }
        } catch (Exception e) {
            log.error("读取图片信息失败", e);
        }
        log.info("图片信息: {}", result);
        return result;
    }

    /**
     * 获取图片的基础信息
     * <p>如宽高、比例、图片格式等</p>
     *
     * @param is 文件内容输入流
     */
    public UploadPictureResult getPictureInfoLocal(InputStream is) {
        try {
            Path tempFile = null;
            try {
                tempFile = Files.createTempFile("copy", ".tmp");
                Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
                return ImageUtil.getPictureInfo(Files.newInputStream(tempFile));
            } finally {
                if (tempFile != null) Files.deleteIfExists(tempFile);
            }
        } catch (Exception e) {
            log.error("读取图片信息失败", e);
        }
        return null;
    }

    /**
     * 获取图片的基础信息
     * <p>如宽高、比例、图片格式等</p>
     *
     * @param fileName 文件全路径名（/path/to/example.jpg）
     */
    public UploadPictureResult getPictureInfoOSS(String fileName) {
        UploadPictureResult result = new UploadPictureResult();
        String bucketName = properties.getBucketName();
        GetObjectRequest request = new GetObjectRequest(bucketName, fileName);
        request.setProcess("image/info");
        try (OSSObject ossObject = ossClient.getObject(request);
             InputStreamReader isr = new InputStreamReader(ossObject.getObjectContent())) {
            StringBuilder content = new StringBuilder();
            char[] chars = new char[2048];
            int read;
            while ((read = isr.read(chars)) != -1) {
                content.append(chars, 0, read);
            }
            // 解析结果并返回
            JSONObject jsonObject = JSONObject.parseObject(content.toString());
            Long picSize = jsonObject.getJSONObject("FileSize").getLong("value");
            String suffix = jsonObject.getJSONObject("Format").getString("value");
            Integer picWidth = jsonObject.getJSONObject("ImageWidth").getInteger("value");
            Integer picHeight = jsonObject.getJSONObject("ImageHeight").getInteger("value");
            double scale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
            result.setPicWidth(picWidth);
            result.setPicHeight(picHeight);
            result.setPicScale(scale);
            result.setPicSize(picSize);
            result.setPicFormat(suffix);
            return result;
        } catch (IOException e) {
            log.error("获取图片附加信息失败: ", e);
        }
        return null;
    }

    /**
     * 获取图片的主色调信息
     *
     * @param fileName 文件全路径（/path/to/example.jpg）
     * @return 图片主色调
     */
    public String getPictureMainColor(String fileName) {
        String bucketName = properties.getBucketName();
        GetObjectRequest request = new GetObjectRequest(bucketName, fileName);
        request.setProcess("image/average-hue");
        try (OSSObject ossObject = ossClient.getObject(request);
             InputStreamReader isr = new InputStreamReader(ossObject.getObjectContent());
             BufferedReader br = new BufferedReader(isr)) {
            String hex = JSONObject.parseObject(br.readLine()).getString("RGB");
            return ColorUtils.toRGBString(Color.decode(hex));
        } catch (IOException e) {
            log.error("获取图片颜色信息失败: ", e);
        }
        return null;
    }

    /**
     * 生成文件名、保存路径等信息
     *
     * @param fileName 文件全路径（/path/to/example.jpg）
     * @return 生成的hex文件路径（形如/path/to/20250101_<16位md5摘要>.jpg）
     */
    private static String createFileName(String fileName) {
        String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);
        return createFileName(fileName, suffix);
    }

    /**
     * 生成文件名、保存路径等信息
     *
     * @param fileName 文件全路径（/path/to/example.jpg）
     * @param suffix   文件后缀名
     * @return 生成的hex文件路径（形如/path/to/20250101_<16位md5摘要>.suffix）
     */
    private static String createFileName(String fileName, String suffix) {
        String path = fileName.substring(0, fileName.lastIndexOf("/"));
        String name = fileName.substring(fileName.lastIndexOf("/") + 1, fileName.lastIndexOf("."));
        String uuid = MD5.create().digestHex(System.currentTimeMillis() + "_" + name);
        String dateFormat = DateUtil.format(new Date(), "yyyyMMdd");
        suffix = suffix.startsWith(".") ? suffix.substring(1) : suffix;
        // 最后的文件名应该是/path/to/20250101_<16位md5摘要>.jpg
        return String.format("%s/%s_%s.%s", path, dateFormat, uuid, suffix);
    }

    private static String trimURL(String url) {
        if (url.startsWith("http://")) {
            return url.substring(7);
        } else if (url.startsWith("https://")) {
            return url.substring(8);
        }
        return url;
    }
}
