package cloud.marisa.picturebackend.websocket.interceptor.picture;

import cloud.marisa.picturebackend.entity.dao.Picture;
import cloud.marisa.picturebackend.entity.dao.Space;
import cloud.marisa.picturebackend.entity.dao.User;
import cloud.marisa.picturebackend.enums.MrsSpaceType;
import cloud.marisa.picturebackend.manager.auth.SpaceUserAuthManager;
import cloud.marisa.picturebackend.manager.auth.constant.SpaceUserPermissionConstants;
import cloud.marisa.picturebackend.service.IPictureService;
import cloud.marisa.picturebackend.service.ISpaceService;
import cloud.marisa.picturebackend.service.IUserService;
import cloud.marisa.picturebackend.util.EnumUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * @author MarisaDAZE
 * @description Websocket握手拦截器
 * @date 2025/4/15
 */
@Log4j2
@Component
public class WsHandShakeInterceptor implements HandshakeInterceptor {

    @Resource
    private IUserService userService;

    @Resource
    private IPictureService pictureService;

    @Resource
    private ISpaceService spaceService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Override
    public boolean beforeHandshake(
            @NotNull ServerHttpRequest request,
            @NotNull ServerHttpResponse response,
            @NotNull WebSocketHandler wsHandler,
            @NotNull Map<String, Object> attributes
    ) throws Exception {
        // 如果请求不是 ServletServerHttpRequest，放行
        if (!(request instanceof ServletServerHttpRequest)) {
            return true;
        }
        ServletServerHttpRequest servletServerHttpRequest = (ServletServerHttpRequest) request;
        HttpServletRequest servletRequest = servletServerHttpRequest.getServletRequest();
        String pid = servletRequest.getParameter("pictureId");
        Long pictureId = NumberUtil.parseLong(pid, 0L);
        // 没有图片ID）
        if (pictureId.equals(0L)) {
            log.error("图片ID为空，拒绝握手 {}", pid);
            return false;
        }
        // 未登录
        User loginUser = userService.getLoginUser(servletRequest);
        if (loginUser == null) {
            log.error("用户未登录，拒绝握手");
            return false;
        }
        // 图片不存在
        Picture targetPicture = pictureService.getById(pictureId);
        if (targetPicture == null) {
            log.error("图片不存在 {}", pictureId);
            return false;
        }
        Long spaceId = targetPicture.getSpaceId();
        Space space = null;
        // 是否为空间编辑（等于空 或者 等于0 有可能是公共空间的图片编辑）
        if (spaceId != null && !spaceId.equals(0L)) {
            space = spaceService.getById(spaceId);
            // 空间不存在
            if (space == null) {
                log.error("团队空间不存在 {}", spaceId);
                return false;
            }
            // 不是团队空间
            MrsSpaceType spaceType = EnumUtil.fromValue(space.getSpaceType(), MrsSpaceType.class);
            if (spaceType != MrsSpaceType.TEAM) {
                log.error("仅支持团队空间协同编辑 {}", spaceType);
                return false;
            }
        }
        // 获取权限列表
        List<String> permissions = spaceUserAuthManager.getPermissionList(space, loginUser);
        if (!permissions.contains(SpaceUserPermissionConstants.PICTURE_EDIT)) {
            String account = loginUser.getUserAccount();
            log.error("用户 {} 没有编辑权限 {}", account, permissions);
            return false;
        }
        attributes.put("user", loginUser);
        attributes.put("userId", loginUser.getId());
        attributes.put("pictureId", pictureId);
        return true;
    }

    @Override
    public void afterHandshake(
            @NotNull ServerHttpRequest request,
            @NotNull ServerHttpResponse response,
            @NotNull WebSocketHandler wsHandler,
            Exception exception) {

    }
}
