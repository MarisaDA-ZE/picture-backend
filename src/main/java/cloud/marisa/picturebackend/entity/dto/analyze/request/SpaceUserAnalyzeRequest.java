package cloud.marisa.picturebackend.entity.dto.analyze.request;

import cloud.marisa.picturebackend.entity.dto.analyze.request.base.SpaceAnalyzeRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @author MarisaDAZE
 * @description 空间统计-用户上传情况DTO
 * @date 2025/4/12
 */
@Data
@ToString
@EqualsAndHashCode(callSuper = true)
public class SpaceUserAnalyzeRequest
        extends SpaceAnalyzeRequest {

    /**
     * 用户ID
     */
    private Long userId;


    /**
     * 时间维度：day / week / month
     */
    private String timeDimension;

}
