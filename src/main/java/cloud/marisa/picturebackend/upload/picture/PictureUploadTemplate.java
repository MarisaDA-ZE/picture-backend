package cloud.marisa.picturebackend.upload.picture;

import cloud.marisa.picturebackend.config.PictureConfig;
import cloud.marisa.picturebackend.entity.dto.file.UploadPictureResult;
import cloud.marisa.picturebackend.exception.BusinessException;
import cloud.marisa.picturebackend.exception.ErrorCode;
import cloud.marisa.picturebackend.util.ImageUtil;
import cloud.marisa.picturebackend.util.MinioUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.crypto.digest.MD5;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
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
@Component
public abstract class PictureUploadTemplate {

    @Autowired
    private MinioUtil minioUtil;

    @Autowired
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
        String fileName = getOriginalFilename(inputSource);
        String compressSuffix = pictureConfig.getCompressImageType();
        // 处理上传后的文件名
        String defaultPath = createFileName("default-" + fileName, pathPrefix).getFilePath();
        String thumbPath = createFileName("thumb-" + fileName, pathPrefix, compressSuffix).getFilePath();
        FileNameInfo originalInfo = createFileName("original-" + fileName, pathPrefix);
        String originalPath = originalInfo.getFilePath();
        System.out.println("保存的文件地址: " + originalPath);
        System.out.println("原图大小: " + result.getPicSize());

        try (InputStream inputStream = getPictureStream(inputSource)) {
            // 准备临时文件
            Path tempFile = Files.createTempFile("copy", ".tmp");
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            // 获取图片信息（原图）
            getPictureInfo(Files.newInputStream(tempFile), result);
            // 是否要压缩
            float rate = pictureConfig.getCompressRate();
            InputStream compressed = Files.newInputStream(tempFile);
            if (rate < 1) {
                defaultPath = createFileName("default-" + fileName, pathPrefix, compressSuffix).getFilePath();
                compressed = ImageUtil.compressImage(compressed, rate, compressSuffix);
            }
            // 上传压缩后的图片（默认图，可能会被压缩）
            minioUtil.upload(compressed, defaultPath);
            // 上传缩略图
            InputStream thumbnailStream = ImageUtil.createThumbnail(Files.newInputStream(tempFile), THUMB_MAX_SIZE, compressSuffix);
            minioUtil.upload(thumbnailStream, thumbPath);
            // 上传原图
            minioUtil.upload(Files.newInputStream(tempFile), originalPath);
            // 图片指纹
            String hex = MD5.create().digestHex(Files.newInputStream(tempFile));
            result.setMd5(hex);
            // 删除临时文件
            Files.deleteIfExists(tempFile);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        // 默认图的参数信息
        result.setPicName(originalInfo.getFileName());
        result.setPicFormat(originalInfo.getSuffix());
        // 默认图
        result.setSavedPath(defaultPath);
        result.setUrl(minioUtil.getFileUrl(defaultPath));
        // 缩略图
        result.setThumbPath(thumbPath);
        result.setUrlThumb(minioUtil.getFileUrl(thumbPath));
        // 原图
        result.setOriginalPath(originalPath);
        result.setUrlOriginal(minioUtil.getFileUrl(originalPath));
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
    protected abstract String getOriginalFilename(Object inputSource);

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
     * @param pathPrefix 路径前缀
     * @param suffix     文件后缀名
     * @return 文件信息的DTO封装
     */
    private FileNameInfo createFileName(String fileName, String pathPrefix, String suffix) {
        String uuid = MD5.create().digestHex(System.currentTimeMillis() + "_" + fileName);
        String dateFormat = DateUtil.format(new Date(), "yyyyMMdd");
        suffix = suffix.startsWith(".") ? suffix.substring(1) : suffix;
        // 最后的文件名应该是20250330_<16位md5摘要>.jpg
        String uploadName = String.format("%s_%s.%s", dateFormat, uuid, suffix);
        String uploadPath = (pathPrefix.endsWith("/") ? pathPrefix : pathPrefix + "/") + uploadName;
        return new FileNameInfo(uploadName, suffix, uploadPath);
    }

    /**
     * 生成文件名、保存路径等信息
     *
     * @param fileName   文件原始名称
     * @param pathPrefix 路径前缀
     * @return 文件信息的DTO封装
     */
    private FileNameInfo createFileName(String fileName, String pathPrefix) {
        String uuid = MD5.create().digestHex(System.currentTimeMillis() + "_" + fileName);
        String dateFormat = DateUtil.format(new Date(), "yyyyMMdd");
        String suffix = FileUtil.getSuffix(fileName);
        // 最后的文件名应该是20250330_<16位md5摘要>.jpg
        String uploadName = String.format("%s_%s.%s", dateFormat, uuid, suffix);
        String uploadPath = (pathPrefix.endsWith("/") ? pathPrefix : pathPrefix + "/") + uploadName;
        return new FileNameInfo(uploadName, suffix, uploadPath);
    }

    /**
     * 获取图片信息
     *
     * @param is     输入流
     * @param result 图片信息对象
     */
    public final void getPictureInfo(InputStream is, UploadPictureResult result) {
        // 获取图片的长宽比信息
        try (is) {
            BufferedImage bufferedImage = ImageIO.read(is);
            if (bufferedImage == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件读取失败");
            }
            int width = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();
            result.setPicWidth(width);
            result.setPicHeight(height);
            double scale = NumberUtil.round(width * 1.0 / height, 2).doubleValue();
            result.setPicScale(scale);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "文件读取失败");
        }
    }

    @Data
    @AllArgsConstructor
    static class FileNameInfo {
        private String fileName;
        private String suffix;
        private String filePath;
    }
}
