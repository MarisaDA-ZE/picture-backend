package cloud.marisa.picturebackend.util;

import cloud.marisa.picturebackend.entity.dao.User;
import cloud.marisa.picturebackend.enums.UserRole;
import cloud.marisa.picturebackend.exception.BusinessException;
import cloud.marisa.picturebackend.exception.ErrorCode;
import org.springframework.util.ObjectUtils;

import javax.servlet.http.HttpServletRequest;

import static cloud.marisa.picturebackend.common.Constants.USER_LOGIN;

/**
 * @author MarisaDAZE
 * @description Session工具类
 * @date 2025/4/1
 */
public class SessionUtil {

    private SessionUtil() {
    }

    /**
     * 获取登录用户信息
     *
     * @param request HttpServlet请求对象
     * @return 登录用户
     */
    public static User getLoginUser(HttpServletRequest request) {
        User loginedUser = getSession(USER_LOGIN, request, User.class);
        if (ObjectUtils.isEmpty(loginedUser.getId())) {
            throw new BusinessException(ErrorCode.AUTHORIZATION_ERROR);
        }
        return loginedUser;
    }

    /**
     * 获取Session
     *
     * @param key     键
     * @param request HttpServlet请求对象
     * @param clazz   要转换的类型
     * @param <T>     类型T
     * @return 结果
     */
    @SuppressWarnings("unchecked")
    public static <T> T getSession(String key, HttpServletRequest request, Class<T> clazz) {
        Object session = request.getSession().getAttribute(key);
        if (ObjectUtils.isEmpty(session)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return (T) session;
    }

    /**
     * 账号是否有某个权限
     *
     * @param user    校验用户
     * @param hasRole 权限等级
     * @return true:具有；false:不具有
     */
    public static boolean hasPermission(User user, UserRole hasRole) {
        if (user == null || hasRole == null) {
            return false;
        }
        UserRole currentRole = EnumUtil.fromValue(user.getUserRole(), UserRole.class);
        if (currentRole == null) {
            return false;
        }
        return currentRole.getLevel() >= hasRole.getLevel();
    }
}
