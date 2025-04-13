package cloud.marisa.picturebackend.entity.dto.analyze.request.base;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author MarisaDAZE
 * @description 空间统计-基础分析DTO
 * @date 2025/4/12
 */
@Data
@ToString
public class SpaceAnalyzeRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 空间ID
     */
    private Long spaceId;

    /**
     * 是否查询公共图库
     */
    private boolean queryPublic;

    /**
     * 是否进行全空间分析
     */
    private boolean queryAll;
}
