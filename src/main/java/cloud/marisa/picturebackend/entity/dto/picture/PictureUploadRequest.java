package cloud.marisa.picturebackend.entity.dto.picture;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author MarisaDAZE
 * @description 图片上传的DTO对象
 * @date 2025/3/30
 */
@Data
@ToString
public class PictureUploadRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 图片ID
     */
    private Long id;

    /**
     * 空间ID
     */
    private Long spaceId;

    /**
     * 文件地址
     */
    private String fileUrl;

    /**
     * 图片名称
     */
    private String picName;
}
