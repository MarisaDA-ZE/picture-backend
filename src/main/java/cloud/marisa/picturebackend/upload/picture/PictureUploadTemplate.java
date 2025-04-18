package cloud.marisa.picturebackend.upload.picture;

import cloud.marisa.picturebackend.api.image.AliyunOssUtil;
import cloud.marisa.picturebackend.config.PictureConfig;
import cloud.marisa.picturebackend.entity.dto.file.UploadPictureResult;
import cloud.marisa.picturebackend.exception.BusinessException;
import cloud.marisa.picturebackend.exception.ErrorCode;
import cloud.marisa.picturebackend.util.ImageUtil;
import cloud.marisa.picturebackend.util.MinioUtil;
import cloud.marisa.picturebackend.util.MrsStreamUtil;
import cloud.marisa.picturebackend.util.colors.ColorUtils;
import cloud.marisa.picturebackend.util.colors.DominantColorExtractor;
import cloud.marisa.picturebackend.util.colors.MrsColorHSV;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.crypto.digest.MD5;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Date;

import static cloud.marisa.picturebackend.common.Constants.THUMB_MAX_SIZE;

/**
 * @author MarisaDAZE
 * @description 图片上传模板类
 * @date 2025/4/2
 */
@Slf4j
@Component
public abstract class PictureUploadTemplate {

    /**
     * MinIO上传工具类
     */
    @Resource
    private MinioUtil minioUtil;


    /**
     * 图片配置信息
     */
    @Resource
    private PictureConfig pictureConfig;

