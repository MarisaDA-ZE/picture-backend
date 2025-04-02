package cloud.marisa.picturebackend.controller;

import cloud.marisa.picturebackend.common.MrsResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author MarisaDAZE
 * @description HealthController.类
 * @date 2025/3/25
 */
@RestController
@RequestMapping("/")
public class HealthController {

    /**
     * 健康检查
     *
     * @return .
     */
    @RequestMapping("/health")
    public MrsResult<?> health() {
        return MrsResult.ok();
    }
}
