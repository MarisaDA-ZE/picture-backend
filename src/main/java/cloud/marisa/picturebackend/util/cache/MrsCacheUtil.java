package cloud.marisa.picturebackend.util.cache;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.*;

/**
 * @author MarisaDAZE
 * @description 处理缓存的工具类
 * @date 2025/4/17
 */
@Log4j2
@Component
public class MrsCacheUtil {

    /**
     * 缓存管理专用的线程池
     */
    private static final ThreadPoolExecutor cacheDeleteExecutor = new ThreadPoolExecutor(
            4,
            16,
            32,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(16),
            new ThreadPoolExecutor.AbortPolicy()
    );

    /**
     * 延迟删除缓存（支持重试）
     *
     * @param redisTemplate 缓存数据的RedisTemplate实例
     * @param localCache    本地缓存的Caffeine实例
     * @param keys          待删除的缓存键
     * @param maxRetry      最大重试次数
     * @param <CT>          Caffeine的Value类型
     * @param <RT>          RedisTemplate的Value类型
     */
    public static <CT, RT> void delayRemoveCache(
            Cache<String, CT> localCache,
            RedisTemplate<String, RT> redisTemplate,
            List<String> keys, int maxRetry) {
        cacheDeleteExecutor.submit(() -> {
            log.info("开始延迟删除缓存: {}", keys);
            long start = System.currentTimeMillis();
            int retryCount = 0;
            boolean success = false;
            try {
                try {
                    // 首次延迟删除
                    Thread.sleep(1000);
                    success = removeCache(localCache, redisTemplate, keys);
                } catch (InterruptedException e) {
                    log.error("首次延迟删除被中断", e);
                    List<RT> rts = redisTemplate.opsForValue().multiGet(keys);
                    // 此时说明已经删除了，不用重试
                    if (rts == null || rts.isEmpty()) {
                        log.info("原因: 缓存本就不存在, 不再执行重试...");
                        return;
                    }
                    Thread.currentThread().interrupt();
                }
                // 重试逻辑
                while (!success && retryCount < maxRetry) {
                    try {
                        Thread.sleep(5000); // 重试间隔
                        success = removeCache(localCache, redisTemplate, keys);
                        if (success) {
                            log.info("缓存重试删除成功");
                            break;
                        }
                        retryCount++;
                        log.warn("缓存删除失败，已重试{}/{}次", retryCount, maxRetry);
                    } catch (InterruptedException e) {
                        log.error("重试过程中被中断", e);
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                if (!success) {
                    log.error("缓存删除失败，已达最大重试次数: {}", maxRetry);
                }
            } finally {
                log.info("关于删除键 {} 的任务耗时: {}ms", keys, System.currentTimeMillis() - start);
            }
        });
    }

    /**
     * 延迟删除缓存（支持重试）
     *
     * @param redisTemplate 缓存数据的RedisTemplate实例
     * @param keys          待删除的缓存键
     * @param maxRetry      最大重试次数
     * @param <RT>          RedisTemplate的Value类型
     */
    public static <RT> void delayRemoveCache(
            RedisTemplate<String, RT> redisTemplate,
            List<String> keys, int maxRetry) {
        cacheDeleteExecutor.submit(() -> {
            log.info("开始延迟删除缓存: {}", keys);
            long start = System.currentTimeMillis();
            int retryCount = 0;
            Long success = 0L;
            try {
                try {
                    // 首次延迟删除
                    Thread.sleep(1000);
                    success = redisTemplate.delete(keys);
                } catch (InterruptedException e) {
                    log.error("首次延迟删除被失败", e);
                    List<RT> rts = redisTemplate.opsForValue().multiGet(keys);
                    // 此时说明已经删除了，不用重试
                    if (rts == null || rts.isEmpty()) {
                        log.info("原因: 缓存本就不存在, 不再执行重试...");
                        return;
                    }
                    Thread.currentThread().interrupt();
                }
                // 重试逻辑
                while ((success != null && success > 0) && retryCount < maxRetry) {
                    try {
                        Thread.sleep(5000); // 重试间隔
                        success = redisTemplate.delete(keys);
                        if (success != null && success > 0L) {
                            log.info("缓存重试删除成功");
                            break;
                        }
                        retryCount++;
                        log.warn("缓存删除失败，已重试{}/{}次", retryCount, maxRetry);
                    } catch (InterruptedException e) {
                        log.error("重试过程中被中断", e);
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                if (success == null || success == 0L) {
                    log.error("缓存删除失败，已达最大重试次数: {}", maxRetry);
                }
            } finally {
                log.info("关于删除键 {} 的任务耗时: {}ms", keys, System.currentTimeMillis() - start);
            }
        });
    }

    /**
     * 删除本地和Redis缓存
     *
     * @param redisTemplate 缓存数据的RedisTemplate实例
     * @param localCache    本地缓存的Caffeine实例
     * @param keys          待删除的缓存键
     * @param <CT>          Caffeine的Value类型
     * @param <RT>          RedisTemplate的Value类型
     * @return Redis是否至少删除了一个有效键
     */
    public static <CT, RT> boolean removeCache(
            Cache<String, CT> localCache,
            RedisTemplate<String, RT> redisTemplate,
            List<String> keys) {
        try {
            log.info("开始删除缓存: {}", keys);
            // 删除本地缓存
            localCache.invalidateAll(keys);

            List<RT> rts = redisTemplate.opsForValue().multiGet(keys);
            if (rts == null || rts.isEmpty()) {
                log.info("Redis中不存在键: {}", keys);
                return true;
            }
            // 删除Redis缓存
            Long removed = redisTemplate.delete(keys);
            return removed != null && removed > 0;
        } catch (Exception e) {
            log.error("删除缓存异常: {}", e.getMessage());
            return false;
        }
    }
}
