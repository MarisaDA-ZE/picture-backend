package cloud.marisa.picturebackend.entity.dto.api;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 搜索图片请求对象
 */
@Data
@ToString
public class SearchPictureRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * accessKey 访问密钥
     */
    @NotNull(message = "accessKey不能为空")
    private String accessKey;

    /**
     * secretKey 密钥
     */
    @NotNull(message = "secretKey不能为空")
    private String secretKey;

    /**
     * 搜索关键词
     */
    private String keyword;

    /**
     * 标签，JSON格式数组（["tag1", "tag2", ...]）
     */
    private String tag;

    /**
     * 分类名称
     */
    private String category;

    /**
     * 数量，默认为6
     */
    @Min(value = 1, message = "数量最小为1")
    @Max(value = 6, message = "数量最大为6")
    private Integer count = 6;

    /**
     * 图片最小宽度
     */
    private Integer minWidth;

    /**
     * 图片最大宽度
     */
    private Integer maxWidth;

    /**
     * 图片最小高度
     */
    private Integer minHeight;

    /**
     * 图片最大高度
     */
    private Integer maxHeight;
}
