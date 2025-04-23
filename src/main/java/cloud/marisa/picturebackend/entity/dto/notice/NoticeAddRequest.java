package cloud.marisa.picturebackend.entity.dto.notice;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * @author MarisaDAZE
 * @description 新增消息记录的DTO封装
 * @date 2025/4/20
 */
@Data
@ToString
public class NoticeAddRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 接收方用户ID
     */
    private Long userId;

    /**
     * 消息类型（0-系统消息；1-用户消息）
     */
    private Integer noticeType;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 发送方ID（0-系统；<ID>-用户）
     */
    private Long senderId;
}
