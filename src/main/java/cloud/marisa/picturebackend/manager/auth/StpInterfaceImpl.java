package cloud.marisa.picturebackend.manager.auth;

import cloud.marisa.picturebackend.entity.dao.Picture;
import cloud.marisa.picturebackend.entity.dao.Space;
import cloud.marisa.picturebackend.entity.dao.SpaceUser;
import cloud.marisa.picturebackend.entity.dao.User;
import cloud.marisa.picturebackend.enums.MrsSpaceRole;
import cloud.marisa.picturebackend.enums.MrsSpaceType;
import cloud.marisa.picturebackend.enums.UserRole;
import cloud.marisa.picturebackend.exception.BusinessException;
import cloud.marisa.picturebackend.exception.ErrorCode;
import cloud.marisa.picturebackend.manager.auth.constant.SpaceUserPermissionConstants;
import cloud.marisa.picturebackend.manager.auth.entity.SpaceUserAuthContext;
import cloud.marisa.picturebackend.service.IPictureService;
import cloud.marisa.picturebackend.service.ISpaceService;
import cloud.marisa.picturebackend.service.ISpaceUserService;
import cloud.marisa.picturebackend.service.IUserService;
import cloud.marisa.picturebackend.util.EnumUtil;
import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.json.JSONUtil;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static cloud.marisa.picturebackend.common.Constants.USER_LOGIN;

/**
 * @author MarisaDAZE
 * @description StpInterfaceImpl.类
 * @date 2025/4/14
 */
@Log4j2
@Component
public class StpInterfaceImpl implements StpInterface {

    @Resource
    private IUserService userService;

    @Resource
    private IPictureService pictureService;

    @Resource
    private ISpaceService spaceService;

    @Resource
    private ISpaceUserService spaceUserService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 不属于空间权限系统
        if (!StpKit.SPACE_TYPE.equals(loginType)) {
            return new ArrayList<>();
        }
        List<String> adminPermissions = spaceUserAuthManager
                .getPermissionsByRole(MrsSpaceRole.ADMIN.getValue());
        SpaceUserAuthContext authContext = getAuthContextByRequest();
        // 所有字段都是空的，是访问的公共图库，且是管理员
        if (isAllFieldsNull(adminPermissions)) {
            return adminPermissions;
        }
        // 获取登录用户，并看他是不是管理员
        Object o = StpKit.SPACE.getSessionByLoginId(loginId).get(USER_LOGIN);
        if (o == null) {
            return new ArrayList<>();
        }
        User loginUser = (User) o;
        Long userId = loginUser.getId();
        boolean isAdmin = userService.hasPermission(loginUser, UserRole.ADMIN);
        // 优先从空间团队表中获取结果
        SpaceUser spaceUser = authContext.getSpaceUser();
        if (spaceUser != null) {
            // 根据当前的角色返回对应的操作权限
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }
        // 传了空间用户ID，说明正在操作团队空间
        Long spaceUserId = authContext.getSpaceUserId();
        if (spaceUserId != null) {
            if (spaceUserId <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间用户ID错误");
            }
            spaceUser = spaceUserService.getById(spaceUserId);
            if (spaceUser == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "未找到空间用户信息");
            }
            // TODO: 不太懂为啥要这样，不可以直接用spaceUser吗？
            SpaceUser dbSpaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceUser.getSpaceId())
                    .eq(SpaceUser::getUserId, userId)
                    .one();
            if (dbSpaceUser == null) {
                return new ArrayList<>();
            }
            return spaceUserAuthManager.getPermissionsByRole(dbSpaceUser.getSpaceRole());
        }

        // 可能是公共空间，也可能没传空间ID
        Long spaceId = authContext.getSpaceId();
        if (spaceId == null) {
            Long pictureId = authContext.getPictureId();
            // 没有空间ID也没有图片ID，默认放行
            if (pictureId == null) {
                return adminPermissions;
            }
            Picture picture = pictureService.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .select(Picture::getId, Picture::getSpaceId, Picture::getUserId)
                    .one();
            if (picture == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "未找到图片信息");
            }
            spaceId = picture.getSpaceId();
            // 操作公共空间图片，需要管理员或他是图片创建者
            if (spaceId == null) {
                if (picture.getUserId().equals(userId) || isAdmin) {
                    return adminPermissions;
                }
                // 仅有基础查看权限
                return Collections.singletonList(SpaceUserPermissionConstants.PICTURE_VIEW);
            }
        }

        Space space = spaceService.getById(spaceId);
        if (space == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "未找到空间信息");
        }
        MrsSpaceType spaceType = EnumUtil.fromValue(space.getSpaceType(), MrsSpaceType.class);
        // 私人空间操作
        if (spaceType == MrsSpaceType.PRIVATE) {
            // 是本人或者管理员
            if (space.getUserId().equals(userId) || isAdmin) {
                return adminPermissions;
            }
            // 否则啥权限都没有
            return new ArrayList<>();
        } else {
            // 团队空间操作
            spaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceId)
                    .eq(SpaceUser::getUserId, userId)
                    .one();
            if (spaceUser == null) {
                return new ArrayList<>();
            }
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }
    }

    @Override
    public List<String> getRoleList(Object o, String s) {
        return new ArrayList<>();
    }

    /**
     * 从HTTP上下文中获取团队空间权限上下文需要的参数
     *
     * @return 团队空间权限上下文
     */
    public SpaceUserAuthContext getAuthContextByRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String contentType = request.getHeader(Header.CONTENT_TYPE.getValue());
        SpaceUserAuthContext authContext;
        log.debug("内容类型 contentType: {}", contentType);
        // 传递的参数是application/json格式的
        if (ContentType.JSON.getValue().equals(contentType)) {
            String body = ServletUtil.getBody(request);
            log.debug("json body: {}", body);
            authContext = JSONUtil.toBean(body, SpaceUserAuthContext.class);
        } else {
            Map<String, String> paramMap = ServletUtil.getParamMap(request);
            log.debug("paramMap: {}", paramMap);
            authContext = BeanUtil.toBean(paramMap, SpaceUserAuthContext.class);
        }
        log.debug("authContext: {}", authContext);
        Long id = authContext.getId();
        if (ObjUtil.isNotNull(id)) {
            String requestURI = request.getRequestURI();
            log.debug("请求URI: {}", requestURI);
            String replace = requestURI.replace(contextPath + "/", "");
            String modelName = StrUtil.subBefore(replace, "/", false);
            log.debug("modelName: {}", modelName);
            switch (modelName) {
                case "picture":
                    authContext.setPictureId(id);
                    break;
                case "space":
                    authContext.setSpaceId(id);
                    break;
                case "spaceUser":
                    authContext.setSpaceUserId(id);
                    break;
            }
        }
        log.debug("最终结果: {}", authContext);
        return authContext;
    }

    private boolean isAllFieldsNull(Object object) {
        if (object == null) {
            return true; // 对象本身为空
        }
        // 获取所有字段并判断是否所有字段都为空
        return Arrays.stream(ReflectUtil.getFields(object.getClass()))
                // 获取字段值
                .map(field -> ReflectUtil.getFieldValue(object, field))
                // 检查是否所有字段都为空
                .allMatch(ObjectUtil::isEmpty);
    }

}
