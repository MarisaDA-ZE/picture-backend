package cloud.marisa.picturebackend.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
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
     * 图片压缩率[0-1], 1表示不压缩
     */
    private Float compressRate;

    /**
     * 压缩后图片的类型
     */
    private String compressImageType;

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

    public void setCompressRate(Float compressRate) {
        this.compressRate = compressRate;
    }

    public void setCompressImageType(String compressImageType) {
        this.compressImageType = compressImageType;
    }

    public void setImageSuffix(List<String> imageSuffix) {
        this.imageSuffix = imageSuffix;
    }

    public void setUrlImageType(List<String> urlImageType) {
        this.urlImageType = urlImageType;
    }
}
