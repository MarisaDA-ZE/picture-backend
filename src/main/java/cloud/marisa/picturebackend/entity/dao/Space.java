package cloud.marisa.picturebackend.entity.dao;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;
import lombok.ToString;

/**
 * 空间表
 *
 * @TableName space
 */
@Data
@ToString
@TableName(value = "space")
public class Space implements Serializable {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 所属用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 空间名称
     */
    @TableField("space_name")
    private String spaceName;

    /**
     * 空间等级（0-普通版、1-专业版、2-旗舰版）
     */
    @TableField("space_level")
    private Integer spaceLevel;

    /**
     * 最大存储空间（Byte）
     */
    @TableField("max_size")
    private Long maxSize;

    /**
     * 最大存储数量（张）
     */
    @TableField("max_count")
    private Integer maxCount;

    /**
     * 已使用的存储空间（Byte）
     */
    @TableField("total_size")
    private Long totalSize;

    /**
     * 已使用的存储数量（张）
     */
    @TableField("total_count")
    private Integer totalCount;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private Date updateTime;

    /**
     * 修改时间
     */
    @TableField("edit_time")
    private Date editTime;

    /**
     * 是否删除（0-未删除、1-已删除）
     */
    @TableField("is_delete")
    private Integer isDelete;

}