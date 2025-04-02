package cloud.marisa.picturebackend.entity.dao;

import cloud.marisa.picturebackend.annotations.MrsFieldName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;
import lombok.ToString;

/**
 * 图片表
 *
 * @TableName picture
 */
@Data
@ToString
@TableName(value = "picture")
public class Picture implements Serializable {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @MrsFieldName(value = "id")
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    @MrsFieldName(value = "userId")
    private Long userId;

    /**
     * 图片URL地址
     */
    private String url;

    /**
     * 图片在文件服务器上保存的地址
     */
    @TableField("saved_path")
    private String savedPath;

    /**
     * 图片名称
     */
    @MrsFieldName(value = "name")
    private String name;

    /**
     * 图片描述
     */
    @MrsFieldName(value = "introduction")
    private String introduction;

    /**
     * 图片分类
     */
    private String category;

    /**
     * 图片标签（JSON数组）
     */
    @TableField("tags")
    private String tags;

    /**
     * 图片大小
     */
    @TableField("pic_size")
    private Long picSize;

    /**
     * 图片宽度
     */
    @TableField("pic_width")
    private Integer picWidth;

    /**
     * 图片高度
     */
    @TableField("pic_height")
    private Integer picHeight;

    /**
     * 图片长宽比
     */
    @TableField("pic_scale")
    private Double picScale;

    /**
     * 图片格式
     */
    @TableField("pic_format")
    private String picFormat;

    /**
     * 图片审核状态（0:待审核，1:已通过，2:已拒绝）
     */
    @TableField("review_status")
    private Integer reviewStatus;

    /**
     * 图片审核信息
     */
    @TableField("review_message")
    private String reviewMessage;

    /**
     * 审核员ID
     */
    @TableField("reviewer_id")
    private Long reviewerId;

    /**
     * 创建时间
     */
    @TableField("create_time")
    @MrsFieldName(value = "createTime")
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private Date updateTime;

    /**
     * 编辑时间
     */
    @TableField("edit_time")
    private Date editTime;

    /**
     * 审核时间
     */
    @TableField("review_time")
    private Date reviewTime;

    /**
     * 逻辑删除（0:未删除，1:已删除）
     */
    @TableField("is_delete")
    private Integer isDelete;
}