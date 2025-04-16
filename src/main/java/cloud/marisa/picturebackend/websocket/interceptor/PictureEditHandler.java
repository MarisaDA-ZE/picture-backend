package cloud.marisa.picturebackend.websocket.interceptor;

import cloud.marisa.picturebackend.entity.dao.User;
import cloud.marisa.picturebackend.util.EnumUtil;
import cloud.marisa.picturebackend.websocket.entity.PictureEditRequestMessage;
import cloud.marisa.picturebackend.websocket.entity.PictureEditResponseMessage;
import cloud.marisa.picturebackend.websocket.enums.PictureEditActionEnum;
import cloud.marisa.picturebackend.websocket.enums.PictureEditMessageTypeEnum;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图片协同编辑的Websocket处理器
 */
@Log4j2
@Component
public class PictureEditHandler extends TextWebSocketHandler {

    /**
     * 正在编辑图片的用户 pictureId - currentEditUserId
     */
    private final Map<Long, Long> pictureEditUsers = new ConcurrentHashMap<>();

    /**
     * 保存所有的连接会话 pictureId - editSocketSet
     */
    private final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();

    /**
     * 建立连接会话
     * <p>发送相应的广播消息</p>
     *
     * @param session websocket 会话对象
     * @throws Exception ex
     */
    @Override
    public void afterConnectionEstablished(
            @NotNull WebSocketSession session)
            throws Exception {
        log.info("图片编辑会话建立 {}", session);
        // 将会话保存至集合
        Map<String, Object> attributes = session.getAttributes();
        User user = (User) attributes.get("user");
        Long pictureId = (Long) attributes.get("pictureId");
        pictureSessions.putIfAbsent(pictureId, ConcurrentHashMap.newKeySet());
        pictureSessions.get(pictureId).add(session);
        // 构造消息体
        PictureEditResponseMessage responseMessage = new PictureEditResponseMessage();
        responseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String message = String.format("用户 %s 进入编辑状态", user.getUserName());
        responseMessage.setMessage(message);
        responseMessage.setUser(User.toVO(user));
        // 进行广播操作
        broadcastToPicture(pictureId, responseMessage);

    }

    /**
     * 全体广播图片操作消息
     *
     * @param pictureId       图片ID
     * @param responseMessage 图片编辑响应消息
     * @throws Exception 可能的异常
     */
    private void broadcastToPicture(
            Long pictureId,
            PictureEditResponseMessage responseMessage)
            throws Exception {
        broadcastToPicture(pictureId, responseMessage, null);
    }

    /**
     * 广播图片操作消息
     *
     * @param pictureId       图片ID
     * @param responseMessage 图片编辑响应消息
     * @param excludeSession  要排除的 websocket 会话对象
     * @throws Exception 可能的异常
     */
    private void broadcastToPicture(
            Long pictureId,
            PictureEditResponseMessage responseMessage,
            WebSocketSession excludeSession)
            throws Exception {
        Set<WebSocketSession> webSocketSessions = pictureSessions.get(pictureId);
        if (CollUtil.isEmpty(webSocketSessions)) {
            log.error("未找到对应的图片编辑会话 {}", pictureId);
            return;
        }
        // 创建序列化器
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        mapper.registerModule(module);
        // 将结果序列化成JSON
        String message = mapper.writeValueAsString(responseMessage);
        log.info("序列化后的消息信息 {}", message);
        TextMessage textMessage = new TextMessage(message);
        for (WebSocketSession socketSession : webSocketSessions) {
            // 一些排除的session并不发送
            if (excludeSession != null && excludeSession.equals(socketSession)) {
                continue;
            }
            if (socketSession.isOpen()) {
                socketSession.sendMessage(textMessage);
            }
        }
    }

