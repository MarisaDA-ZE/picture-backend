package cloud.marisa.picturebackend.entity.vo;

import java.io.Serializable;
import java.util.Date;

import cloud.marisa.picturebackend.entity.dao.Notice;
import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;
import lombok.ToString;
import org.springframework.beans.BeanUtils;

/**
 * 用户消息的Vo类
 */
@Data
@ToString
public class NoticeVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @JSONField(format = "string")
    private Long id;

    /**
     * 用户ID
     */
    @JSONField(format = "string")
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
     * 消息内容
     */
    private String content;

    /**
     * 发送方ID（0-系统；<ID>-用户）
     */
    private Long senderId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 一些业务层自定义的附加参数
     */
    private String additionalParams;

    /***
     * 将消息对象转换为消息Vo对象
     * @param notice 消息对象
     * @return 消息Vo对象
     */
    public static NoticeVo toVo(Notice notice) {
        NoticeVo noticeVo = new NoticeVo();
        BeanUtils.copyProperties(notice, noticeVo);
        return noticeVo;
    }

    /***
     * 将消息Vo对象转换为消息对象
     * @param vo 消息Vo对象
     * @return 消息对象
     */
    public static Notice toDao(NoticeVo vo) {
        Notice notice = new Notice();
        BeanUtils.copyProperties(vo, notice);
        return notice;
    }
}