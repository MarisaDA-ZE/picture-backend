package cloud.marisa.picturebackend.entity.dto.picture;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author MarisaDAZE
 * @description 以图搜图的请求参数封装
 * @date 2025/4/5
 */
@Data
@ToString
public class SearchPictureByPictureRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 图片ID
     */
    private Long pictureId;

}