    /**
     * 上传图片文件
     *
     * @param inputSource 文件输入（文件或URL）
     * @param pathPrefix  保存路径前缀
     * @return 文件信息
     */
    public final UploadPictureResult uploadPictureObject(Object inputSource, String pathPrefix) {
        // 校验文件信息
        validPicture(inputSource);
        UploadPictureResult result = new UploadPictureResult();
        // 获取文件名（不含文件扩展名）
        String fileName = getFileName(inputSource);
        log.info("原始文件名 {}", fileName);
        // 压缩后的图片类型，默认是webp
        String compressSuffix = pictureConfig.getCompressImageType();
        // 要上传的图片信息（对象中包含图片名、文件后缀、OSS保存地址）
        FileNameInfo thumbInfo;     // 拇指图信息
        FileNameInfo defaultInfo;   // 默认图信息
        FileNameInfo originalInfo;  // 原图信息
        // 保存图片
        try (InputStream inputStream = getPictureStream(inputSource)) {
            // 准备临时文件
            Path tempFile = Files.createTempFile("copy", ".tmp");
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            // 获取图片信息（原图信息）
            UploadPictureResult pictureInfo = ImageUtil.getPictureInfo(Files.newInputStream(tempFile));
            BeanUtils.copyProperties(pictureInfo, result);
            String suffix = result.getPicFormat();
            // 处理上传后的文件名
            thumbInfo = createFileName("thumb-" + fileName, compressSuffix, pathPrefix);
            defaultInfo = createFileName("default-" + fileName, suffix, pathPrefix);
            originalInfo = createFileName("original-" + fileName, suffix, pathPrefix);

            InputStream defaultPictureStream = Files.newInputStream(tempFile);
            // 如果默认图要进行压缩
            float rate = pictureConfig.getCompressRate();
            if (rate < 1) {
                defaultInfo = createFileName("default-" + fileName, compressSuffix, pathPrefix);
                defaultPictureStream = ImageUtil.compressImage(defaultPictureStream, rate, compressSuffix);
            }
            log.info("拇指图保存信息 {}", thumbInfo);
            log.info("默认图保存信息 {}", defaultInfo);
            log.info("原图保存信息 {}", originalInfo);

            // 上传压缩后的图片（默认图，可能会被压缩）
            minioUtil.upload(defaultPictureStream, defaultInfo.getFilePath());
            // 上传缩略图
            InputStream thumbnailStream = ImageUtil.createThumbnail(Files.newInputStream(tempFile), THUMB_MAX_SIZE, compressSuffix);
            byte[] thumbnailBytes = thumbnailStream.readAllBytes(); // 缩略图的内存小，通常在50KB左右
            thumbnailStream.close();
            minioUtil.upload(new ByteArrayInputStream(thumbnailBytes), thumbInfo.getFilePath());
            // 获取图片主要颜色
            // Color color = ImageUtil.getDominantColor2(new ByteArrayInputStream(thumbnailBytes));
            Color color = DominantColorExtractor.extractDominantColor(new ByteArrayInputStream(thumbnailBytes));
            String rgbString = ColorUtils.toRGBString(color);
            result.setPicColor(rgbString);
            // 主颜色的hsv分量
            MrsColorHSV colorHSV = ColorUtils.toHSV(color);
            result.setMColorHue(colorHSV.getHue());
            result.setMColorSaturation(colorHSV.getSaturation());
            result.setMColorValue(colorHSV.getValue());
            // 主要颜色的色调、饱和度、明度桶号
            result.setMHueBucket(colorHSV.getHueBucket());
            result.setMSaturationBucket(colorHSV.getSaturationBucket());
            result.setMValueBucket(colorHSV.getValueBucket());
            // 上传原图
            minioUtil.upload(Files.newInputStream(tempFile), originalInfo.getFilePath());
            // 图片指纹
            String hex = MD5.create().digestHex(Files.newInputStream(tempFile));
            result.setMd5(hex);
            // 删除临时文件
            Files.deleteIfExists(tempFile);
        } catch (IOException e) {
            log.error("图片上传失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        // 默认图的参数信息
        result.setPicName(originalInfo.getFileName());
        // 默认图
        result.setSavedPath(defaultInfo.getFilePath());
        result.setUrl(minioUtil.getFileUrl(defaultInfo.getFilePath()));
        // 缩略图
        result.setThumbPath(thumbInfo.getFilePath());
        result.setThumbnailUrl(minioUtil.getFileUrl(thumbInfo.getFilePath()));
        // 原图
        result.setOriginalPath(originalInfo.getFilePath());
        result.setOriginalUrl(minioUtil.getFileUrl(originalInfo.getFilePath()));
        return result;
    }


    /**
     * 校验图片是否符合规范
     *
     * @param inputSource 文件对象
     */
    protected abstract void validPicture(Object inputSource);

    /**
     * 获取文件原始名称
     *
     * @param inputSource 文件对象
     * @return 结果
     */
    protected abstract String getFileName(Object inputSource);

    /**
     * 获取输入流
     *
     * @param inputSource 文件对象/URL
     * @return 输入流
     */
    public abstract InputStream getPictureStream(Object inputSource);

    /**
     * 获取图片大小
     *
     * @param inputSource 输入源
     * @return 结果
     */
    public abstract Long getPictureSize(Object inputSource);

    /**
     * 生成文件名、保存路径等信息
     *
     * @param fileName   文件原始名称
     * @param suffix     文件后缀名
     * @param pathPrefix 路径前缀
     * @return 文件信息的DTO封装
     */
    private FileNameInfo createFileName(String fileName, String suffix, String pathPrefix) {
        String uuid = MD5.create().digestHex(System.currentTimeMillis() + "_" + fileName);
        String dateFormat = DateUtil.format(new Date(), "yyyyMMdd");
        suffix = suffix.startsWith(".") ? suffix.substring(1) : suffix;
        // 最后的文件名应该是20250330_<16位md5摘要>.jpg
        String uploadName = String.format("%s_%s.%s", dateFormat, uuid, suffix);
        String uploadPath = (pathPrefix.endsWith("/") ? pathPrefix : pathPrefix + "/") + uploadName;
        return new FileNameInfo(uploadName, suffix, uploadPath);
    }
    @Data
    @ToString
    @AllArgsConstructor
    static class FileNameInfo {
        private String fileName;
        private String suffix;
        private String filePath;
    }
}
