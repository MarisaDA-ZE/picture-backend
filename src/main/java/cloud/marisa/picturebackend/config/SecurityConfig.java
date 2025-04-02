package cloud.marisa.picturebackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * @author MarisaDAZE
 * @description SecurityConfig.类
 * @date 2025/3/28
 */
@Configuration
public class SecurityConfig {

    /**
     * 配置Spring Security的加密算法为BCrypt
     *
     * @return .
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 当引入spring-boot-starter-security后，
     * Spring Security 会自动应用默认的安全配置，
     * 所有的 HTTP 请求都会被拦截并需要进行身份认证。
     * 下面这个Bean就是用于解除拦截的。
     *
     * @param security .
     * @return .
     * @throws Exception .
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity security) throws Exception {
        security.csrf()
                .disable()
                .authorizeRequests()
                .anyRequest()
                .permitAll();
        return security.build();
    }
}
