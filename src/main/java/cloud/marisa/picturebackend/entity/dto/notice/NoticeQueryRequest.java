package cloud.marisa.picturebackend.entity.dto.notice;

import cloud.marisa.picturebackend.entity.dto.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * @author MarisaDAZE
 * @description 查询消息参数的DTO封装
 * @date 2025/4/20
 */

@Data
@ToString
@EqualsAndHashCode(callSuper = true)
public class NoticeQueryRequest extends PageRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 消息类型（0-系统消息；1-用户消息）
     */
    private Integer noticeType;

    /**
     * 是否已读（0-未读；1-已读）
     */
    private Integer isRead;

    /**
     * 发送方ID（0-系统；<ID>-用户）
     */
    private Long senderId;
}
