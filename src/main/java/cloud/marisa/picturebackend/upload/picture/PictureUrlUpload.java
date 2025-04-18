package cloud.marisa.picturebackend.upload.picture;

import cloud.marisa.picturebackend.config.PictureConfig;
import cloud.marisa.picturebackend.exception.BusinessException;
import cloud.marisa.picturebackend.exception.ErrorCode;
import cloud.marisa.picturebackend.util.MrsPathUtil;
import cn.hutool.core.io.unit.DataSize;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * @author MarisaDAZE
 * @description URL格式的图片上传
 * @date 2025/4/2
 */
@Slf4j
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
    public String getFileName(Object inputSource) {
        String fileURL = (String) inputSource;
        try (HttpResponse response = HttpUtil.createRequest(Method.HEAD, fileURL).execute()) {
            log.info("请求状态：{}", response.isOk());
            // 如果请求失败，则返回URL中的文件名，尽管他并不准确
            if (!response.isOk()) {
                return getFileNameByURL(fileURL);
            }
            // 这里不应该这么做，比如application/x-gzip这个MIME类型对应的文件格式是.gz
            // 正确的做法应该用Map建立映射关系
            String contentType = response.header("Content-Type");
            if (StrUtil.isBlank(contentType)) {
                return getFileNameByURL(fileURL);
            }
            String[] parts = contentType.split(";");
            String mimeType = parts[0].trim(); // 去掉可能的多余参数（如 charset）
            String[] typeParts = mimeType.split("/");
            if (typeParts.length == 2) {
                return typeParts[1];
            }
            return typeParts[0];
        } catch (Exception e) {
            return getFileNameByURL(fileURL);
        }
    }

    @Override
    public InputStream getPictureStream(Object inputSource) {
        String url = (String) inputSource;
        byte[] bytes = HttpUtil.downloadBytes(url);
        return new ByteArrayInputStream(bytes);

    }

    @Override
    public Long getPictureSize(Object inputSource) {
        if (inputSource == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String url = (String) inputSource;
        try (HttpResponse response = HttpUtil.createRequest(Method.HEAD, url).execute()) {
            String contentLength = response.header("Content-Length");
            return StrUtil.isBlank(contentLength) ? null : Long.parseLong(contentLength);
        }
    }

    private String getFileNameByURL(String url) {
        try {
            Path path = Path.of(url);
            Path fileName = path.getFileName();
            log.info("文件名：{}", fileName);
            return fileName.toString();
        } catch (InvalidPathException ex) {
            log.info("无法解析的URL地址: ", ex);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无法解析的URL地址");
        } catch (Exception e) {
            log.error("获取文件名失败: ", e);
            // https://vigen-invi.oss-cn-shanghai.aliyuncs.com/service_dashscope/ImageOutPainting/
            // 2025-04-18/public/8d4fea61-d86d-4ffa-9b47-59819f8c03d2/
            // result-cb015854-c637-47d3-99ff-ae9547dc8ec1.jpg
            // ?OSSAccessKeyId=LTAI5t7aiMEUzu1F2xPMCdFj&Expires=1744986184&Signature=cBKYt0pgQvqxh%2FDtnPW0BG%2B378M%3D
            // https://xxx/yyy/zzz.jpg
            String trimParams = url.contains("?") ? url.substring(0, url.indexOf("?")) : url;
            // zzz.jpg
            return trimParams.substring(trimParams.lastIndexOf("/") + 1);
        }
    }
}
