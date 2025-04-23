package cloud.marisa.picturebackend.util;

import cloud.marisa.picturebackend.entity.dao.Notice;
import cloud.marisa.picturebackend.entity.vo.NoticeVo;
import cloud.marisa.picturebackend.enums.notice.MrsNoticeRead;
import cloud.marisa.picturebackend.enums.notice.MrsNoticeType;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author MarisaDAZE
 * @description Sse事件工具类
 * @date 2025/4/21
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class SseEmitterUtil {

    /**
     * 消息持久化（userId - Map＜noticeId, Notice＞）
     */

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 存储用户ID与SseEmitter的映射
     */
    private static final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 异步任务的映射
     */
    private static final Map<Long, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();

    /**
     * SSE线程池
     */
    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(6);

    /**
     * SSE超时时间（毫秒）
     */
    private static final Long SSE_TIME_OUT = 15_000L;

    /**
     * SSE心跳间隔（毫秒）
     */
    private static final Long HEARTBEAT_PERIOD = 3_000L;

    /**
     * 获取和SSE任务相关的RedisKey
     *
     * @param userId 用户ID
     * @return redisKey
     */
    private String getNoticeKey(Long userId) {
        return String.format("notice:retry:%d", userId);
    }

    /**
     * 建立SSE连接
     *
     * @param userId 用户ID
     * @return SSE实例
     */
    public SseEmitter subscribe(Long userId) {
        // 先尝试释放历史资源
        closeSse(userId);
        // 创建实例（15秒后销毁）
        SseEmitter emitter = new SseEmitter(SSE_TIME_OUT);
        // 发送心跳
        initHeartbeat(userId, emitter);
        // 添加到Map映射
        emitters.put(userId, emitter);
        return emitter;
    }

    /***
     * 使用定时任务发送心跳
     * @param userId   用户ID
     * @param emitter   SSE实例
     */
    public void initHeartbeat(Long userId, SseEmitter emitter) {
        AtomicInteger index = new AtomicInteger(0);
        // (15000 / 3000) => 5
        long count = SSE_TIME_OUT / HEARTBEAT_PERIOD;
        ScheduledFuture<?> future = executor.scheduleAtFixedRate(() -> {
            try {
                index.getAndIncrement();
                int ix = index.get();
                // 连接关闭
                if ((count - ix) < 0) {
                    log.info("SSE连接关闭...");
                    closeSse(userId);
                } else {
                    log.info("SSE心跳发送中... {}", ix);
                    emitter.send(SseEmitter
                            .event()
                            .data("ping")
                            .reconnectTime(HEARTBEAT_PERIOD)
                    );
                }
            } catch (Exception e) {
                emitter.complete();
            }
        }, 0, HEARTBEAT_PERIOD, TimeUnit.MILLISECONDS);
        // 加入任务列表
        futures.put(userId, future);
        // 监听连接关闭事件，取消定时任务
        emitter.onCompletion(() -> {
            log.info("任务结束，释放线程资源");
            future.cancel(true);
        });
        emitter.onTimeout(() -> {
            log.info("任务超时，释放线程资源");
            future.cancel(true);
        });
    }

    /**
     * 检查并发送所有需要重发的消息
     *
     * @param userId 用户ID
     */
    public void checkReSend(Long userId) {
        List<Notice> retryNotices = getRetryNotices(userId);
        // 消息列表为空，说明没有消息要重发
        if (retryNotices.isEmpty()) return;
        SseEmitter emitter = emitters.get(userId);
        log.info("即将向用户 {} 推送{}条消息", userId, retryNotices.size());
        log.info("对应的SSE实例 {}", emitter);
        send(userId, retryNotices, emitter);
    }

    /**
     * 已读一些消息，如果它将要准备重试，则将它移除掉
     *
     * @param noticeIds 已读消息的ID列表
     * @param userId    用户ID
     */
    public void readListIfRetry(List<Long> noticeIds, Long userId) {
        List<Notice> retryNotices = getRetryNotices(userId);
        if (retryNotices.isEmpty()) {
            log.info("用户 {} 没有待重发的消息", userId);
            return;
        }
        // 筛选需要重发的消息
        List<Notice> retryList = retryNotices.stream()
                .map(retryNotice -> {
                    // 只返回重试列表 和 已读消息列表 中都有的消息
                    Long retryNoticeId = retryNotice.getId();
                    // 已读消息在重试消息列表中，则这条消息就不用重发
                    if (noticeIds.contains(retryNoticeId)) {
                        return null;
                    }
                    // 反之，这条消息还是需要推送给用户
                    return retryNotice;
                })
                .filter(ObjUtil::isNotNull)
                .collect(Collectors.toList());
        // 更新缓存
        String json = JSONUtil.toJsonStr(retryList);
        redisTemplate.opsForValue().set(getNoticeKey(userId), json);
    }

    /**
     * 按类型移除已读的消息
     *
     * @param noticeType 已读消息的ID列表
     * @param userId     用户ID
     */
    public void readListByType(MrsNoticeType noticeType, Long userId) {
        List<Notice> retryNotices = getRetryNotices(userId);
        if (retryNotices.isEmpty()) {
            log.info("用户 {} 没有待重发的消息", userId);
            return;
        }
        // 筛选需要重发的消息
        List<Notice> retryList = retryNotices.stream()
                .map(retryNotice -> {
                    Integer retryNT = retryNotice.getNoticeType();
                    Integer isRead = retryNotice.getIsRead();
                    // 类型为空，则是全类型匹配，只用关心状态
                    if (noticeType == null) {
                        if (isRead.equals(MrsNoticeRead.UNREAD.getValue())) {
                            return null;
                        }
                    } else {
                        // 消息类型是提供的消息，且状态为未读，就从列表中移除
                        if (retryNT.equals(noticeType.getValue()) &&
                                isRead.equals(MrsNoticeRead.UNREAD.getValue())) {
                            return null;
                        }
                    }
                    return retryNotice;
                })
                .filter(ObjUtil::isNotNull)
                .collect(Collectors.toList());
        // 更新缓存
        String json = JSONUtil.toJsonStr(retryList);
        redisTemplate.opsForValue().set(getNoticeKey(userId), json);
    }

    /**
     * 向用户推送一条消息
     *
     * @param userId 用户ID
     * @param notice 消息对象
     */
    public void pushMessage(Long userId, Notice notice) {
        // 取出重推列表，加入当前消息
        List<Notice> notices = getRetryNotices(userId);
        notices.add(notice);
        SseEmitter emitter = emitters.get(userId);
        send(userId, notices, emitter);
    }

    /**
     * 根据ID获取所有需要重发的消息
     *
     * @param userId 用户ID
     * @return 重发消息列表 或 空列表
     */
    private List<Notice> getRetryNotices(Long userId) {
        String redisKey = getNoticeKey(userId);
        // 取出重推列表，加入当前消息
        String noticesJSON = redisTemplate.opsForValue().get(redisKey);
        // log.info("检查是否有未推送消息: {}", noticesJSON);
        // 没有待推送的消息
        if (StrUtil.isBlank(noticesJSON)) return new ArrayList<>();
        List<Notice> notices = JSONUtil.toList(noticesJSON, Notice.class);
        // 消息列表为空
        if (notices.isEmpty()) return new ArrayList<>();
        log.info("未推送消息列表: {}", notices);
        return notices;
    }

    /**
     * 向用户推送消息（列表）
     *
     * @param userId  用户ID
     * @param notices 消息列表
     * @param emitter SSE实例
     */
    public void send(Long userId, List<Notice> notices, SseEmitter emitter) {
        String redisKey = getNoticeKey(userId);
        try {
            Set<JSONObject> collect = notices
                    .stream()
                    .map(NoticeVo::toVo)
                    .map(vo -> {
                        String json = JSONObject.toJSONString(vo);
                        return JSONObject.parseObject(json);
                    })
                    .collect(Collectors.toSet());
            // 推送消息
            emitter.send(SseEmitter
                    .event()
                    .data(JSONUtil.toJsonStr(collect))
            );
            // 成功送达，删除所有待推送任务
            notices = new ArrayList<>();
        } catch (RuntimeException | IOException e) {
            log.info("SSE消息推送异常，{}", userId);
            emitters.remove(userId);
            if (emitter != null) {
                emitter.completeWithError(e);
            }
        } finally {
            // 更新持久化列表
            String json = JSONUtil.toJsonStr(notices);
            redisTemplate.opsForValue().set(redisKey, json);
        }
    }

    /**
     * SSE连接主动关闭
     *
     * @param userId 用户ID
     */
    public void closeSse(Long userId) {
        // 异步线程定时任务
        ScheduledFuture<?> previousFuture = futures.get(userId);
        if (previousFuture != null) {
            previousFuture.cancel(true);
            futures.remove(userId);
        }
        // SSE事件实例
        SseEmitter previousEmitter = emitters.get(userId);
        if (previousEmitter != null) {
            previousEmitter.onCompletion(() -> {
                // log.info("尝试添加结束逻辑");
                if (previousFuture != null) {
                    // log.info("之前的任务结束，释放对应的线程资源");
                    previousFuture.cancel(true);
                }
            });
            previousEmitter.onTimeout(() -> {
                // log.info("尝试添加超时逻辑");
                if (previousFuture != null) {
                    // log.info("之前的任务超时，释放对应的线程资源");
                    previousFuture.cancel(true);
                }
            });
            // previousEmitter.complete();
            emitters.remove(userId);
        }
    }
}
