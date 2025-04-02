package cloud.marisa.picturebackend.entity.dto.picture;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author MarisaDAZE
 * @description 图片批量上传的DTO对象
 * @date 2025/3/30
 */
@Data
@ToString
public class PictureUploadBatchRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 抓取数量
     */
    private Integer count = 10;

    /**
     * 搜索关键词
     */
    private String searchText;

    /**
     * 名称前缀
     */
    private String namePrefix;
}
