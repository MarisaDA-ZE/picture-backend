package cloud.marisa.picturebackend.manager.auth;

import cloud.marisa.picturebackend.entity.dao.Space;
import cloud.marisa.picturebackend.entity.dao.SpaceUser;
import cloud.marisa.picturebackend.entity.dao.User;
import cloud.marisa.picturebackend.enums.MrsSpaceRole;
import cloud.marisa.picturebackend.enums.MrsSpaceType;
import cloud.marisa.picturebackend.enums.UserRole;
import cloud.marisa.picturebackend.manager.auth.constant.SpaceUserPermissionConstants;
import cloud.marisa.picturebackend.manager.auth.entity.SpaceUserAuthConfig;
import cloud.marisa.picturebackend.manager.auth.entity.SpaceUserRole;
import cloud.marisa.picturebackend.service.ISpaceUserService;
import cloud.marisa.picturebackend.service.IUserService;
import cloud.marisa.picturebackend.util.EnumUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author MarisaDAZE
 * @description 团队空间权限管理器
 * @date 2025/4/14
 */
@Log4j2
@Component
public class SpaceUserAuthManager {

    @Resource
    private IUserService userService;

    @Resource
    private ISpaceUserService spaceUserService;

    private static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;

    static {
        String path = "biz/spaceUserAuthConfig.json";
        String json = ResourceUtil.readUtf8Str(path);
        SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(json, SpaceUserAuthConfig.class);
    }

    public List<String> getPermissionsByRole(String role) {
        if (StrUtil.isBlank(role)) {
            return new ArrayList<>();
        }
        SpaceUserRole userRole = SPACE_USER_AUTH_CONFIG.getRoles().stream()
                .filter(e -> role.equals(e.getKey()))
                .findFirst()
                .orElse(null);
        if (userRole == null) {
            return new ArrayList<>();
        }
        return userRole.getPermissions();
    }

    /**
     * 获取权限列表
     *
     * @param space     空间对象
     * @param loginUser 登录用户
     * @return .
     */
    public List<String> getPermissionList(Space space, User loginUser) {
        List<String> adminPermissions = getPermissionsByRole(MrsSpaceRole.ADMIN.getValue());
        boolean isAdmin = userService.hasPermission(loginUser, UserRole.ADMIN);
        // 公共空间
        if (space == null) {
            if (isAdmin) {
                return adminPermissions;
            }
            return new ArrayList<>();
        }
        // 私人空间或者团队空间
        MrsSpaceType spaceType = EnumUtil.fromValue(space.getSpaceType(), MrsSpaceType.class);
        if (spaceType == null) {
            return new ArrayList<>();
        }
        Long userId = loginUser.getId();
        // 看空间类型
        switch (spaceType) {
            case PRIVATE:
                // 是创建者或者管理员，拥有所有权限
                if (userId.equals(space.getUserId()) || isAdmin) {
                    return adminPermissions;
                }
                // 不是，无权限
                return new ArrayList<>();
            case TEAM:
                // 团队空间，根据空间ID和用户ID查库
                SpaceUser spaceUser = spaceUserService.lambdaQuery()
                        .eq(SpaceUser::getSpaceId, space.getId())
                        .eq(SpaceUser::getUserId, userId)
                        .one();
                String role = spaceUser.getSpaceRole();
                MrsSpaceRole spaceRole = EnumUtil.fromValue(role, MrsSpaceRole.class);
                if (spaceRole == null) {
                    return new ArrayList<>();
                }
                return getPermissionsByRole(spaceRole.getValue());
        }
        return new ArrayList<>();
    }

}
