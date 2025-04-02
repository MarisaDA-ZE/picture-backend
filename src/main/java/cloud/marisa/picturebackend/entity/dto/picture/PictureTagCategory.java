package cloud.marisa.picturebackend.entity.dto.picture;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * @author MarisaDAZE
 * @description 图片的tag和分类DTO
 * @date 2025/4/1
 */
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PictureTagCategory {

    /**
     * 标签列表
     */
    private List<String> tagList;

    /**
     * 分类列表
     */
    private List<String> categoryList;
}
