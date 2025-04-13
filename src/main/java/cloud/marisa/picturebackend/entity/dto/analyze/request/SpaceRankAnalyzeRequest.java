package cloud.marisa.picturebackend.entity.dto.analyze.request;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author MarisaDAZE
 * @description 空间统计-空间使用排名DTO
 * @date 2025/4/12
 */
@Data
@ToString
public class SpaceRankAnalyzeRequest
        implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 获取前n个排名的空间
     */
    private Integer topN = 10;
}
