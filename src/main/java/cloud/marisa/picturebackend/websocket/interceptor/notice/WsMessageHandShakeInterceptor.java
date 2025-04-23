package cloud.marisa.picturebackend.websocket.interceptor.notice;

import cloud.marisa.picturebackend.entity.dao.User;
import cloud.marisa.picturebackend.service.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Log4j2
@Component
@RequiredArgsConstructor
public class WsMessageHandShakeInterceptor implements HandshakeInterceptor {

    private final IUserService userService;

    /**
     * 握手前获取请求参数
     *
     * @param request    当前请求
     * @param response   当前响应对象
     * @param wsHandler  the target WebSocket handler
     * @param attributes the attributes from the HTTP handshake to associate with the WebSocket
     *                   session; the provided attributes are copied, the original map is not used.
     * @return 是否放行
     */
    @Override
    public boolean beforeHandshake(
            @NotNull ServerHttpRequest request,
            @NotNull ServerHttpResponse response,
            @NotNull WebSocketHandler wsHandler,
            @NotNull Map<String, Object> attributes) {
        // 获取原生 HttpServletRequest（需要类型转换）
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            HttpServletRequest httpRequest = servletRequest.getServletRequest();
            User user = userService.getLoginUserIfLogin(httpRequest);
            if (user == null) {
                log.info("当前用户未登录");
                return false;
            }
            attributes.put("user", user);
        }
        return true;
    }

    @Override
    public void afterHandshake(
            @NotNull ServerHttpRequest request,
            @NotNull ServerHttpResponse response,
            @NotNull WebSocketHandler wsHandler,
            Exception exception) {
        // 握手后逻辑（可选）
    }
}