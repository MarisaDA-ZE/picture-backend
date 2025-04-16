package cloud.marisa.picturebackend.util;

import cloud.marisa.picturebackend.entity.dao.User;
import cloud.marisa.picturebackend.enums.MrsUserRole;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author MarisaDAZE
 * @description MrsAuthUtil.类
 * @date 2025/4/5
 */
public class MrsAuthUtil {

    /**
     * 获取前端用的权限列表
     * <p>根据前端枚举出来的（气</p>
     *
     * @param uid       从某个非用户对象上取到的用户ID
     * @param loginUser 登录用户
     * @return 权限列表
     */
    public static List<String> getPermissions(Long uid, User loginUser) {
        Long u_uid = loginUser.getId();
        MrsUserRole currentRole = EnumUtil.fromValue(loginUser.getUserRole(), MrsUserRole.class);
        return getPermissions(uid, u_uid, currentRole);
    }

    /**
     * 获取前端用的权限列表
     * <p>根据前端枚举出来的（气</p>
     *
     * @param uid         从某个非用户对象上取到的用户ID
     * @param u_uid       登录用户的UID
     * @param currentRole 当前所属的角色
     * @return 权限列表
     */
    public static List<String> getPermissions(Long uid, Long u_uid, MrsUserRole currentRole) {
        ArrayList<String> permissions = new ArrayList<>();
        permissions.add("picture:view");
        permissions.add("picture:upload");
        // 是你自己的或者你是管理员
        if (Objects.equals(uid, u_uid) || (currentRole != null && currentRole.moreThanRole(MrsUserRole.ADMIN))) {
            permissions.add("picture:edit");
            permissions.add("picture:delete");
        }
        if (Objects.equals(uid, u_uid)) {
            permissions.add("spaceUser:manage");
        }
        return permissions;
    }
}
