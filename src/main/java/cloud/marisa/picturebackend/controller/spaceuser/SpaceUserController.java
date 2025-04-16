package cloud.marisa.picturebackend.controller.spaceuser;

import cloud.marisa.picturebackend.annotations.SaSpaceCheckPermission;
import cloud.marisa.picturebackend.common.MrsResult;
import cloud.marisa.picturebackend.entity.dao.SpaceUser;
import cloud.marisa.picturebackend.entity.dao.User;
import cloud.marisa.picturebackend.entity.dto.common.DeleteRequest;
import cloud.marisa.picturebackend.entity.dto.spaceuser.SpaceUserAddRequest;
import cloud.marisa.picturebackend.entity.dto.spaceuser.SpaceUserEditRequest;
import cloud.marisa.picturebackend.entity.dto.spaceuser.SpaceUserQueryRequest;
import cloud.marisa.picturebackend.entity.vo.SpaceUserVo;
import cloud.marisa.picturebackend.exception.BusinessException;
import cloud.marisa.picturebackend.exception.ErrorCode;
import cloud.marisa.picturebackend.manager.auth.constant.SpaceUserPermissionConstants;
import cloud.marisa.picturebackend.service.ISpaceUserService;
import cloud.marisa.picturebackend.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * @author MarisaDAZE
 * @description 团队空间服务的控制层
 * @date 2025/4/14
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/spaceUser")
public class SpaceUserController {

    /**
     * 用户服务
     */
    private final IUserService userService;

    /**
     * 团队空间服务
     */
    private final ISpaceUserService spaceUserService;

    /**
     * 根据条件查询一个 加入我的 团队空间的 成员信息
     *
     * @param queryRequest       查询参数的DTO封装
     * @param httpServletRequest HttpServlet请求对象
     * @return 团队空间成员信息
     */
    @PostMapping("/get")
    @SaSpaceCheckPermission(SpaceUserPermissionConstants.SPACE_USER_MANAGE)
    public MrsResult<?> getSpaceUser(
            @RequestBody SpaceUserQueryRequest queryRequest,
            HttpServletRequest httpServletRequest) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 获取登录用户
        User loginUser = userService.getLoginUser(httpServletRequest);
        // 获取一个符合条件的 团队空间成员
        SpaceUser spaceUser = spaceUserService.getSpaceUser(queryRequest, loginUser);
        if (spaceUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return MrsResult.ok(spaceUser);
    }

    /**
     * 根据条件 查询一个加入我的 团队空间的 成员的信息
     * <p>不限制权限，任何人都可用</p>
     * <p>但未登录会抛出异常</p>
     *
     * @param queryRequest       查询参数的DTO封装
     * @param httpServletRequest HttpServlet请求对象
     * @return 团队空间成员信息
     */
    @PostMapping("/get/vo")
    public MrsResult<?> getSpaceUserVo(
            @RequestBody SpaceUserQueryRequest queryRequest,
            HttpServletRequest httpServletRequest) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 获取登录用户
        User loginUser = userService.getLoginUserIfLogin(httpServletRequest);
        // 登录用户为空说明此时用户未登录
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 获取团队空间对象
        SpaceUser spaceUser = spaceUserService.getSpaceUser(queryRequest, loginUser);
        if (spaceUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        SpaceUserVo vo = spaceUserService.getSpaceUserVo(spaceUser);
        return MrsResult.ok(vo);
    }

