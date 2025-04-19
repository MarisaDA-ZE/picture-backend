package cloud.marisa.picturebackend.manager.upload;

import cloud.marisa.picturebackend.entity.dto.file.UploadPictureResult;
import cloud.marisa.picturebackend.upload.picture.CountingInputStream;
import cloud.marisa.picturebackend.util.colors.ColorUtils;
import cloud.marisa.picturebackend.util.colors.MrsColorHSV;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.crypto.digest.MD5;
import lombok.*;
import lombok.extern.log4j.Log4j2;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Iterator;

/**
 * @author MarisaDAZE
 * @description 阿里云OSS文件上传管理器
 * @date 2025/4/19
 */
@Log4j2
public abstract class AliyunPictureUploadManager {

    /**
     * 将文件上传到对应的文件服务器
     *
     * @param inputSource 要保存的文件对象
     * @param savePath    保存全路径（/path/to/...）
     * @return 图片基础信息
     */
    protected abstract UploadPictureResult uploadPicture(Object inputSource, String savePath);

    /**
     * 组装返回对象
     *
     * @param parameters 返回参数列表
     * @return 返回对象
     */
    protected UploadPictureResult getResult(AsyncParameters parameters) {
        String picMD5 = parameters.getPicMD5();
        PictureSizeInfo sizeInfo = parameters.getSizeInfo();
        UploadInfo thumbImageInfo = parameters.getThumbImageInfo();
        UploadInfo defaultImageInfo = parameters.getDefaultImageInfo();
        UploadInfo originalImageInfo = parameters.getOriginalImageInfo();
        String mainColor = parameters.getMainColor();
        UploadPictureResult result = new UploadPictureResult();
        // 图片保存信息
        result.setThumbnailUrl(thumbImageInfo.getUrl());
        result.setThumbPath(thumbImageInfo.getSavePath());
        result.setUrl(defaultImageInfo.getUrl());
        result.setSavedPath(defaultImageInfo.getSavePath());
        result.setOriginalUrl(originalImageInfo.getUrl());
        result.setOriginalPath(originalImageInfo.getSavePath());
        // 图像MD5、名字
        result.setMd5(picMD5);
        result.setPicName(originalImageInfo.getFileName());
        // 图像尺寸信息
        result.setPicSize(sizeInfo.getSize());
        result.setPicWidth(sizeInfo.getWidth());
        result.setPicHeight(sizeInfo.getHeight());
        result.setPicScale(sizeInfo.getAspectRatio());
        result.setPicFormat(sizeInfo.getFormat());
        // 图像颜色信息
        result.setPicColor(mainColor);
        // 获取颜色的hsv分量和桶号等信息
        MrsColorHSV colorHSV = ColorUtils.toHSV(mainColor);
        // hsv分量
        result.setMColorHue(colorHSV.getHue());
        result.setMColorSaturation(colorHSV.getSaturation());
        result.setMColorValue(colorHSV.getValue());
        // 主要颜色的色调、饱和度、明度桶号
        result.setMHueBucket(colorHSV.getHueBucket());
        result.setMSaturationBucket(colorHSV.getSaturationBucket());
        result.setMValueBucket(colorHSV.getValueBucket());
        // 返回结果
        return result;
    }

    /**
     * 获取一张图的永久公网访问地址
     *
     * @param savePath 图片在OSS上的位置
     * @return 图片的URL路径
     */
    protected abstract String getPicturePublicURL(String savePath);

    /**
     * 获取图片的主色调信息
     *
     * @param fileName 文件全路径（/path/to/example.jpg）
     * @return 图片主色调
     */
    protected abstract String getPictureMainColor(String fileName);

    /**
     * 获取文件的MD5
     *
     * @param is 文件输入流
     * @return 文件MD5
     */
    protected String calculateMD5(InputStream is) {
        try (is) {
            return MD5.create().digestHex(is);
        } catch (IOException e) {
            log.error("获取文件MD5失败: ", e);
        }
        return null;
    }


    /**
     * 获取图片文件的基础数据
     * <p>如宽高、大小等</p>
     *
     * @param is 文件输入流（使用自定义Stream包装）
     * @return 图片信息
     */
    protected PictureSizeInfo getImageInfo(CountingInputStream is) {
        try (ImageInputStream in = ImageIO.createImageInputStream(is)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    reader.setInput(in);
                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);
                    // 获取图片后缀名（jpg、png、gif...）
                    String format = reader.getFormatName();
                    // 图片长宽比
                    double scale = NumberUtil.round(width * 1.0 / height, 2).doubleValue();
                    return PictureSizeInfo.builder()
                            .width(width)
                            .height(height)
                            .size(is.getSize())
                            .aspectRatio(scale)
                            .format(format)
                            .build();
                } finally {
                    reader.dispose();
                }
            }
        } catch (IOException e) {
            log.error("获取图片尺寸信息失败: ", e);
        }
        return null;
    }

    /**
     * 生成文件名、保存路径等信息
     *
     * @param fileName 文件名称（example）
     * @param savePath 文件保存路径（/path/to/...）
     * @param suffix   文件后缀名(jpg、png、webp...)
     * @return 生成的hex文件路径（形如path/to/20250101_<16位md5摘要>.suffix）
     */
    protected String createFileName(String fileName, String savePath, String suffix) {
        String dateFormat = DateUtil.format(new Date(), "yyyyMMdd");
        String md5 = MD5.create().digestHex(System.currentTimeMillis() + "_" + fileName);
        if (savePath.endsWith("/")) {
            savePath = savePath.substring(0, savePath.length() - 1);
        }
        // 最后的文件名应该是/path/to/20250101_<16位md5摘要>.jpg
        return String.format("%s/%s_%s.%s", savePath, dateFormat, md5, suffix);
    }

    /***
     * 保存信息封装
     */
    @Data
    @ToString
    @AllArgsConstructor
    protected static class UploadInfo {
        /**
         * 文件名链接（https://...）
         */
        private String url;


        /**
         * 文件名（example.jpg）
         */
        private String fileName;

        /**
         * 保存位置（/path/to/example.jpg）
         */
        private String savePath;
    }

    /***
     * 图片信息封装
     */
    @Data
    @Builder
    @ToString
    protected static class PictureSizeInfo {
        /**
         * 图片宽度
         */
        int width;

        /**
         * 图片高度
         */
        int height;

        /**
         * 图片大小（Byte）
         */
        long size;

        /**
         * 图片宽高比
         */
        double aspectRatio;

        /**
         * 文件后缀名
         */
        String format;
    }

    /***
     * 异步上传信息的封装类
     */
    @Data
    @Builder
    @ToString
    protected static class AsyncParameters {

        /**
         * 图片的MD5
         */
        private String picMD5;

        /**
         * 图片尺寸信息
         */
        private PictureSizeInfo sizeInfo;

        /**
         * 缩略图上传信息
         */
        private UploadInfo thumbImageInfo;

        /**
         * 默认图上传信息
         */
        private UploadInfo defaultImageInfo;

        /**
         * 原图上传信息
         */
        private UploadInfo originalImageInfo;

        /**
         * 图片的主要颜色
         */
        private String mainColor;
    }
}
