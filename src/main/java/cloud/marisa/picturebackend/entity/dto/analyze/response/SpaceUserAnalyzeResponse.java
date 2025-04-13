package cloud.marisa.picturebackend.entity.dto.analyze.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author MarisaDAZE
 * @description 空间统计-用户信息DTO
 * @date 2025/4/12
 */
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SpaceUserAnalyzeResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 时间区间
     */
    private String period;

    /**
     * 上传数量
     */
    private Long count;

}
