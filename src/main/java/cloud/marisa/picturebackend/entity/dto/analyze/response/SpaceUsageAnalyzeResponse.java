package cloud.marisa.picturebackend.entity.dto.analyze.response;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author MarisaDAZE
 * @description 空间统计-使用情况DTO
 * @date 2025/4/12
 */
@Data
@Builder
@ToString
public class SpaceUsageAnalyzeResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 已使用大小
     */
    private Long usedSize;

    /**
     * 总大小
     */
    private Long maxSize;

    /**
     * 空间使用比例
     */
    private Double sizeUsageRatio;

    /**
     * 当前图片数量
     */
    private Long usedCount;

    /**
     * 最大图片数量
     */
    private Long maxCount;

    /**
     * 图片数量占比
     */
    private Double countUsageRatio;
}
