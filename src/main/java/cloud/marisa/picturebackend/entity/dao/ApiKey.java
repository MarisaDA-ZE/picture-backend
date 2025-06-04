package cloud.marisa.picturebackend.entity.dao;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;
import lombok.ToString;


/**
 * 图床API密钥表
 * @TableName api_key
 */
@Data
@ToString
@TableName("api_key")
public class ApiKey implements Serializable {
    
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    
    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    /** 用户ID */
    @TableField("user_id")  
    private Long userId;

    /** 访问密钥 */
    @TableField("access_key")
    private String accessKey;

    /** 密钥 */
    @TableField("secret_key")
    private String secretKey;
    
    /** 名称 */
    @TableField("name")
    private String name;
    
    /** 描述 */
    @TableField("description")
    private String description;
    
    /** 状态（0-禁用，1-启用） */
    @TableField("status")
    private Integer status;
    
    /** 每日限制 */
    @TableField("daily_limit")
    private Integer dailyLimit;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /** 是否删除（0-未删除，1-已删除） */
    @TableField("is_deleted")
    @TableLogic
    private Integer isDeleted;
}
