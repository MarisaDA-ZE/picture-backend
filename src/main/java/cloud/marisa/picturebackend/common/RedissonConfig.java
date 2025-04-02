package cloud.marisa.picturebackend.common;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author MarisaDAZE
 * @description Redisson配置类
 * @date 2025/4/2
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private Integer port;

    @Value("${spring.redis.password}")
    private String password;

    /**
     * Redisson客户端的Bean
     *
     * @return Redisson客户端
     */
    @Bean
    public RedissonClient redissonClient() {
        String url = "redis://" + host + ":" + port;
        Config config = new Config();
        config.useSingleServer()
                .setAddress(url)
                .setPassword(password)
                .setDatabase(2);
        return Redisson.create(config);
    }
}
