package cloud.marisa.picturebackend.config;

import lombok.extern.log4j.Log4j2;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.session.DefaultCookieSerializerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author MarisaDAZE
 * @description Redisson配置类
 * @date 2025/4/2
 */
@Log4j2
@Configuration
public class RedissonConfig {

    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private Integer port;

    @Value("${spring.redis.database}")
    private Integer database;

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
        log.info("Redisson 地址 {}", url);
        log.info("Redisson 密码 {}", password);
        log.info("Redisson DB {}", database);
        Config config = new Config();
        config.useSingleServer()
                .setAddress(url)
                .setPassword(password)
                .setDatabase(database);
        return Redisson.create(config);
    }

    @Bean
    DefaultCookieSerializerCustomizer cookieSerializerCustomizer() {
        return cookieSerializer -> {
            cookieSerializer.setSameSite("None"); // 设置cookie的SameSite属性为None，否则跨域set-cookie会被chrome浏览器阻拦
            cookieSerializer.setUseSecureCookie(true); // sameSite为None时，useSecureCookie必须为true
        };
    }
}
