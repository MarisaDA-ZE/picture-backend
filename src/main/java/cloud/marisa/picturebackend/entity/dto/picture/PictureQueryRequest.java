package cloud.marisa.picturebackend.entity.dto.picture;

import cloud.marisa.picturebackend.entity.dto.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @author MarisaDAZE
 * @description 查询图片的DTO对象
 * @date 2025/3/31
 */

@Data
@ToString
@EqualsAndHashCode(callSuper = true)
public class PictureQueryRequest extends PageRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 图片ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 是否只查询 spaceId 为 null 的数据
     */
    private boolean nullSpaceId;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 搜索关键词（同时搜索名称、简介等）
     */
    private String searchText;

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
     * 宽高匹配方式
     * <p>大于、小于、等于</p>
     */
    private String whType;

    /**
     * 图片长宽比
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;

    /**
     * 审核状态（0:待审核，1:已通过，2:已拒绝）
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;

    /**
     * 审核员ID
     */
    private Long reviewerId;

    /**
     * 开始编辑时间
     */
    private Date startEditTime;

    /**
     * 结束编辑时间
     */
    private Date endEditTime;

}
