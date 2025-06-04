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
 * API调用记录表
 *
 * @TableName api_call_record
 */
@Data
@ToString
@TableName(value = "api_call_record")
public class ApiCallRecord implements Serializable {

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
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 调用类型
     */
    @TableField("call_type")
    private String callType;

    /**
     * 调用时间
     */
    @TableField("call_time")
    private Date callTime;

    /**
     * 调用IP
     */
    @TableField("ip")
    private String ip;

    /**
     * 状态：0-失败 1-成功
     */
    @TableField("status")
    private Integer status;
}