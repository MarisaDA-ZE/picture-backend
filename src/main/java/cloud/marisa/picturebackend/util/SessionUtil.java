package cloud.marisa.picturebackend.util;

import cloud.marisa.picturebackend.entity.dao.User;
import cloud.marisa.picturebackend.enums.MrsUserRole;
import cloud.marisa.picturebackend.exception.BusinessException;
import cloud.marisa.picturebackend.exception.ErrorCode;
import lombok.extern.log4j.Log4j2;
import org.springframework.util.ObjectUtils;

import javax.servlet.http.HttpServletRequest;

import static cloud.marisa.picturebackend.common.Constants.USER_LOGIN;

/**
 * @author MarisaDAZE
 * @description Session工具类
 * @date 2025/4/1
 */
@Log4j2
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
     * 从Session中获取一个用户信息
     *
     * @param request HttpServlet请求对象
     * @return 登录用户
     */
    public static User getUserIfLogin(HttpServletRequest request) {
        try {
            return getSession(USER_LOGIN, request, User.class);
        } catch (BusinessException ex) {
            // 如果报错，说明用户未登录,返回空
            return null;
        }
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
    public static boolean hasPermission(User user, MrsUserRole hasRole) {
        if (user == null || hasRole == null) {
            return false;
        }
        MrsUserRole currentRole = EnumUtil.fromValue(user.getUserRole(), MrsUserRole.class);
        if (currentRole == null) {
            return false;
        }
        return currentRole.moreThanRole(hasRole);
    }

    /**
     * 判断当前角色是否具有某个权限
     *
     * @param currentRole 校验用户
     * @param targetRole  权限等级
     * @return true:具有；false:不具有
     */
    public static boolean hasPermission(MrsUserRole currentRole, MrsUserRole targetRole) {
        if (currentRole == null || targetRole == null) {
            return false;
        }
        return currentRole.moreThanRole(targetRole);
    }
}