    /**
     * 客户端处理文本消息
     *
     * @param session websocket 会话对象
     * @param message 消息对象
     * @throws Exception ex
     */
    public void handleTextMessage(
            @NotNull WebSocketSession session,
            @NotNull TextMessage message)
            throws Exception {
        String payload = message.getPayload();
        PictureEditRequestMessage messageResponse = JSONUtil.toBean(payload, PictureEditRequestMessage.class);
        String sType = messageResponse.getType();
        PictureEditMessageTypeEnum editType = EnumUtil.fromValue(sType, PictureEditMessageTypeEnum.class);
        if (editType == null) {
            editType = PictureEditMessageTypeEnum.ERROR;
        }
        Map<String, Object> attributes = session.getAttributes();
        User user = (User) attributes.get("user");
        Long pictureId = (Long) attributes.get("pictureId");
        // 根据类型发送消息
        switch (editType) {
            case ENTER_EDIT:
                handleEnterEditMessage(messageResponse, session, user, pictureId);
                break;
            case EDIT_ACTION:
                handleEditActionMessage(messageResponse, session, user, pictureId);
                break;
            case EXIT_EDIT:
                handleExitEditMessage(messageResponse, session, user, pictureId);
                break;
            default:
                PictureEditResponseMessage responseMessage = new PictureEditResponseMessage();
                responseMessage.setType(PictureEditMessageTypeEnum.ERROR.getValue());
                String format = String.format("消息类型错误 %s", sType);
                responseMessage.setMessage(format);
                responseMessage.setUser(User.toVO(user));
                session.sendMessage(new TextMessage(JSONUtil.toJsonStr(responseMessage)));
        }
    }

    @Override
    public void afterConnectionClosed(
            @NotNull WebSocketSession session,
            @NotNull CloseStatus closeStatus)
            throws Exception {
        Map<String, Object> attributes = session.getAttributes();
        User user = (User) attributes.get("user");
        Long pictureId = (Long) attributes.get("pictureId");
        handleExitEditMessage(null, session, user, pictureId);
        // 移除用户编辑信息
        Set<WebSocketSession> webSocketSet = pictureSessions.get(pictureId);
        if (webSocketSet != null) {
            webSocketSet.remove(session);
            if (webSocketSet.isEmpty()) {
                pictureSessions.remove(pictureId);
            }
        }
        // 构建响应消息
        String message = String.format("用户 %s 已离开编辑", user.getUserName());
        PictureEditResponseMessage responseMessage = PictureEditResponseMessage.builder()
                .type(PictureEditMessageTypeEnum.INFO.getValue())
                .message(message)
                .user(User.toVO(user))
                .build();
        // 发送广播消息
        broadcastToPicture(pictureId, responseMessage);
    }

    private void handleEnterEditMessage(
            PictureEditRequestMessage requestMessage,
            WebSocketSession session,
            User user,
            Long pictureId) throws Exception {
        // 有用户正在编辑，不能操作
        if (pictureEditUsers.containsKey(pictureId)) {
            return;
        }
        // 没有用户正在编辑
        pictureEditUsers.put(pictureId, user.getId());
        String message = String.format("用户 %s 进入编辑状态", user.getUserName());
        PictureEditResponseMessage responseMessage = PictureEditResponseMessage.builder()
                .type(PictureEditMessageTypeEnum.ENTER_EDIT.getValue())
                .message(message)
                .user(User.toVO(user))
                .build();
        broadcastToPicture(pictureId, responseMessage);
    }

    private void handleEditActionMessage(
            PictureEditRequestMessage requestMessage,
            WebSocketSession session,
            User user,
            Long pictureId) throws Exception {

        // 获取正在编辑的用户ID
        Long editUserId = pictureEditUsers.get(pictureId);
        String sAction = requestMessage.getEditAction();
        PictureEditActionEnum editAction = EnumUtil.fromValue(sAction, PictureEditActionEnum.class);
        if (editAction == null) {
            return;
        }
        // 有人正在编辑，并且编辑者是自己时
        if (editUserId != null && editUserId.equals(user.getId())) {
            pictureEditUsers.remove(pictureId);
            String message = String.format("用户 %s 执行了 %s", user.getUserName(), editAction.getText());
            PictureEditResponseMessage responseMessage = PictureEditResponseMessage.builder()
                    .type(PictureEditMessageTypeEnum.EDIT_ACTION.getValue())
                    .message(message)
                    .user(User.toVO(user))
                    .build();
            // 发送广播消息
            broadcastToPicture(pictureId, responseMessage);
        }
    }

    private void handleExitEditMessage(
            PictureEditRequestMessage requestMessage,
            WebSocketSession session,
            User user,
            Long pictureId) throws Exception {
        Long editUserId = pictureEditUsers.get(pictureId);

        if (editUserId == null || !editUserId.equals(user.getId())) {
            return;
        }
        pictureEditUsers.remove(pictureId);
        // 构建响应消息
        String message = String.format("用户 %s 已退出编辑", user.getUserName());
        PictureEditResponseMessage responseMessage = PictureEditResponseMessage.builder()
                .type(PictureEditMessageTypeEnum.EXIT_EDIT.getValue())
                .message(message)
                .user(User.toVO(user))
                .build();
        // 发送广播消息
        broadcastToPicture(pictureId, responseMessage);
    }
}
