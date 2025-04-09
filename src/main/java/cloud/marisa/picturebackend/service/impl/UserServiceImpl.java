package cloud.marisa.picturebackend.service.impl;

import cloud.marisa.picturebackend.entity.dao.User;
import cloud.marisa.picturebackend.entity.dto.user.*;
import cloud.marisa.picturebackend.entity.vo.UserVo;
import cloud.marisa.picturebackend.enums.SortEnum;
import cloud.marisa.picturebackend.enums.UserRole;
import cloud.marisa.picturebackend.exception.BusinessException;
import cloud.marisa.picturebackend.exception.ErrorCode;
import cloud.marisa.picturebackend.mapper.UserMapper;
import cloud.marisa.picturebackend.service.IUserService;
import cloud.marisa.picturebackend.util.EnumUtil;
import cloud.marisa.picturebackend.util.RandomUtil;
import cloud.marisa.picturebackend.util.SessionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.servlet.http.HttpServletRequest;
import java.lang.Object;

import java.util.List;

import static cloud.marisa.picturebackend.common.Constants.USER_LOGIN;

/**
 * @author Marisa
 * @description 针对表【user(用户表)】的数据库操作Service实现
 * @createDate 2025-03-28 15:58:26
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements IUserService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public long register(AccountRegisterRequest request) {
        System.out.println(request);
        // 校验请求参数
        checkParamsRegister(request);

        // 检查是否重复
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUserAccount, request.getUserAccount());
        long count = baseMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在");
        }
        // 创建用户对象
        User user = new User();
        user.setUserAccount(request.getUserAccount());
        String encoded = passwordEncoder.encode(request.getUserPassword());
        user.setUserName("用户_" + RandomUtil.getRandomString(6));
        user.setUserPassword(encoded);
        user.setUserRole(UserRole.USER.getValue());
        // 保存用户
        int insert = baseMapper.insert(user);
        if (insert <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "注册失败，数据库错误");
        }
        return user.getId();
    }

    @Override
    public UserVo login(AccountLoginRequest loginRequest, HttpServletRequest servletRequest) {
        checkParamsLogin(loginRequest);
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUserAccount, loginRequest.getUserAccount());
        User dbUser = this.getOne(queryWrapper);
        if (dbUser == null || !passwordEncoder.matches(loginRequest.getUserPassword(), dbUser.getUserPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码错误");
        }
        // 设置session
        // 通过setAttribute时，前端拿到的只是一个session_id，真正的数据在服务器，因此不用担心数据篡改
        servletRequest.getSession().setAttribute(USER_LOGIN, dbUser);

        // 返回VO对象
        return User.toVO(dbUser);
    }

    @Override
    public User getLoginUser(HttpServletRequest servletRequest) {
        return SessionUtil.getLoginUser(servletRequest);
    }

    @Override
    public boolean logout(HttpServletRequest servletRequest) {
        Object session = servletRequest.getSession().getAttribute(USER_LOGIN);
        User currentUser = (User) session;
        if (ObjectUtils.isEmpty(session) || ObjectUtils.isEmpty(currentUser.getId())) {
            throw new BusinessException(ErrorCode.AUTHORIZATION_ERROR, "未登录");
        }
        // 删除session
        servletRequest.getSession().removeAttribute(USER_LOGIN);
        return true;
    }

    @Override
    public UserVo createUser(CreateUserRequest createUserRequest) {
        // 校验请求参数
        checkParamsCreate(createUserRequest);

        User user = new User();
        BeanUtils.copyProperties(createUserRequest, user);
        // 使用BCrypt加密
        String encoded = passwordEncoder.encode(createUserRequest.getUserPassword());
        user.setUserPassword(encoded);

        boolean saved = this.save(user);
        if (!saved) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "创建失败，数据库错误。");
        }
        return User.toVO(user);
    }

    @Override
    public UserVo updateUser(UpdateUserRequest updateUserRequest) {
        // 校验请求参数
        checkParamsUpdate(updateUserRequest);

        User dbUser = this.getById(updateUserRequest.getId());
        if (dbUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        Long id = dbUser.getId();
        String password = dbUser.getUserPassword();

        // 有密码就把 明文密码 加密，没密码就用原来的
        if (StrUtil.isNotBlank(updateUserRequest.getUserPassword())) {
            password = passwordEncoder.encode(updateUserRequest.getUserPassword());
        }
        // 拷贝值
        BeanUtils.copyProperties(updateUserRequest, dbUser);
        dbUser.setId(id);   // id 不能被修改
        dbUser.setUserPassword(password);   // 更新密码

        boolean updated = this.updateById(dbUser);
        if (!updated) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "修改失败，数据库错误。");
        }
        return User.toVO(dbUser);
    }

    @Override
    public Page<UserVo> queryUserPage(QueryUserRequest queryUserRequest) {
        LambdaQueryWrapper<User> queryWrapper = getQueryWrapper(queryUserRequest);
        if (queryWrapper == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询条件不能为空");
        }
        int current = queryUserRequest.getCurrent();
        int size = queryUserRequest.getPageSize();
        Page<User> pageResult = this.page(new Page<>(current, size), queryWrapper);
        Page<UserVo> result = new Page<>(current, size, pageResult.getTotal());
        result.setRecords(User.toVoList(pageResult.getRecords()));
        return result;
    }

    @Override
    public UserVo getUserVoById(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "id不能为空");
        }
        User dbUser = this.getById(id);
        if (ObjectUtils.isEmpty(dbUser)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return User.toVO(dbUser);
    }

    @Override
    public boolean deleteById(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "id不能为空");
        }
        boolean removed = this.removeById(id);
        if (!removed) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "删除失败，数据库错误。");
        }
        return true;
    }

    @Override
    public boolean batchDeleteByIds(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "id不能为空");
        }
        boolean removed = this.removeByIds(ids);
        if (!removed) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "删除失败，数据库错误。");
        }
        return true;
    }

    @Override
    public boolean hasPermission(User user, UserRole hasRole) {
        return SessionUtil.hasPermission(user, hasRole);
    }

    /**
     * 构造一个查询条件
     *
     * @param request 查询请求DTO对象
     * @return 查询条件
     */
    private LambdaQueryWrapper<User> getQueryWrapper(QueryUserRequest request) {
        if (ObjectUtils.isEmpty(request)) {
            return null;
        }

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        // id不为空
        if (!ObjectUtils.isEmpty(request.getId())) {
            queryWrapper.eq(User::getId, request.getId());
        }

        // 账号不为空（模糊匹配）
        if (StrUtil.isNotBlank(request.getUserAccount())) {
            queryWrapper.like(User::getUserAccount, request.getUserAccount());
        }

        // 角色不为空
        if (!ObjectUtils.isEmpty(request.getUserRole())) {
            UserRole userRole = EnumUtil.fromValue(request.getUserRole(), UserRole.class);
            // 角色不存在，可能是非法访问
            if (userRole == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "未知的角色");
            }
            queryWrapper.eq(User::getUserRole, userRole.getValue());
        }

        // 昵称不为空（模糊匹配）
        if (StrUtil.isNotBlank(request.getUserName())) {
            queryWrapper.like(User::getUserName, request.getUserName());
        }

        // 手机号不为空（模糊匹配）
        if (StrUtil.isNotBlank(request.getPhone())) {
            queryWrapper.like(User::getPhone, request.getPhone());
        }

        // 邮箱不为空（模糊匹配）
        if (StrUtil.isNotBlank(request.getEmail())) {
            queryWrapper.like(User::getEmail, request.getEmail());
        }

        // 需要排序时
        if (StrUtil.isNotBlank(request.getSortField())) {
            SortEnum sortType = EnumUtil.fromValue(request.getSortOrder(), SortEnum.class);
            boolean isAsc = sortType == SortEnum.ASC;
            queryWrapper.orderBy(true, isAsc, getSortField(request.getSortField()));
        }
        System.out.println(queryWrapper.getSqlSegment());
        return queryWrapper;
    }

    /**
     * User对象可排序字段和其的对应关系
     * <p>这么做好也不好</p>
     * <p>好处是限制了前端的排序类型，只能根据规定的类型排序</p>
     * <p>坏处是对应关系后续可能会改变，如果有人修改了实体类和数据库，而没有改这里，就可能产生歧义</p>
     *
     * @param sortField 排序字段
     * @return 排序字段
     */
    private SFunction<User, ?> getSortField(String sortField) {
        switch (sortField) {
            case "id":
                return User::getId;
            case "account":
                return User::getUserAccount;
            case "userName":
                return User::getUserName;
            case "role":
                return User::getUserRole;
            case "phone":
                return User::getPhone;
            case "email":
                return User::getEmail;
            case "createTime":
                return User::getCreateTime;
            case "updateTime":
                return User::getUpdateTime;
            case "editTime":
                return User::getEditTime;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "未知的排序字段");
        }
    }

    /**
     * 校验注册参数
     *
     * @param request 请求体
     */
    private void checkParamsRegister(AccountRegisterRequest request) {
        checkAccount(request.getUserAccount());
        checkPassword(request.getUserPassword());
        if (!request.getUserPassword().equals(request.getCheckPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
    }

    /**
     * 校验登录参数
     *
     * @param request 请求体
     */
    private void checkParamsLogin(AccountLoginRequest request) {
        checkAccount(request.getUserAccount());
        checkPassword(request.getUserPassword());
    }

    /**
     * 校验新增参数（管理员可用）
     *
     * @param request 请求体
     */
    private void checkParamsCreate(CreateUserRequest request) {
        checkAccount(request.getUserAccount());
        checkPassword(request.getUserPassword());
        checkRole(request.getUserRole());
        if (StrUtil.isNotBlank(request.getUserName())) checkuserName(request.getUserName());
        if (StrUtil.isNotBlank(request.getPhone())) checkPhone(request.getPhone());
        if (StrUtil.isNotBlank(request.getEmail())) checkEmail(request.getEmail());
    }

    /**
     * 校验更新参数（管理员可用）
     *
     * @param request 请求体
     */
    private void checkParamsUpdate(UpdateUserRequest request) {
        if (StrUtil.isNotBlank(request.getUserAccount())) checkAccount(request.getUserAccount());
        if (StrUtil.isNotBlank(request.getUserPassword())) checkPassword(request.getUserPassword());
        if (StrUtil.isNotBlank(request.getUserRole())) checkRole(request.getUserRole());
        if (StrUtil.isNotBlank(request.getUserName())) checkuserName(request.getUserName());
        if (StrUtil.isNotBlank(request.getPhone())) checkPhone(request.getPhone());
        if (StrUtil.isNotBlank(request.getEmail())) checkEmail(request.getEmail());
    }

    /**
     * 校验账户名
     *
     * @param account 账户名
     */
    private void checkAccount(String account) {
        if (StrUtil.isBlank(account)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能为空");
        }
        if (account.length() < 6 || account.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度必须在6-20位之间");
        }
        if (!account.matches("^[a-zA-Z0-9_]+$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号只能包含数字、字母和下划线");
        }
    }

    /**
     * 校验密码
     *
     * @param password 密码
     */
    private void checkPassword(String password) {
        if (StrUtil.isBlank(password)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能为空");
        }
        if (password.length() < 6 || password.length() > 24) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度必须在6-24位之间");
        }
        if (!password.matches("^[a-zA-Z0-9_@#$%^&*()?.]+$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码只能包含数字、字母以及_@#$%^&*()?.特殊符号");
        }
    }

    /**
     * 校验角色
     *
     * @param role 角色
     */
    private void checkRole(String role) {
        if (StrUtil.isBlank(role)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未知的角色");
        }
        UserRole userRole = EnumUtil.fromValue(role, UserRole.class);
        if (userRole == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未知的角色");
        }

    }

    /**
     * 校验昵称规则
     *
     * @param userName 昵称
     */
    private void checkuserName(String userName) {
        if (StrUtil.isBlank(userName)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "昵称不能为空");
        }
        if (userName.length() < 4 || userName.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "昵称长度必须在4-20位之间");
        }
    }

    /**
     * 校验是否为大陆手机号
     *
     * @param phone 手机号
     */
    private void checkPhone(String phone) {
        if (StrUtil.isBlank(phone)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号不能为空");
        }
        if (phone.length() != 11) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "非大陆手机号");
        }
        if (!phone.matches("^1(3[0-9]|5[0-3,5-9]|7[1-3,5-8]|8[0-9])\\d{8}$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未知的手机号");
        }
    }

    /**
     * 校验电子邮箱地址
     *
     * @param email 邮箱地址
     */
    private void checkEmail(String email) {
        if (StrUtil.isBlank(email)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱不能为空");
        }
        if (email.length() > 48) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱号过长");
        }
        if (!email.matches("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "错误的邮箱号");
        }
    }
}