    /**
     * 获取加入我的 团队空间的 用户列表
     *
     * @param queryRequest       查询参数的DTO封装
     * @param httpServletRequest HttpServlet请求对象
     * @return 团队空间成员列表
     */
    @PostMapping("/list")
    @SaSpaceCheckPermission(SpaceUserPermissionConstants.SPACE_USER_MANAGE)
    public MrsResult<?> getSpaceUserList(
            @RequestBody SpaceUserQueryRequest queryRequest,
            HttpServletRequest httpServletRequest) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 获取登录用户
        User loginUser = userService.getLoginUser(httpServletRequest);
        // 获取加入（我的）团队空间的用户列表
        List<SpaceUser> spaceUsers = spaceUserService.getSpaceUserList(queryRequest, loginUser);
        // 转成Vo并返回前端
        List<SpaceUserVo> voList = spaceUserService.getSpaceUserVoList(spaceUsers);
        return MrsResult.ok(voList);
    }

    /**
     * 查询我加入的 团队空间 的列表
     *
     * @param httpServletRequest HttpServlet请求对象
     * @return 我加入的 团队空间列表
     */
    @PostMapping("/list/my")
    public MrsResult<?> listMyTeamSpace(HttpServletRequest httpServletRequest) {
        // 获取登录用户
        User loginUser = userService.getLoginUserIfLogin(httpServletRequest);
        // 此时用户未登录，则没有团队空间
        if (loginUser == null) {
            return MrsResult.ok(new ArrayList<>());
        }
        // 组装查询条件
        SpaceUserQueryRequest queryRequest = new SpaceUserQueryRequest();
        queryRequest.setUserId(loginUser.getId());
        // 查询我加入的 团队空间 的列表
        List<SpaceUser> spaceUsers = spaceUserService.getSpaceUserList(queryRequest, loginUser);
        // 转成Vo并返回前端
        List<SpaceUserVo> voList = spaceUserService.getSpaceUserVoList(spaceUsers);
        return MrsResult.ok(voList);
    }

    /**
     * 添加一名成员到 我的团队空间
     *
     * @param addRequest         添加参数的DTO封装
     * @param httpServletRequest HttpServlet请求对象
     * @return 成员ID
     */
    @PostMapping("/add")
    @SaSpaceCheckPermission(SpaceUserPermissionConstants.SPACE_USER_MANAGE)
    public MrsResult<?> addSpaceUser(
            @RequestBody SpaceUserAddRequest addRequest,
            HttpServletRequest httpServletRequest) {
        if (addRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 获取登录用户
        User loginUser = userService.getLoginUser(httpServletRequest);
        // 添加一个用户到（我的）团队空间
        long addId = spaceUserService.addSpaceUser(addRequest, loginUser);
        return MrsResult.ok(addId);
    }

    /**
     * 编辑一个加入我团队空间的成员信息
     *
     * @param editRequest        添加参数的DTO封装
     * @param httpServletRequest HttpServlet请求对象
     * @return 团队空间成员ID
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(SpaceUserPermissionConstants.SPACE_USER_MANAGE)
    public MrsResult<?> editSpaceUser(
            @RequestBody SpaceUserEditRequest editRequest,
            HttpServletRequest httpServletRequest) {
        if (editRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 获取登录用户
        User loginUser = userService.getLoginUser(httpServletRequest);
        // 编辑加入（我的）团队空间的一个用户
        boolean edited = spaceUserService.editSpaceUser(editRequest, loginUser);
        if (!edited) {
            return MrsResult.failed(false);
        }
        return MrsResult.ok(true);
    }

    /**
     * 删除一名加入我团队空间成员
     *
     * @param deleteRequest      删除参数的DTO封装
     * @param httpServletRequest HttpServlet请求对象
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(SpaceUserPermissionConstants.SPACE_USER_MANAGE)
    public MrsResult<?> deleteSpaceUser(
            @RequestBody DeleteRequest deleteRequest,
            HttpServletRequest httpServletRequest) {
        if (deleteRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 获取登录用户
        User loginUser = userService.getLoginUser(httpServletRequest);
        // 删除（我的）团队空间中的一个成员
        boolean deleted = spaceUserService.deleteSpaceUserById(deleteRequest, loginUser);
        if (!deleted) {
            return MrsResult.failed(false);
        }
        return MrsResult.ok(true);
    }
}
