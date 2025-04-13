package cloud.marisa.picturebackend.entity.dao;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 空间-用户的关联表
 *
 * @TableName space_user
 */
@TableName(value = "space_user")
@Data
@ToString
@EqualsAndHashCode
public class SpaceUser implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 空间ID
     */
    @TableField("space_id")
    private Long spaceId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 空间角色（viewer、editor、admin）
     */
    @TableField("space_role")
    private String spaceRole;

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
}