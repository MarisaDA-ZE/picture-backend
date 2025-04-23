package cloud.marisa.picturebackend.controller.notice;

import cloud.marisa.picturebackend.annotations.AuthCheck;
import cloud.marisa.picturebackend.common.MrsResult;
import cloud.marisa.picturebackend.entity.dao.Notice;
import cloud.marisa.picturebackend.entity.dao.User;
import cloud.marisa.picturebackend.entity.dto.common.DeleteRequest;
import cloud.marisa.picturebackend.entity.dto.notice.NoticeEditRequest;
import cloud.marisa.picturebackend.entity.dto.notice.NoticeQueryRequest;
import cloud.marisa.picturebackend.entity.vo.NoticeVo;
import cloud.marisa.picturebackend.enums.MrsUserRole;
import cloud.marisa.picturebackend.exception.BusinessException;
import cloud.marisa.picturebackend.exception.ErrorCode;
import cloud.marisa.picturebackend.service.INoticeService;
import cloud.marisa.picturebackend.service.IUserService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author MarisaDAZE
 * @description 消息服务控制层
 * @date 2025/4/20
 */
@Log4j2
@RestController
@RequestMapping("/notice")
@RequiredArgsConstructor
public class NoticeController {

    /**
     * 用户服务
     */
    private final IUserService userService;

    /**
     * 通知服务
     */
    private final INoticeService noticeService;

    /**
     * 分页获取消息列表
     *
     * @param queryRequest   查询参数的DTO封装
     * @param servletRequest HttpServlet请求对象
     * @return .
     */
    @AuthCheck(mustRole = MrsUserRole.USER)
    @PostMapping(path = "/list/vo")
    public MrsResult<?> getNoticePage(
            @RequestBody NoticeQueryRequest queryRequest,
            HttpServletRequest servletRequest) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(servletRequest);
        Page<Notice> noticePage = noticeService.getNoticePage(queryRequest, loginUser);
        Page<NoticeVo> voPage = new Page<>(queryRequest.getCurrent(), queryRequest.getPageSize(), noticePage.getTotal());
        voPage.setSize(noticePage.getSize());
        List<NoticeVo> voList = noticePage.getRecords().stream()
                .map(NoticeVo::toVo)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return MrsResult.ok(voPage);
    }

    /**
     * 根据消息ID，读一条消息
     *
     * @param editRequest    阅读消息的请求对象
     * @param servletRequest HttpServlet请求对象
     * @return .
     */
    @AuthCheck(mustRole = MrsUserRole.USER)
    @PostMapping(path = "/readById")
    public MrsResult<?> readById(
            @RequestBody NoticeEditRequest editRequest,
            HttpServletRequest servletRequest) {
        if (editRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long noticeId = editRequest.getId();
        if (noticeId == null || noticeId < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(servletRequest);
        noticeService.readById(noticeId, loginUser);
        return MrsResult.ok();
    }

    /**
     * 根据ID，读一条消息
     *
     * @param editRequest    编辑参数的DTO封装
     * @param servletRequest HttpServlet请求对象
     * @return 已读结果
     */
    @AuthCheck(mustRole = MrsUserRole.USER)
    @PostMapping(path = "/readBatch")
    public MrsResult<?> readBatchByUser(
            @RequestBody NoticeEditRequest editRequest,
            HttpServletRequest servletRequest) {
        if (editRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(servletRequest);
        Integer noticeType = editRequest.getNoticeType();
        boolean isRead = noticeService.readBatchByUser(noticeType, loginUser);
        if (!isRead) return MrsResult.ok("当前没有未读消息");
        return MrsResult.ok();
    }

    /**
     * 根据ID
     *
     * @param deleteRequest  删除参数封装
     * @param servletRequest HttpServlet请求对象
     * @return .
     */
    @AuthCheck(mustRole = MrsUserRole.USER)
    @PostMapping(path = "/remove")
    public MrsResult<?> removeById(
            @RequestBody DeleteRequest deleteRequest,
            HttpServletRequest servletRequest) {
        if (deleteRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long noticeId = deleteRequest.getId();
        if (noticeId == null || noticeId < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(servletRequest);
        Notice dbNotice = noticeService.getById(noticeId);
        if (dbNotice == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "消息不存在");
        }
        if (!dbNotice.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.AUTHORIZATION_ERROR);
        }
        boolean removed = noticeService.removeById(noticeId);
        if (!removed) {
            return MrsResult.failed();
        }
        return MrsResult.ok();
    }

    /**
     * 测试推送消息
     *
     * @param userId  用户ID
     * @param message 消息内容
     */
    @AuthCheck(mustRole = MrsUserRole.ADMIN)
    @PostMapping("/pushTest/{userId}")
    public void pushTest(
            @PathVariable Long userId,
            String message) {
        log.info("message {}", message);
        log.info("notice==>尝试推送消息给用户 {}", userId);
        Notice notice = new Notice();
        notice.setId(1001L);
        notice.setIsRead(0);    // 未读
        notice.setNoticeType(0);    // 系统消息
        notice.setContent(message);
        notice.setCreateTime(new Date());
        // noticeService.save(notice);
        noticeService.pushMessage(userId, notice);
    }
}
