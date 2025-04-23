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
 * -- 用户消息表，用于记录用户接收到的各类消息 --
 *
 * @TableName notice
 */
@Data
@ToString
@TableName(value = "notice")
public class Notice implements Serializable {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 消息类型（0-系统消息；1-用户消息）
     */
    @TableField("notice_type")
    private Integer noticeType;

    /**
     * 是否已读（0-未读；1-已读）
     */
    @TableField("is_read")
    private Integer isRead;

    /**
     * 消息内容
     */
    @TableField("content")
    private String content;

    /**
     * 发送方ID（0-系统；<ID>-用户）
     */
    @TableField("sender_id")
    private Long senderId;

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
     * 是否被删除（0-未删除；1-已删除）
     */
    @TableField("is_delete")
    private Integer isDelete;

    /**
     * 一些业务层自定义的附加参数
     */
    @TableField("additional_params")
    private String additionalParams;
}