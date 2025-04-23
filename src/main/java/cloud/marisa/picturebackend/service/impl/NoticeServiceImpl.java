package cloud.marisa.picturebackend.service.impl;


import cloud.marisa.picturebackend.entity.dao.Notice;
import cloud.marisa.picturebackend.entity.dao.User;
import cloud.marisa.picturebackend.entity.dto.notice.NoticeAddRequest;
import cloud.marisa.picturebackend.entity.dto.notice.NoticeQueryRequest;
import cloud.marisa.picturebackend.enums.notice.MrsNoticeRead;
import cloud.marisa.picturebackend.enums.notice.MrsNoticeType;
import cloud.marisa.picturebackend.exception.BusinessException;
import cloud.marisa.picturebackend.exception.ErrorCode;
import cloud.marisa.picturebackend.exception.ThrowUtils;
import cloud.marisa.picturebackend.mapper.NoticeMapper;
import cloud.marisa.picturebackend.service.INoticeService;
import cloud.marisa.picturebackend.service.IUserService;
import cloud.marisa.picturebackend.util.EnumUtil;
import cloud.marisa.picturebackend.util.SseEmitterUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collections;
import java.util.Map;


/**
 * @author Marisa
 * @description 针对表【notice(-- 用户消息表，用于记录用户接收到的各类消息 --)】的数据库操作Service实现
 * @createDate 2025-04-20 17:50:30
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice>
        implements INoticeService {

    /**
     * 用户服务
     */
    private final IUserService userService;

    /**
     * SSE工具类
     */
    private final SseEmitterUtil sseUtil;

    @Override
    public SseEmitter subscribe(Long userId) {
        SseEmitter subscribe = sseUtil.subscribe(userId);
        // 检查是否有堆积的消息
        sseUtil.checkReSend(userId);
        return subscribe;
    }

    @Override
    public void closeSse(Long uid) {
        sseUtil.closeSse(uid);
    }

    @Override
    public void pushMessage(Long uid, Notice notice) {
        sseUtil.pushMessage(uid, notice);
    }

    @Override
    public Notice saveNotice(NoticeAddRequest addRequest, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(addRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 检查用户是否存在
        Long userId = addRequest.getUserId();
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR);
        boolean exists = userService
                .lambdaQuery()
                .eq(User::getId, userId)
                .exists();
        if (!exists) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        // 入库并返回结果
        Notice notice = new Notice();
        BeanUtils.copyProperties(addRequest, notice);
        boolean saved = this.save(notice);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "消息保存失败");
        return notice;
    }

    @Override
    public Page<Notice> getNoticePage(
            NoticeQueryRequest queryRequest,
            User loginUser) {
        Page<Notice> requestPage = new Page<>(queryRequest.getCurrent(), queryRequest.getPageSize());
        log.info("查询参数：{}", queryRequest);
        Long userId = loginUser.getId();
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        queryRequest.setUserId(userId);
        LambdaQueryWrapper<Notice> queryWrapper = getQueryWrapper(queryRequest);
        return this.page(requestPage, queryWrapper);
    }

    @Override
    public boolean readBatchByUser(Integer nt, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        LambdaUpdateWrapper<Notice> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper
                .eq(Notice::getUserId, loginUser.getId())
                .eq(Notice::getIsRead, MrsNoticeRead.UNREAD.getValue())
                .set(Notice::getIsRead, MrsNoticeRead.READ.getValue());
        // noticeType为空时，要已读掉该用户的所有未读消息
        MrsNoticeType noticeType = EnumUtil.fromValue(nt, MrsNoticeType.class);
        if (nt != null) {
            ThrowUtils.throwIf(noticeType == null, ErrorCode.PARAMS_ERROR, "未知的通知类型");
            updateWrapper.eq(Notice::getNoticeType, noticeType.getValue());
        }
        boolean updated = this.update(updateWrapper);
        // 同步移除掉redis中的重试信息
        sseUtil.readListByType(noticeType, loginUser.getId());
        return updated;
    }

    @Override
    public void readById(Long noticeId, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(noticeId == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        Notice notice = this.getById(noticeId);
        ThrowUtils.throwIf(notice == null, ErrorCode.NOT_FOUND, "消息不存在");
        Long dbUserId = notice.getUserId();
        if (!loginUser.getId().equals(dbUserId)) {
            throw new BusinessException(ErrorCode.AUTHORIZATION_ERROR, "暂无操作权限");
        }
        // 参数中的阅读状态
        MrsNoticeRead isRead = MrsNoticeRead.READ;
        // 数据库中的阅读状态
        Integer dbir = notice.getIsRead();
        MrsNoticeRead dbIsRead = EnumUtil.fromValue(dbir, MrsNoticeRead.class);
        ThrowUtils.throwIf(isRead == dbIsRead, ErrorCode.PARAMS_ERROR, "请勿重复已读");
        // 参数正常，开始变更
        LambdaUpdateWrapper<Notice> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper
                .eq(Notice::getId, noticeId)
                .set(Notice::getIsRead, isRead.getValue());
        boolean updated = this.update(updateWrapper);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "消息状态更新失败");
        // 同步更新redis中的重试列表
        sseUtil.readListIfRetry(Collections.singletonList(notice.getId()), loginUser.getId());
    }

    private LambdaQueryWrapper<Notice> getQueryWrapper(NoticeQueryRequest queryRequest) {
        LambdaQueryWrapper<Notice> queryWrapper = new LambdaQueryWrapper<>();
        // 主键ID
        Long noticeId = queryRequest.getId();
        if (noticeId != null && noticeId > 0) {
            queryWrapper.eq(Notice::getId, noticeId);
        }
        // 用户ID
        Long userId = queryRequest.getUserId();
        if (userId != null && userId > 0) {
            queryWrapper.eq(Notice::getUserId, userId);
        }
        // 消息类型（0-系统消息；1-用户消息）
        Integer nt = queryRequest.getNoticeType();
        if (nt != null) {
            MrsNoticeType noticeType = EnumUtil.fromValue(nt, MrsNoticeType.class);
            ThrowUtils.throwIf(noticeType == null, ErrorCode.PARAMS_ERROR, "未知的通知类型");
            queryWrapper.eq(Notice::getNoticeType, noticeType.getValue());
        }
        // 消息状态（0-未读；1-已读）
        Integer ir = queryRequest.getIsRead();
        if (ir != null) {
            MrsNoticeRead isRead = EnumUtil.fromValue(ir, MrsNoticeRead.class);
            ThrowUtils.throwIf(isRead == null, ErrorCode.PARAMS_ERROR, "未知的消息状态");
            queryWrapper.eq(Notice::getIsRead, isRead.getValue());
        }
        // 发送方ID
        Long senderId = queryRequest.getSenderId();
        if (senderId != null) {
            queryWrapper.eq(Notice::getSenderId, senderId);
        }
        // 优先按未读消息创建时间倒叙排序，然后按已读消息创建时间倒叙排序
        queryWrapper
                .orderByAsc(Notice::getIsRead)  // 未读（0）在前，已读（1）在后
                .orderByDesc(Notice::getCreateTime); // 创建时间倒序
        return queryWrapper;
    }
}




