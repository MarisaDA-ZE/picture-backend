package cloud.marisa.picturebackend.entity.dto.notice;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author MarisaDAZE
 * @description 修改消息内容参数的DTO封装
 * @date 2025/4/20
 */
@Data
@ToString
@NoArgsConstructor
public class NoticeEditRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 消息类型（0-系统消息；1-用户消息）
     */
    private Integer noticeType;

    /**
     * 是否已读（0-未读；1-已读）
     */
    private Integer isRead;
}
