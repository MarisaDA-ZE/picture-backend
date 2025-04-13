package cloud.marisa.picturebackend.entity.dto.analyze.response;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author MarisaDAZE
 * @description 空间统计-类别分析DTO
 * @date 2025/4/12
 */
@Data
@Builder
@ToString
public class SpaceCategoryAnalyzeResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 图片分类
     */
    private String category;

    /**
     * 图片数量
     */
    private Long count;

    /**
     * 分类图片总大小
     */
    private Long totalSize;
}
