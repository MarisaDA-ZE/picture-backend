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
 * API密钥与空间关联表
 *
 * @TableName api_key_space
 */
@Data
@ToString
@TableName(value = "api_key_space")
public class ApiKeySpace implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * API密钥ID
     */
    @TableField("api_key_id")
    private Long apiKeyId;

    /**
     * 空间ID
     */
    @TableField("space_id")
    private Long spaceId;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;
}