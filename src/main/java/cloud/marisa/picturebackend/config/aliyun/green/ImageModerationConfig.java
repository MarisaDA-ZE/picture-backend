package cloud.marisa.picturebackend.config.aliyun.green;

import com.aliyun.green20220302.Client;
import com.aliyun.teaopenapi.models.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author MarisaDAZE
 * @description 图片AI审核配置类
 * @date 2025/4/17
 */
@Configuration
public class ImageModerationConfig {

    @Value("${mrs.aliyun.green.access-key-id}")
    private String ACCESS_KEY_ID;

    @Value("${mrs.aliyun.green.access-key-secret}")
    private String ACCESS_KEY_SECRET;

    @Value("${mrs.aliyun.green.endpoint}")
    private String endpoint;

    @Bean
    public Client getClient() throws Exception {
        Config config = new Config();
        config.setAccessKeyId(ACCESS_KEY_ID);
        config.setAccessKeySecret(ACCESS_KEY_SECRET);
        config.setRegionId("cn-shanghai");
        config.setEndpoint(endpoint);
        //连接时超时时间，单位毫秒（ms）。
        config.setReadTimeout(6000);
        //读取时超时时间，单位毫秒（ms）。
        config.setConnectTimeout(3000);
        return new Client(config);
    }
}
