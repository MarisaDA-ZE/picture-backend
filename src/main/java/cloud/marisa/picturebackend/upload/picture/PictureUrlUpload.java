package cloud.marisa.picturebackend.upload.picture;

import cloud.marisa.picturebackend.config.PictureConfig;
import cloud.marisa.picturebackend.exception.BusinessException;
import cloud.marisa.picturebackend.exception.ErrorCode;
import cloud.marisa.picturebackend.util.MrsPathUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.unit.DataSize;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author MarisaDAZE
 * @description URL格式的图片上传
 * @date 2025/4/2
 */
@Component
public class PictureUrlUpload extends PictureUploadTemplate {
    @Autowired
    private PictureConfig pictureConfig;

    @Override
    protected void validPicture(Object inputSource) {
        String url = (String) inputSource;
        if (StrUtil.isBlank(url)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件地址不能为空");
        }
        if (!MrsPathUtil.isWebURL(url)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持http(s)协议的链接");
        }
        try (HttpResponse response = HttpUtil.createRequest(Method.HEAD, url).execute()) {
            // 有些URL地址可能不支持Head请求访问，所以这里即使请求没成功也不报错
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                return;
            }
            // 校验文件类型
            String contentType = response.header("Content-Type");
            if (StrUtil.isNotBlank(contentType)) {
                boolean contains = pictureConfig.getUrlImageType().contains(contentType);
                if (!contains) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的文件类型 " + contentType);
                }
            }
            // 校验文件大小
            String contentLength = response.header("Content-Length");
            if (StrUtil.isNotBlank(contentLength)) {
                try {
                    long fileSize = Long.parseLong(contentLength);
                    String maxSizeStr = pictureConfig.getImageMaxSize();
                    long maxSize = DataSize.parse(maxSizeStr).toBytes();
                    if (fileSize > maxSize) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件最大不能超过 " + maxSizeStr);
                    }
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小格式错误");
                }
            }
        }
    }

    @Override
    protected String getOriginalFilename(Object inputSource) {
        String fileURL = (String) inputSource;
        try (HttpResponse response = HttpUtil.createRequest(Method.HEAD, fileURL).execute()) {
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                return FileUtil.getName(fileURL);
            }
            // 这里不应该这么做，比如application/x-gzip这个MIME类型对应的文件格式是.gz
            // 正确的做法应该用Map建立映射关系
            String contentType = response.header("Content-Type");
            if (StrUtil.isBlank(contentType)) {
                return FileUtil.getName(fileURL);
            }
            String[] parts = contentType.split(";");
            String mimeType = parts[0].trim(); // 去掉可能的多余参数（如 charset）
            String[] typeParts = mimeType.split("/");
            if (typeParts.length == 2) {
                String subType = typeParts[1];  // 子类型
                return "." + subType;
            }
            return "." + typeParts[0];
        } catch (Exception e) {
            return FileUtil.getName(fileURL);
        }
    }

    @Override
    protected InputStream getPictureStream(Object inputSource) {
        String url = (String) inputSource;
        try {
            Path tempFile = Files.createTempFile("download", ".tmp");
            HttpUtil.downloadFile(url, tempFile.toFile());
            return Files.newInputStream(tempFile);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "下载文件失败");
        }
    }

    @Override
    protected Long getPictureSize(Object inputSource) {
        if (inputSource == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String url = (String) inputSource;
        try (HttpResponse response = HttpUtil.createRequest(Method.HEAD, url).execute()) {
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                return null;
            }
            String contentLength = response.header("Content-Length");
            return StrUtil.isBlank(contentLength) ? null : Long.parseLong(contentLength);
        }
    }
}
