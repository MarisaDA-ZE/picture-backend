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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author MarisaDAZE
 * @description 空间-用户控制层
 * @date 2025/4/13
 */
@RestController
@RequestMapping("/spaceUser")
public class SpaceUserController {

    @Autowired
    private IUserService userService;

    @Autowired
    private ISpaceUserService spaceUserService;

    /**
     * 获取空间-用户对象
     *
     * @param queryRequest       查询参数的DTO封装
     * @param httpServletRequest HttpServlet请求对象
     * @return 空间-用户对象
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
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 获取空间-用户对象
        SpaceUser spaceUser = spaceUserService.getSpaceUser(queryRequest, loginUser);
        if (spaceUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return MrsResult.ok(spaceUser);
    }

    /**
     * 获取空间-用户Vo对象
     *
     * @param queryRequest       查询参数的DTO封装
     * @param httpServletRequest HttpServlet请求对象
     * @return 空间-用户Vo对象
     */
    @PostMapping("/get/vo")
    public MrsResult<?> getSpaceUserVo(
            @RequestBody SpaceUserQueryRequest queryRequest,
            HttpServletRequest httpServletRequest) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 获取登录用户
        User loginUser = userService.getLoginUser(httpServletRequest);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 获取空间-用户对象
        SpaceUser spaceUser = spaceUserService.getSpaceUser(queryRequest, loginUser);
        if (spaceUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        SpaceUserVo vo = spaceUserService.getSpaceUserVo(spaceUser);
        return MrsResult.ok(vo);
    }

    /**
     * 获取空间-用户对象
     *
     * @param queryRequest       查询参数的DTO封装
     * @param httpServletRequest HttpServlet请求对象
     * @return 空间-用户对象
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
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        List<SpaceUser> spaceUsers = spaceUserService.getSpaceUserList(queryRequest, loginUser);
        List<SpaceUserVo> voList = spaceUserService.getSpaceUserVoList(spaceUsers);
        return MrsResult.ok(voList);
    }

    /**
     * 查询 我 加入的团队 的 空间列表
     *
     * @param httpServletRequest HttpServlet请求对象
     * @return 空间-用户对象
     */
    @PostMapping("/list/my")
    public MrsResult<?> listMyTeamSpace(HttpServletRequest httpServletRequest) {
        // 获取登录用户
        User loginUser = userService.getLoginUser(httpServletRequest);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        SpaceUserQueryRequest queryRequest = new SpaceUserQueryRequest();
        queryRequest.setUserId(loginUser.getId());
        List<SpaceUser> spaceUsers = spaceUserService.getSpaceUserList(queryRequest, loginUser);
        List<SpaceUserVo> voList = spaceUserService.getSpaceUserVoList(spaceUsers);
        return MrsResult.ok(voList);
    }

    /**
     * 添加空间-用户对象
     *
     * @param addRequest         添加参数的DTO封装
     * @param httpServletRequest HttpServlet请求对象
     * @return 空间-用户对象ID
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
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 添加空间用户
        long addId = spaceUserService.addSpaceUser(addRequest, loginUser);
        return MrsResult.ok(addId);
    }

    /**
     * 添加空间-用户对象
     *
     * @param editRequest        添加参数的DTO封装
     * @param httpServletRequest HttpServlet请求对象
     * @return 空间-用户对象ID
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
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        boolean edited = spaceUserService.editSpaceUser(editRequest, loginUser);
        if (!edited) {
            return MrsResult.failed(false);
        }
        return MrsResult.ok(true);
    }

    /**
     * 删除空间-用户对象
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
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 删除空间用户
        boolean deleted = spaceUserService.deleteSpaceUserById(deleteRequest, loginUser);
        if (!deleted) {
            return MrsResult.failed(false);
        }
        return MrsResult.ok(true);
    }
}
