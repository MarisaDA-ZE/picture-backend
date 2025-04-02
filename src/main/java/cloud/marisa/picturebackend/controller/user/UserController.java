package cloud.marisa.picturebackend.controller.user;

import cloud.marisa.picturebackend.annotations.AuthCheck;
import cloud.marisa.picturebackend.common.MrsResult;
import cloud.marisa.picturebackend.entity.dao.User;
import cloud.marisa.picturebackend.entity.dto.common.DeleteRequest;
import cloud.marisa.picturebackend.entity.dto.user.*;
import cloud.marisa.picturebackend.entity.vo.UserVo;
import cloud.marisa.picturebackend.enums.UserRole;
import cloud.marisa.picturebackend.exception.BusinessException;
import cloud.marisa.picturebackend.exception.ErrorCode;
import cloud.marisa.picturebackend.service.IUserService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;



/**
 * @author MarisaDAZE
 * @description 用户控制层
 * @date 2025/3/28
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private IUserService userService;

    /**
     * 用户注册
     *
     * @param registerRequest 注册DTO对象
     * @return 用户ID
     */
    @PostMapping("/register")
    MrsResult<?> register(@RequestBody AccountRegisterRequest registerRequest) {
        System.out.println(registerRequest);
        long id = userService.register(registerRequest);
        return MrsResult.ok(id);
    }

    /**
     * 用户登录
     *
     * @param loginRequest   登录DTO对象
     * @param servletRequest HTTP请求体
     * @return 用户VO
     */
    @PostMapping("/login")
    MrsResult<?> login(@RequestBody AccountLoginRequest loginRequest, HttpServletRequest servletRequest) {
        System.out.println(loginRequest);
        UserVo userVo = userService.login(loginRequest, servletRequest);
        return MrsResult.ok(userVo);
    }

    /**
     * 根据session_id获取用户登录信息
     *
     * @param servletRequest HTTP请求体
     * @return 用户VO
     */
    @GetMapping("/get/login")
    MrsResult<?> getLoginUser(HttpServletRequest servletRequest) {
        User loginUser = userService.getLoginUser(servletRequest);
        return MrsResult.ok(User.toVO(loginUser));
    }

    /**
     * 用户登出
     *
     * @param servletRequest HTTP请求体
     * @return 登出是否成功
     */
    @PostMapping("/logout")
    MrsResult<?> logout(HttpServletRequest servletRequest) {
        if (ObjectUtils.isEmpty(servletRequest)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean logout = userService.logout(servletRequest);
        return MrsResult.ok(logout);
    }

    /**
     * 管理员新建用户
     *
     * @param createUserRequest 新建DTO对象
     * @return 用户VO
     */
    @PutMapping("/add")
    @AuthCheck(mustRole = UserRole.ADMIN)
    public MrsResult<?> add(@RequestBody CreateUserRequest createUserRequest) {
        UserVo userVo = userService.createUser(createUserRequest);
        return MrsResult.ok(userVo);
    }

    /**
     * 管理员更新用户信息
     *
     * @param updateUserRequest 更新DTO对象
     * @return 用户VO
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserRole.ADMIN)
    public MrsResult<?> update(@RequestBody UpdateUserRequest updateUserRequest) {
        UserVo userVo = userService.updateUser(updateUserRequest);
        return MrsResult.ok(userVo);
    }

    /**
     * 管理员按ID查询用户信息
     *
     * @param id 用户ID
     * @return 用户VO分页对象
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserRole.ADMIN)
    public MrsResult<?> getById(@RequestParam Long id) {
        if (ObjectUtils.isEmpty(id)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "id不能为空");
        }
        User user = userService.getById(id);
        if (ObjectUtils.isEmpty(user)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return MrsResult.ok(user);
    }

    /**
     * 按ID查询用户信息
     *
     * @param id 用户ID
     * @return 用户VO分页对象
     */
    @GetMapping("/get/vo")
    public MrsResult<?> getVo(@RequestParam Long id) {
        UserVo userVo = userService.getUserVoById(id);
        return MrsResult.ok(userVo);
    }

    /**
     * 管理员查询用户信息
     * <p>以post方式的查询</p>
     *
     * @param queryUserRequest 查询DTO对象
     * @return 用户VO分页对象
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserRole.ADMIN)
    public MrsResult<?> queryPage(@RequestBody QueryUserRequest queryUserRequest) {
        Page<UserVo> pages = userService.queryUserPage(queryUserRequest);
        return MrsResult.ok(pages);
    }

    /**
     * 管理员根据ID删除一个用户
     *
     * @param deleteRequest 删除DTO对象
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserRole.ADMIN)
    public MrsResult<?> deleteById(@RequestBody DeleteRequest deleteRequest) {
        userService.deleteById(deleteRequest.getId());
        return MrsResult.ok("删除成功");
    }

    /**
     * 管理员根据ID列表删除多个用户
     *
     * @param deleteRequest 删除DTO对象
     * @return 是否删除成功
     */
    @PostMapping("/delete-batch")
    @AuthCheck(mustRole = UserRole.ADMIN)
    public MrsResult<?> deleteByIds(@RequestBody DeleteRequest deleteRequest) {
        userService.batchDeleteByIds(deleteRequest.getBatchIds());
        return MrsResult.ok("删除成功");
    }
}
