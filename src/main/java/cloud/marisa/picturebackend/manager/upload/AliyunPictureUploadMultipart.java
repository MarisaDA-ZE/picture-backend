package cloud.marisa.picturebackend.manager.upload;

import cloud.marisa.picturebackend.api.image.AliyunOssUtil;
import cloud.marisa.picturebackend.config.PictureConfig;
import cloud.marisa.picturebackend.config.aliyun.oss.AliyunOssConfigProperties;
import cloud.marisa.picturebackend.entity.dto.file.UploadPictureResult;
import cloud.marisa.picturebackend.exception.BusinessException;
import cloud.marisa.picturebackend.exception.ErrorCode;
import cloud.marisa.picturebackend.upload.picture.CountingInputStream;
import cloud.marisa.picturebackend.util.ImageUtil;
import cloud.marisa.picturebackend.util.MrsPathUtil;
import cloud.marisa.picturebackend.util.colors.ColorUtils;
import cn.hutool.core.io.unit.DataSize;
import com.alibaba.fastjson2.JSONObject;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.OSSObject;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.io.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

import static cloud.marisa.picturebackend.common.Constants.THUMB_MAX_SIZE;

/**
 * @author MarisaDAZE
 * @description 文件上传类（本地文件上传至OSS服务器）
 * @date 2025/4/19
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class AliyunPictureUploadMultipart extends AliyunPictureUploadManager {

    /**
     * 阿里云对象存储工具类
     */
    private final AliyunOssUtil ossUtil;

    /**
     * 对象存储配置信息
     */
    private final AliyunOssConfigProperties ossProperties;

    /**
     * 图片配置信息
     */
    private final PictureConfig pictureConfig;

    /**
     * 图片异步处理线程池
     */
    private final ThreadPoolExecutor pictureUploadPoolExecutor;

    /**
     * 将文件上传到对应的文件服务器
     *
     * @param inputSource 要保存的文件对象
     * @param savePath    保存全路径（/path/to/...）
     * @return 图片基础信息
     */
    public UploadPictureResult uploadPicture(
            Object inputSource,
            String savePath) {
        if (inputSource == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 图片异步处理
        MultipartFile file = (MultipartFile) inputSource;
        // 获取图片MD5
        CompletableFuture<String> getMd5Future = CompletableFuture.supplyAsync(
                () -> calculateMD5(file),
                pictureUploadPoolExecutor);
        // 获取图片尺寸信息
        CompletableFuture<PictureSizeInfo> dimensionFuture = CompletableFuture.supplyAsync(
                () -> getImageInfo(file),
                pictureUploadPoolExecutor);
        // 上传 缩略图 到 文件服务器
        CompletableFuture<UploadInfo> thumbImageFuture = CompletableFuture.supplyAsync(
                () -> uploadThumbImage(file, savePath),
                pictureUploadPoolExecutor);
        // 上传 默认图 到 文件服务器
        CompletableFuture<UploadInfo> defaultImageFuture = CompletableFuture.supplyAsync(
                () -> uploadDefaultImage(file, savePath),
                pictureUploadPoolExecutor);
        // 上传 原图 到 文件服务器
        CompletableFuture<UploadInfo> originalImageFuture = CompletableFuture.supplyAsync(
                () -> uploadOriginalImage(file, savePath),
                pictureUploadPoolExecutor);
        // 等待所有任务完成
        CompletableFuture.allOf(
                        getMd5Future,
                        dimensionFuture,
                        defaultImageFuture,
                        thumbImageFuture,
                        originalImageFuture)
                .join();
        // 获取各个任务的结果（此时不会阻塞）
        UploadInfo originalImageInfo = originalImageFuture.join();
        String mainColor = getPictureMainColor(originalImageInfo.getSavePath());
        // 组装结果并返回
        AsyncParameters parameters = AsyncParameters.builder()
                .picMD5(getMd5Future.join())
                .sizeInfo(dimensionFuture.join())
                .thumbImageInfo(thumbImageFuture.join())
                .defaultImageInfo(defaultImageFuture.join())
                .originalImageInfo(originalImageInfo)
                .mainColor(mainColor)
                .build();
        log.info("Multipart上传==>异步处理结果: {}", JSONObject.toJSONString(parameters));
        return getResult(parameters);
    }

    /**
     * 上传缩略图文件到 文件服务器
     *
     * @param file     图片文件
     * @param savePath 保存全路径（/path/to/...）
     * @return 缩略图URL
     */
    private UploadInfo uploadThumbImage(MultipartFile file, String savePath) {
        String fileName = file.getOriginalFilename();
        String suffix = pictureConfig.getCompressImageType();
        String trimName = MrsPathUtil.trimName(fileName);
        String trimSuffix = MrsPathUtil.trimSuffix(suffix);
        String uploadPath = createFileName("thumbnail_" + trimName, savePath, trimSuffix);
        try (InputStream is = file.getInputStream()) {
            InputStream thumbIs = ImageUtil.createThumbnail(is, THUMB_MAX_SIZE, suffix);
            log.info("Multipart上传==>保存拇指图（原始）: {}",
                    String.format("%s/<time>_%s.%s", savePath, "thumbnail_" + trimName, trimSuffix));
            log.info("Multipart上传==>保存拇指图（路径）: {}", uploadPath);
            ossUtil.uploadInputStream(uploadPath, thumbIs);
            // 封装返回结果
            String publicURL = getPicturePublicURL(uploadPath);
            return new UploadInfo(publicURL, trimName + "." + trimSuffix, uploadPath);
        } catch (IOException ex) {
            log.error("Multipart上传==>缩略图上传失败: ", ex);
        }
        return null;
    }

    /**
     * 上传默认图到 文件服务器
     *
     * @param file     图片文件
     * @param savePath 保存全路径（/path/to/...）
     * @return 默认图URL
     */
    private UploadInfo uploadDefaultImage(MultipartFile file, String savePath) {
        try (InputStream is = file.getInputStream()) {
            InputStream uploadIs = is;
            String suffix = file.getContentType();
            // 检查是否需要压缩
            long compressSize = DataSize.parse(pictureConfig.getCompressMaxSize()).toBytes();
            if (file.getSize() > compressSize) {
                float rate = pictureConfig.getCompressRate();
                suffix = pictureConfig.getCompressImageType();
                uploadIs = ImageUtil.compressImage(is, rate, suffix);
            }
            String trimName = MrsPathUtil.trimName(file.getOriginalFilename());
            String trimSuffix = MrsPathUtil.trimSuffix(suffix);
            String uploadPath = createFileName("default_" + trimName, savePath, trimSuffix);
            log.info("Multipart上传==>保存默认图（原始）: {}",
                    String.format("%s/<time>_%s.%s", savePath, "default_" + trimName, trimSuffix));
            log.info("Multipart上传==>保存默认图（路径）: {}", uploadPath);
            ossUtil.uploadInputStream(uploadPath, uploadIs);
            // 封装返回结果
            String publicURL = getPicturePublicURL(uploadPath);
            return new UploadInfo(publicURL, trimName + "." + trimSuffix, uploadPath);
        } catch (IOException ex) {
            log.error("Multipart上传==>默认图上传失败: ", ex);
        }
        return null;
    }

    /**
     * 上传原图到 文件服务器
     *
     * @param file     图片文件
     * @param savePath 保存全路径（/path/to/...）
     * @return 原图URL
     */
    private UploadInfo uploadOriginalImage(MultipartFile file, String savePath) {
        String fileName = file.getOriginalFilename();
        String trimName = MrsPathUtil.trimName(fileName);
        String trimSuffix = MrsPathUtil.trimSuffix(fileName);
        String uploadPath = createFileName("original_" + trimName, savePath, trimSuffix);
        try (InputStream is = file.getInputStream()) {
            log.info("Multipart上传==>保存原图（原始）: {}",
                    String.format("%s/<time>_%s.%s", savePath, "original_" + trimName, trimSuffix));
            log.info("Multipart上传==>保存原图（路径）: {}", uploadPath);
            ossUtil.uploadInputStream(uploadPath, is);
            // 封装返回结果
            String publicURL = getPicturePublicURL(uploadPath);
            return new UploadInfo(publicURL, trimName + "." + trimSuffix, uploadPath);
        } catch (IOException ex) {
            log.error("Multipart上传==>原图上传失败: ", ex);
        }
        return null;
    }

    /**
     * 获取一张图的永久公网访问地址
     *
     * @param savePath 图片在OSS上的位置
     * @return 图片的URL路径
     */
    @Override
    public String getPicturePublicURL(String savePath) {
        String endpoint = ossProperties.getEndpoint();
        String ep = MrsPathUtil.trimAgreementWeb(endpoint);
        String bucketName = ossProperties.getBucketName();
        return String.format("https://%s.%s/%s", bucketName, ep, savePath);
    }

    /**
     * 获取图片的主色调信息
     *
     * @param fileName 文件全路径（/path/to/example.jpg）
     * @return 图片主色调
     */
    @Override
    public String getPictureMainColor(String fileName) {
        GetObjectRequest request = new GetObjectRequest(ossProperties.getBucketName(), fileName);
        request.setProcess("image/average-hue");
        try (OSSObject ossObject = ossUtil.getObject(request);
             InputStreamReader isr = new InputStreamReader(ossObject.getObjectContent());
             BufferedReader br = new BufferedReader(isr)) {
            String hex = JSONObject.parseObject(br.readLine()).getString("RGB");
            return ColorUtils.toRGBString(Color.decode(hex));
        } catch (IOException e) {
            log.error("Multipart上传==>获取图片颜色信息失败: ", e);
        }
        return null;
    }

    /**
     * 获取文件的MD5
     *
     * @param file 图片文件
     * @return 文件MD5
     */
    private String calculateMD5(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            return calculateMD5(is);
        } catch (IOException e) {
            log.error("获取文件MD5失败: ", e);
        }
        return null;
    }

    /**
     * 获取图片文件的基础数据
     * <p>如宽高、大小等</p>
     *
     * @param file 图片文件
     * @return 图片信息
     */
    private PictureSizeInfo getImageInfo(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            PictureSizeInfo imageInfo = getImageInfo(new CountingInputStream(is));
            long size = file.getSize();
            log.info("URL上传==>图片大小（流）:{};（文件）: {}", imageInfo.getSize(), size);
            imageInfo.setSize(size);
            return imageInfo;
        } catch (IOException ex) {
            log.error("获取图片信息失败: ", ex);
        }
        return null;
    }
}
