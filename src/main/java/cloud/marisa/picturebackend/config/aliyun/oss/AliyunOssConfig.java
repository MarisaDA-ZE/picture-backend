package cloud.marisa.picturebackend.config.aliyun.oss;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.SignVersion;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author MarisaDAZE
 * @description 阿里云OSS对象存储配置类
 * @date 2025/4/17
 */
@Data
@Configuration
@RequiredArgsConstructor
public class AliyunOssConfig {

    /**
     * 阿里云对象存储配置信息
     */
    private final AliyunOssConfigProperties properties;

    @Bean
    public OSS getOssClient() {
        CredentialsProvider credentialsProvider = new DefaultCredentialProvider(
                properties.getAccessKeyId(),
                properties.getAccessKeySecret());
        // 创建 OSSClient 实例
        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        // 显式声明使用 V4 签名算法
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);
        return OSSClientBuilder.create()
                .endpoint(properties.getEndpoint())
                .credentialsProvider(credentialsProvider)
                .region(properties.getRegion())
                .build();
    }
}
