package cloud.marisa.picturebackend.service;

import cloud.marisa.picturebackend.entity.dao.Notice;
import cloud.marisa.picturebackend.entity.dao.User;
import cloud.marisa.picturebackend.entity.dto.notice.NoticeAddRequest;
import cloud.marisa.picturebackend.entity.dto.notice.NoticeEditRequest;
import cloud.marisa.picturebackend.entity.dto.notice.NoticeQueryRequest;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;


/**
 * @author Marisa
 * @description 针对表【notice(-- 用户消息表，用于记录用户接收到的各类消息 --)】的数据库操作Service
 * @createDate 2025-04-20 17:50:30
 */
public interface INoticeService extends IService<Notice> {

    /**
     * 与指定用户建立连接
     *
     * @param userId 用户ID
     * @return SSE事件对象
     */
    SseEmitter subscribe(Long userId);

    /**
     * 与指定用户断开连接
     *
     * @param userId 用户ID
     */
    void closeSse(Long userId);

    /***
     * 向指定用户推送消息
     * @param userId    用户ID
     * @param notice   消息对象
     */
    void pushMessage(Long userId, Notice notice);

    /**
     * 保存一条消息到数据库
     *
     * @param addRequest 添加条件封装
     * @param loginUser  当前登录用户
     * @return 消息Vo对象
     */
    Notice saveNotice(NoticeAddRequest addRequest, User loginUser);


    /**
     * 根据条件分页查询消息
     * <p>默认排序 未读（时间降序），已读（时间降序）</p>
     *
     * @param queryRequest 查询条件封装
     * @param loginUser    当前登录用户
     * @return 分页消息信息
     */
    Page<Notice> getNoticePage(NoticeQueryRequest queryRequest, User loginUser);

    /**
     * 一键已读某个用户的某一类消息
     *
     * @param noticeType 消息类型（0-系统消息；1-用户消息）
     * @param loginUser  当前登录用户
     * @return 本次操作一共已读了多少条消息
     */
    boolean readBatchByUser(Integer noticeType, User loginUser);

    /**
     * 按ID已读一条消息
     *
     * @param noticeId 消息ID
     * @param loginUser   当前登录用户
     */
    void readById(Long noticeId, User loginUser);

}
