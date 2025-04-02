package cloud.marisa.picturebackend.upload.picture;

import cloud.marisa.picturebackend.entity.dto.file.UploadPictureResult;
import cloud.marisa.picturebackend.exception.BusinessException;
import cloud.marisa.picturebackend.exception.ErrorCode;
import cloud.marisa.picturebackend.util.MinioUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.crypto.digest.MD5;
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

/**
 * @author MarisaDAZE
 * @description 图片上传模板类
 * @date 2025/4/2
 */
@Component
public abstract class PictureUploadTemplate {

    @Autowired
    private MinioUtil minioUtil;

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
        // 处理上传后的文件名
        String originalFilename = getOriginalFilename(inputSource);
        System.out.println("原始文件名: " + originalFilename);
        String uuid = MD5.create().digestHex(System.currentTimeMillis() + "_" + originalFilename);
        String dateFormat = DateUtil.format(new Date(), "yyyyMMdd");
        String suffix = FileUtil.getSuffix(originalFilename);
        // 最后的文件名应该是20250330_<16位md5摘要>.jpg
        String uploadName = String.format("%s_%s.%s", dateFormat, uuid, suffix);
        String uploadPath = (pathPrefix.endsWith("/") ? pathPrefix : pathPrefix + "/") + uploadName;
        System.out.println("保存的文件地址: " + uploadPath);
        try (InputStream is = getPictureStream(inputSource)) {
            // 获取文件大小 大文件不能这么干
            result.setPicSize((long) is.available());
            // 通过临时文件拷贝流
            // 是否还有其它更高效的方法呢？？
            Path tempFile = Files.createTempFile("copy", ".tmp");
            Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
            // 获取图片信息（方法调用后自动关闭流）
            getPictureInfo(Files.newInputStream(tempFile), result);
            // 上传图片到服务器（方法调用后自动关闭流）
            minioUtil.upload(Files.newInputStream(tempFile), uploadPath);
            Files.deleteIfExists(tempFile);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        // 补充其它信息
        result.setPicName(originalFilename);
        result.setSavedPath(uploadPath);
        result.setPicFormat(suffix);
        result.setUrl(minioUtil.getFileUrl(uploadPath));
        return result;
    }


    /**
     * 校验文件是否符合规范
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
     * 从一个不知道是啥的东西身上获取输入流
     *
     * @param inputSource 文件对象/URL
     * @return 输入流
     */
    protected abstract InputStream getPictureStream(Object inputSource);

    /**
     * 获取图片信息
     *
     * @param is     输入流
     * @param result 图片信息对象
     */
    public final void getPictureInfo(InputStream is, UploadPictureResult result) {
        // 获取图片的长宽比信息
        try(is) {
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
}
