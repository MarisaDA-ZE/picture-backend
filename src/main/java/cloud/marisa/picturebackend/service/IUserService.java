package cloud.marisa.picturebackend.service;

import cloud.marisa.picturebackend.entity.dao.User;
import cloud.marisa.picturebackend.entity.dto.user.*;
import cloud.marisa.picturebackend.entity.vo.UserVo;
import cloud.marisa.picturebackend.enums.MrsUserRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author Marisa
 * @description 针对表【user(用户表)】的数据库操作Service
 * @createDate 2025-03-28 15:58:26
 */
public interface IUserService extends IService<User> {

    /**
     * 用户注册接口
     *
     * @param registerRequest 注册DTO对象
     * @return 用户ID
     */
    long register(AccountRegisterRequest registerRequest);

    /**
     * 用户登录接口
     *
     * @param loginRequest   登录DTO对象
     * @param servletRequest http请求对象
     * @return 用户VO
     */
    UserVo login(AccountLoginRequest loginRequest, HttpServletRequest servletRequest);

    /**
     * 获取当前登录用户信息(从session中)
     * <p>返回登录用户对象 或 报错</p>
     *
     * @param servletRequest http请求对象
     * @return 当前用户
     */
    User getLoginUser(HttpServletRequest servletRequest);

    /**
     * 获取当前登录用户信息(从session中)
     * <p>如果没登录就会返回空</p>
     *
     * @param servletRequest http请求对象
     * @return 登录用户 或 null
     */
    User getLoginUserIfLogin(HttpServletRequest servletRequest);

    /**
     * 从httpServletRequest中获取当前用户的角色
     * <p>如果未登录，就拿到的是Guest角色</p>
     *
     * @param servletRequest http请求对象
     * @return 用户角色
     */
    MrsUserRole getCurrentUserRole(HttpServletRequest servletRequest);

    /**
     * 通过session退出登录
     *
     * @param servletRequest http请求对象
     * @return 是否退出成功
     */
    boolean logout(HttpServletRequest servletRequest);

    /**
     * 创建一个用户
     * <p>需要管理员及以上权限</p>
     *
     * @param createUserRequest 创建用户的DTO对象
     * @return 用户VO对象 或 null
     */
    UserVo createUser(CreateUserRequest createUserRequest);

    /**
     * 更新一个用户的信息
     *
     * @param updateUserRequest 更新用户的DTO对象
     * @return 用户VO对象 或 null
     */
    UserVo updateUser(UpdateUserRequest updateUserRequest);

    /**
     * 查询用户数据（可能为分页查）
     * <p>需要管理员及以上权限</p>
     *
     * @param queryUserRequest 查询用户的DTO对象
     * @return 用户VO列表 或 null
     */
    Page<UserVo> queryUserPage(QueryUserRequest queryUserRequest);

    /**
     * 根据ID查询一个用户信息
     * <p>需要管理员及以上权限</p>
     *
     * @param id 用户ID
     * @return 用户VO对象 或 null
     */
    UserVo getUserVoById(Long id);

    /**
     * 根据ID删除一个用户
     * <p>需要管理员及以上权限</p>
     *
     * @param id 用户ID
     * @return 是否删除成功
     */
    boolean deleteById(Long id);

    /**
     * 根据ID删除一组用户
     * <p>需要管理员及以上权限</p>
     *
     * @param ids 删除用户的列表
     * @return 用户VO列表 或 null
     */
    boolean batchDeleteByIds(List<Long> ids);

    /**
     * 检查用户是否具有权限
     * <p>当前用户的权限大于等于hasRole的权限则返回true，反之则为false</p>
     *
     * @param user    用户
     * @param hasRole 需要具有的权限
     * @return .
     */
    boolean hasPermission(User user, MrsUserRole hasRole);

    /**
     * 检查当前角色是否具有权限
     * <p>当前角色的权限大于等于hasRole的权限则返回true，反之则为false</p>
     *
     * @param currentRole 当前有的权限
     * @param hasRole     需要具有的权限
     * @return .
     */
    boolean hasPermission(MrsUserRole currentRole, MrsUserRole hasRole);
}
