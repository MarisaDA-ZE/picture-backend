package cloud.marisa.picturebackend.config;

import cloud.marisa.picturebackend.websocket.interceptor.notice.MrsNotificationHandler;
import cloud.marisa.picturebackend.websocket.interceptor.notice.WsMessageHandShakeInterceptor;
import cloud.marisa.picturebackend.websocket.interceptor.picture.PictureEditHandler;
import cloud.marisa.picturebackend.websocket.interceptor.picture.WsHandShakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import javax.annotation.Resource;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Resource
    private PictureEditHandler pictureEditHandler;

    @Resource
    private WsHandShakeInterceptor wsHandshakeInterceptor;

    //通知服务相关
    @Resource
    private MrsNotificationHandler mrsNotificationHandler;
    @Resource
    private WsMessageHandShakeInterceptor wsMessageHandShakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // websocket
        registry.addHandler(pictureEditHandler, "/ws/picture/edit")
                .addInterceptors(wsHandshakeInterceptor)
                .setAllowedOrigins("*");
        // 通知服务
        registry.addHandler(mrsNotificationHandler, "/ws/notification")
                .addInterceptors(wsMessageHandShakeInterceptor)
                .setAllowedOrigins("*");
    }

}
