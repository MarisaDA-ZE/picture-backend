package cloud.marisa.picturebackend.controller.space;

import cloud.marisa.picturebackend.annotations.AuthCheck;
import cloud.marisa.picturebackend.annotations.SaSpaceCheckPermission;
import cloud.marisa.picturebackend.common.MrsResult;
import cloud.marisa.picturebackend.entity.dao.Space;
import cloud.marisa.picturebackend.entity.dao.User;
import cloud.marisa.picturebackend.entity.dto.common.DeleteRequest;
import cloud.marisa.picturebackend.entity.dto.space.SpaceAddRequest;
import cloud.marisa.picturebackend.entity.dto.space.SpaceQueryRequest;
import cloud.marisa.picturebackend.entity.dto.space.SpaceUpdateRequest;
import cloud.marisa.picturebackend.entity.vo.SpaceLevelVo;
import cloud.marisa.picturebackend.entity.vo.SpaceVo;
import cloud.marisa.picturebackend.enums.MrsUserRole;
import cloud.marisa.picturebackend.enums.SpaceLevelEnum;
import cloud.marisa.picturebackend.exception.BusinessException;
import cloud.marisa.picturebackend.exception.ErrorCode;
import cloud.marisa.picturebackend.manager.auth.SpaceUserAuthManager;
import cloud.marisa.picturebackend.manager.auth.constant.SpaceUserPermissionConstants;
import cloud.marisa.picturebackend.service.ISpaceService;
import cloud.marisa.picturebackend.service.IUserService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static cloud.marisa.picturebackend.common.Constants.MAX_PAGE_SIZE;

/**
 * @author MarisaDAZE
 * @description 空间图片控制层
 * @date 2025/4/4
 */
@RestController
@RequestMapping("/space")
@RequiredArgsConstructor
public class SpaceController {

    /**
     * 用户服务
     */
    private final IUserService userService;

    /**
     * 空间服务
     */
    private final ISpaceService spaceService;

    /**
     * 团队空间用户权限管理器
     */
    private final SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 管理员按ID查询空间信息
     *
     * @param id 用户ID
     * @return 空间对象
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = MrsUserRole.ADMIN)
    public MrsResult<?> getOne(@RequestParam Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Space space = spaceService.getById(id);
        if (space == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "空间不存在");
        }
        return MrsResult.ok(space);
    }

    /**
     * 按ID查询空间信息
     *
     * @param id             空间ID
     * @param servletRequest HttpServlet请求对象
     * @return 空间对象
     */
    @GetMapping("/get/vo")
    @SaSpaceCheckPermission(SpaceUserPermissionConstants.PICTURE_VIEW)
    public MrsResult<?> getOneVo(
            @RequestParam(name = "id") Long id,
            HttpServletRequest servletRequest) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Space space = spaceService.getById(id);
        if (space == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "空间不存在");
        }
        User loggedUser = userService.getLoginUser(servletRequest);
        /* 权限管理（已废弃），现在统一使用sa-token提供的注解管理空间权限
         * Long userId = loggedUser.getId();
         * boolean isAdmin = userService.hasPermission(loggedUser, MrsUserRole.ADMIN);
         * if (!Objects.equals(space.getUserId(), userId) && !isAdmin) {
         *     throw new BusinessException(ErrorCode.AUTHORIZATION_ERROR, "无空间访问权限");
         * }
         * */
        SpaceVo vo = SpaceVo.toVo(space);
        List<String> permissions = spaceUserAuthManager.getPermissionList(space, loggedUser);
        vo.setPermissionList(permissions);
        return MrsResult.ok(vo);
    }

    /**
     * 分页查询空间信息
     * <p>仅管理员可用</p>
     * <p>以post方式的查询</p>
     *
     * @param queryRequest 查询DTO对象
     * @return 空间VO分页对象
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = MrsUserRole.ADMIN)
    public MrsResult<?> queryPage(
            @RequestBody SpaceQueryRequest queryRequest,
            HttpServletRequest servletRequest) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(servletRequest);
        Page<Space> spacePage = spaceService.getSpacePage(queryRequest, loginUser);
        return MrsResult.ok(spacePage);
    }

    /**
     * 分页查询空间信息
     * <p>以post方式的查询</p>
     *
     * @param queryRequest   查询DTO对象
     * @param servletRequest HttpServlet请求对象
     * @return 空间VO分页对象
     */
    @PostMapping("/list/page/vo")
    @SaSpaceCheckPermission(SpaceUserPermissionConstants.PICTURE_VIEW)
    public MrsResult<?> queryPageVo(
            @RequestBody SpaceQueryRequest queryRequest,
            HttpServletRequest servletRequest) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        int size = queryRequest.getPageSize();
        if (size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分页参数过大");
        }
        User loginUser = userService.getLoginUser(servletRequest);
        Page<Space> spacePage = spaceService.getSpacePage(queryRequest, loginUser);
        Page<SpaceVo> spacePageVo = spaceService.getSpacePageVo(spacePage, loginUser);
        return MrsResult.ok(spacePageVo);
    }

    /**
     * 管理员更新空间信息
     *
     * @param addRequest     创建数据的DTO封装
     * @param servletRequest httpServlet请求对象
     * @return 是否更新成功
     */
    @PostMapping("/add")
    @SaSpaceCheckPermission(SpaceUserPermissionConstants.PICTURE_UPLOAD)
    public MrsResult<?> addSpace(
            @RequestBody SpaceAddRequest addRequest,
            HttpServletRequest servletRequest) {
        if (ObjectUtils.isEmpty(addRequest)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loggedUser = userService.getLoginUser(servletRequest);
        long sid = spaceService.addSpace(addRequest, loggedUser);
        if (sid != -1) {
            return MrsResult.ok("空间创建成功", sid);
        }
        return MrsResult.failed("空间创建失败");
    }

    /**
     * 管理员更新空间信息
     *
     * @param updateRequest 更新数据的DTO封装
     * @return 是否更新成功
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = MrsUserRole.ADMIN)
    public MrsResult<?> updateSpace(@RequestBody SpaceUpdateRequest updateRequest) {
        boolean updated = spaceService.updateSpace(updateRequest);
        if (updated) {
            return MrsResult.ok("更新成功", updateRequest.getId());
        }
        return MrsResult.failed("更新失败");
    }

    /**
     * 根据ID删除一个空间
     * <p>用户也可用，但需要本人操作</p>
     *
     * @param deleteRequest  删除DTO对象
     * @param servletRequest HttpServlet请求对象
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = MrsUserRole.USER)
    public MrsResult<?> deleteById(
            @RequestBody DeleteRequest deleteRequest,
            HttpServletRequest servletRequest) {
        if (ObjectUtils.isEmpty(deleteRequest)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(servletRequest);
        boolean deleted = spaceService.deleteSpaceById(deleteRequest, loginUser);
        if (deleted) {
            return MrsResult.ok("删除成功");
        }
        return MrsResult.failed("删除失败");
    }

    /**
     * 获取所有空间类型
     *
     * @return .
     */
    @GetMapping("/list/level")
    public MrsResult<?> listSpaceLevel() {
        List<SpaceLevelVo> collect = Arrays.stream(SpaceLevelEnum.values())
                .map(level -> new SpaceLevelVo(level.getValue(),
                        level.getText(),
                        level.getMaxCount(),
                        level.getMaxSize()))
                .collect(Collectors.toList());
        return MrsResult.ok(collect);
    }
}
