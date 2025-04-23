package cloud.marisa.picturebackend.websocket.interceptor.notice;

import cloud.marisa.picturebackend.entity.dao.User;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图片协同编辑的Websocket处理器
 */
@Log4j2
@Component
public class MrsNotificationHandler extends TextWebSocketHandler {

    /**
     * 保存所有的连接会话 用户ID - 会话集合
     */
    private final Map<Long, Set<WebSocketSession>> SESSIONS = new ConcurrentHashMap<>();


    /**
     * 建立连接会话
     *
     * @param session the websocket session
     */
    @Override
    public void afterConnectionEstablished(
            @NotNull WebSocketSession session) {
        Map<String, Object> attributes = session.getAttributes();
        User user = (User) attributes.get("user");
        Long userId = user.getId();
        SESSIONS.putIfAbsent(userId, ConcurrentHashMap.newKeySet());
        SESSIONS.get(userId).add(session);
        log.info("已与用户 {} 建立ws连接", user.getUserName());
    }

    /**
     * 客户端处理文本消息
     *
     * @param session websocket 会话对象
     * @param message 消息对象
     */
    public void handleTextMessage(
            @NotNull WebSocketSession session,
            @NotNull TextMessage message) {
        log.info("收到消息: {}", message.getPayload());
    }

    public void sendMessage(Long userId, String message) {
        Set<WebSocketSession> sessionSet = SESSIONS.get(userId);
        if (sessionSet == null || sessionSet.isEmpty()) {
            log.info("未找到对应的用户 {}", userId);
            return;
        }
        for (WebSocketSession session : sessionSet) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    log.error("消息发送失败", e);
                }
            }
        }

    }

    /**
     * 关闭会话
     *
     * @param session     websocket session
     * @param closeStatus 关闭状态
     */
    @Override
    public void afterConnectionClosed(
            @NotNull WebSocketSession session,
            @NotNull CloseStatus closeStatus) {
        Map<String, Object> attributes = session.getAttributes();
        User user = (User) attributes.get("user");
        Long userId = user.getId();
        Set<WebSocketSession> sessionSet = SESSIONS.get(userId);
        if (sessionSet != null) {
            sessionSet.remove(session);
            if (sessionSet.isEmpty()) {
                SESSIONS.remove(userId);
            }
        }
        log.info("用户 {} 已关闭连接", user.getUserName());
    }
}
