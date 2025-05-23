package cloud.marisa.picturebackend.entity.dto.file;

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
public class UploadPictureResult implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 图片地址
     */
    private String url;
    /**
     * 拇指图URL地址
     * <p>拇指图，极致压缩</p>
     */
    private String thumbnailUrl;

    /**
     * 原图URL地址
     * <p>原图，不压缩，会根据这个生成指纹</p>
     */
    private String originalUrl;

    /**
     * 图片在文件服务器上保存的路径
     */
    private String savedPath;

    /**
     * 缩略图在文件服务器上的地址
     */
    private String thumbPath;

    /**
     * 原图在文件服务器上的地址
     */
    private String originalPath;

    /**
     * 文件指纹
     */
    private String md5;

    /**
     * 图片名称
     */
    private String picName;

    /**
     * 图片大小
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 图片主要颜色（r,g,b）
     */
    private String picColor;

    /**
     * 主要颜色的色调（0~360°）
     */
    private Float mColorHue;

    /**
     * 主要颜色的饱和度（0~100°）
     */
    private Float mColorSaturation;

    /**
     * 主要颜色的明度（0~100°）
     */
    private Float mColorValue;

    /**
     * 主色调分桶量（每10°一个桶，共36个）
     */
    private Integer mHueBucket;

    /**
     * 饱和度分桶（0~100%）
     */
    private Integer mSaturationBucket;

    /**
     * 明度分桶（0~100%）
     */
    private Integer mValueBucket;

    /**
     * 图片长宽比
     */
    private Double picScale;

    /**
     * 图片格式（jpg、png、...）
     */
    private String picFormat;

}
