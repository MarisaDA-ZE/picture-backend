package cloud.marisa.picturebackend.entity.dto.picture;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * @author MarisaDAZE
 * @description 图片批量修改参数的封装
 * @date 2025/4/9
 */
@Data
@ToString
public class PictureEditBatchRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 要修改的图片ID列表
     */
    private List<Long> pictureIdList;

    /**
     * 命名规则
     */
    private String nameRule;

    /**
     * 所属空间ID
     */
    private Long spaceId;

    /**
     * 图片分类
     */
    private String category;

    /**
     * 图片标签
     */
    private List<String> tags;
}
