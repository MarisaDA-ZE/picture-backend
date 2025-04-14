package cloud.marisa.picturebackend.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.strategy.SaAnnotationStrategy;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.PostConstruct;

/**
 * @author MarisaDAZE
 * @description SaToken配置类
 * @date 2025/4/14
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册Sa-Token的拦截器，开启注解鉴权功能
        registry.addInterceptor(new SaInterceptor())
                .addPathPatterns("/**");
    }

    @PostConstruct
    public void rewriteSaStrategy() {
        // 重写Said-Token的注解处理器，以实现注解合并
        SaAnnotationStrategy.instance.getAnnotation = AnnotatedElementUtils::getMergedAnnotation;
    }
}
