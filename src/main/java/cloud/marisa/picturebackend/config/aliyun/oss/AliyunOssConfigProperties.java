package cloud.marisa.picturebackend.config.aliyun.oss;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author MarisaDAZE
 * @description 阿里云OSS对象存储配置文件
 * @date 2025/4/17
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "mrs.aliyun.oss")
public class AliyunOssConfigProperties {

    /**
     * 访问密钥ID
     */
    private String accessKeyId;

    /**
     * 访问密钥密码
     */
    private String accessKeySecret;

    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * 图片文件夹
     */
    private String picturesFolder;

    /**
     * 接入点
     */
    private String endpoint;

    /**
     * 接入区域
     */
    private String region;

}
