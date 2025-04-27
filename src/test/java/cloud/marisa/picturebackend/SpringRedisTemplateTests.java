package cloud.marisa.picturebackend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * @author MarisaDAZE
 * @description SpringRedis集成测试
 * @date 2025/4/2
 */
@SpringBootTest
public class SpringRedisTemplateTests {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

//    @Test
    public void redisTest() throws InterruptedException {
        String key = "uid";
        String value = "114514";
        redisTemplate.opsForValue().set(key, value, 60, TimeUnit.SECONDS);
        System.out.printf("设置成功==> {key=%s, value=%s}\n", key, value);
        Thread.sleep(1000);
        String val = redisTemplate.opsForValue().get(key);
        System.out.printf("获取成功==> {key=%s, value=%s}\n", key, val);
    }
}
