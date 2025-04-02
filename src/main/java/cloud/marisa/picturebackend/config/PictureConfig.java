package cloud.marisa.picturebackend.config;

import cn.hutool.core.io.unit.DataSize;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author MarisaDAZE
 * @description PictureConfig.类
 * @date 2025/3/30
 */
@Getter
@Setter
@ToString
@Component
@ConfigurationProperties(prefix = "mrs.file.picture")
public class PictureConfig {

    /**
     * 最大允许文件大小
     */
    private String imageMaxSize;

    /**
     * 允许的文件后缀（.jpg .png ...）
     */
    private List<String> imageSuffix;

    /**
     * 从网络获取时允许的MIME类型（如application/x-gzip）
     */
    private List<String> urlImageType;

    public void setImageMaxSize(String imageMaxSize) {
        this.imageMaxSize = imageMaxSize;
    }

    public void setImageSuffix(List<String> imageSuffix) {
        this.imageSuffix = imageSuffix;
    }

    public void setUrlImageType(List<String> urlImageType) {
        this.urlImageType = urlImageType;
    }
}
