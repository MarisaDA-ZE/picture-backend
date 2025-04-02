package cloud.marisa.picturebackend.upload.picture;

import cloud.marisa.picturebackend.config.PictureConfig;
import cloud.marisa.picturebackend.exception.BusinessException;
import cloud.marisa.picturebackend.exception.ErrorCode;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.unit.DataSize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * @author MarisaDAZE
 * @description MultipartFile格式的图片上传
 * @date 2025/4/2
 */
@Component
public class PictureMultipartFileUpload extends PictureUploadTemplate {

    @Autowired
    private PictureConfig pictureConfig;

    @Override
    public void validPicture(Object inputSource) {
        if (inputSource == null || ObjectUtils.isEmpty(inputSource)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件为空");
        }
        MultipartFile multipartFile = (MultipartFile) inputSource;
        long maxSize = DataSize.parse(pictureConfig.getImageMaxSize()).toBytes();
        long currentSize = multipartFile.getSize();
        if (currentSize > maxSize) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件最大不能超过" + pictureConfig.getImageMaxSize());
        }
        String fileName = multipartFile.getOriginalFilename();
        if (fileName == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名为空");
        }
        if (fileName.length() > 128) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名过长");
        }
        String suffix = FileUtil.getSuffix(fileName);
        List<String> suffixList = pictureConfig.getImageSuffix();
        // 保证配置文件中读取到的文件后缀始终是没有.分隔符
        boolean b = suffixList.stream()
                .map(sfx -> sfx.startsWith(".") ? sfx.substring(1) : sfx)
                .anyMatch(s -> s.equals(suffix));
        if (!b) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的文件类型 ." + suffix);
        }
    }

    @Override
    public String getOriginalFilename(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        return multipartFile.getOriginalFilename();
    }

    @Override
    public InputStream getPictureStream(Object inputSource) {
        if (inputSource == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        try {
            MultipartFile multipartFile = (MultipartFile) inputSource;
            return multipartFile.getInputStream();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件读取失败");
        }

    }
}
