package cloud.marisa.picturebackend.entity.dto.picture;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * @author MarisaDAZE
 * @description 更新图片的DTO对象
 * @date 2025/3/31
 */
@Data
@ToString
public class PictureUpdateRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 图片ID
     */
    private Long id;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 空间ID
     */
    private Long spaceId;

    /**
     * 图片描述
     */
    private String introduction;

    /**
     * 图片分类
     */
    private String category;

    /**
     * 图片标签
     */
    private List<String> tags;
}
