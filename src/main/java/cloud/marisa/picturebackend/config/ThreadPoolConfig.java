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

    /**
     * 这个线程池用于处理批量图片编辑的异步任务
     */
    @Bean("editBatchPoolExecutor")
    public ThreadPoolExecutor editBatchPoolExecutor() {
        return new ThreadPoolExecutor(
                8,
                24,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(12),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    /**
     * 这个线程池用于处理AI图片审核的异步任务
     *
     * @return 一个线程池
     */
    @Bean("asyncReviewThreadPool")
    public ThreadPoolExecutor asyncReviewThreadPool() {
        return new ThreadPoolExecutor(
                6,
                12,
                64,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(16),
                new ThreadPoolExecutor.AbortPolicy());
    }


    /**
     * 这个线程池用于异步处理图片上传任务
     * <p>主要是获取图片相关信息，并更新到数据库</p>
     */
    @Bean("pictureUploadPoolExecutor")
    public ThreadPoolExecutor pictureUploadPoolExecutor() {
        return new ThreadPoolExecutor(
                4,
                16,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(16),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }
}
