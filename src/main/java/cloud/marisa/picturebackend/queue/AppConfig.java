package cloud.marisa.picturebackend.queue;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Value("${mrs.queue.capacity:100}")
    private int queueCapacity;

    @Bean
    public int getQueueCapacity() {
        return queueCapacity;
    }
}