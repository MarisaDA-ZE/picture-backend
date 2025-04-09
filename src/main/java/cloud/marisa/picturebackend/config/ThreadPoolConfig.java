package cloud.marisa.picturebackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author MarisaDAZE
 * @description 自定义线程池配置
 * @date 2025/4/9
 */
@Configuration
public class ThreadPoolConfig {

    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        return new ThreadPoolExecutor(
                8,
                24,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(12),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }
}
