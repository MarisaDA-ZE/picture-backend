package cloud.marisa.picturebackend.service.impl;

import cloud.marisa.picturebackend.entity.dao.Space;
import cloud.marisa.picturebackend.entity.dao.SpaceUser;
import cloud.marisa.picturebackend.entity.dao.User;
import cloud.marisa.picturebackend.entity.dto.common.DeleteRequest;
import cloud.marisa.picturebackend.entity.dto.spaceuser.SpaceUserAddRequest;
import cloud.marisa.picturebackend.entity.dto.spaceuser.SpaceUserEditRequest;
import cloud.marisa.picturebackend.entity.dto.spaceuser.SpaceUserQueryRequest;
import cloud.marisa.picturebackend.entity.vo.SpaceUserVo;
import cloud.marisa.picturebackend.entity.vo.SpaceVo;
import cloud.marisa.picturebackend.entity.vo.UserVo;
import cloud.marisa.picturebackend.enums.MrsSpaceRole;
import cloud.marisa.picturebackend.exception.BusinessException;
import cloud.marisa.picturebackend.exception.ErrorCode;
import cloud.marisa.picturebackend.exception.ThrowUtils;
import cloud.marisa.picturebackend.mapper.SpaceUserMapper;
import cloud.marisa.picturebackend.service.ISpaceService;
import cloud.marisa.picturebackend.service.IUserService;
import cloud.marisa.picturebackend.service.ISpaceUserService;
import cloud.marisa.picturebackend.util.EnumUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 当有循环依赖的地方时，不要使用构造函数注入，此时应该使用属性注入或者Setter注入
 *
 * @author Marisa
 * @description 针对表【space_user(空间-用户的关联表)】的数据库操作Service实现
 * @createDate 2025-04-13 16:46:09
 */
@Log4j2
@Service
public class SpaceUserServiceImpl
        extends ServiceImpl<SpaceUserMapper, SpaceUser>
        implements ISpaceUserService {

    /**
     * 用户服务
     */
    @Resource
    private IUserService userService;

    /**
     * 个人空间服务
     */
    @Lazy
    @Resource
    private ISpaceService spaceService;

    @Override
    public SpaceUser getSpaceUser(
            SpaceUserQueryRequest queryRequest,
            User loginUser) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 校验参数
        validSpaceUser(queryRequest);
        // 组装查询条件，并返回满足条件的成员信息
        LambdaQueryWrapper<SpaceUser> queryWrapper = getQueryWrapper(queryRequest);
        return this.getOne(queryWrapper);
    }

    @Override
    public SpaceUserVo getSpaceUserVo(SpaceUser spaceUser) {
        SpaceUserVo spaceUserVo = SpaceUserVo.toVo(spaceUser);
        // 获取用户信息
        Long userId = spaceUser.getUserId();
        if (userId != null && userId > 0) {
            UserVo userVo = userService.getUserVoByIdCache(userId);
            spaceUserVo.setUser(userVo);
        }
        // 获取空间信息
        Long spaceId = spaceUser.getSpaceId();
        if (spaceId != null && spaceId > 0) {
            Space space = spaceService.getById(spaceId);
            // 这里还会查一次库
            SpaceVo spaceVo = spaceService.getSpaceVo(space);
            spaceUserVo.setSpace(spaceVo);
        }
        return spaceUserVo;
    }

    @Override
    public List<SpaceUser> getSpaceUserList(
            SpaceUserQueryRequest queryRequest,
            User loginUser) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 校验参数
        validSpaceUser(queryRequest);
        // 构造查询条件并返回符合条件的团队空间成员列表
        LambdaQueryWrapper<SpaceUser> queryWrapper = getQueryWrapper(queryRequest);
        return this.list(queryWrapper);
    }

    @Override
    public List<SpaceUserVo> getSpaceUserVoList(List<SpaceUser> spaceUsers) {
        if (spaceUsers == null || spaceUsers.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> userIdSet = new HashSet<>();
        Set<Long> spaceIdSet = new HashSet<>();
        // 先将团队空间成员对象用流的方式转换成团队空间成员Vo对象
        List<SpaceUserVo> voList = spaceUsers.stream()
                .map(spaceUser -> {
                    Long userId = spaceUser.getUserId();
                    if (userId != null) userIdSet.add(userId);
                    Long spaceId = spaceUser.getSpaceId();
                    if (spaceId != null) spaceIdSet.add(spaceId);
                    return SpaceUserVo.toVo(spaceUser);
                })
                .collect(Collectors.toList());
        // 批量查询用户并按用户ID分组（UserId-User）
        Map<Long, List<User>> usersMap = userService.listByIds(userIdSet)
                .stream()
                .collect(Collectors.groupingBy(User::getId));
        // 批量查询空间并按空间ID分组（SpaceId-Space）
        Map<Long, List<Space>> spacesMap = spaceService.listByIds(spaceIdSet)
                .stream()
                .collect(Collectors.groupingBy(Space::getId));
        // 组装结果并返回
        voList.forEach(vo -> {
            Long userId = vo.getUserId();
            if (userId != null && userId > 0) {
                User user = usersMap.get(userId).get(0);
                vo.setUser(UserVo.toVO(user));
            }
            Long spaceId = vo.getSpaceId();
            if (spaceId != null && spaceId > 0) {
                Space space = spacesMap.get(spaceId).get(0);
                // 列表中就没必要把空间对应的用户都查出来了
                // 很耗费性能，不如让用户点详情的时候再针对性的查库
                vo.setSpace(SpaceVo.toVo(space));
            }
        });
        return voList;
    }

    @Override
    public long addSpaceUser(
            SpaceUserAddRequest addRequest,
            User loginUser) {
        ThrowUtils.throwIf(addRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        log.info("添加一名团队成员: {}", addRequest);
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(addRequest, spaceUser);
        Long spaceId = spaceUser.getSpaceId();
        String roleString = spaceUser.getSpaceRole();
        if (roleString == null && spaceId != null) {
            SpaceUser dbSpaceUser = this.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceUser.getSpaceId())
                    .eq(SpaceUser::getUserId, loginUser.getId())
                    .one();
            spaceUser.setSpaceRole(dbSpaceUser.getSpaceRole());
        }
        // TODO: 这里要根据不同的创建者等级，限制团队人数

        validSpaceUser(spaceUser, true);
        boolean saved = this.save(spaceUser);
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return spaceUser.getId();
    }

    @Override
    public boolean editSpaceUser(SpaceUserEditRequest editRequest, User loginUser) {
        if (editRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(editRequest, spaceUser);
        validSpaceUser(spaceUser, false);
        Long spaceUserId = editRequest.getId();
        if (spaceUserId == null || spaceUserId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SpaceUser dbSpaceUser = this.getById(spaceUserId);
        if (dbSpaceUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "团队成员不存在");
        }
        return this.updateById(spaceUser);
    }

    @Override
    public boolean deleteSpaceUserById(
            DeleteRequest deleteRequest,
            User loggedUser) {
        Long id = deleteRequest.getId();
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SpaceUser spaceUser = this.getById(id);
        if (spaceUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return this.removeById(id);
    }

    /**
     * 校验空间-用户参数信息
     *
     * @param queryRequest 空间-用户对象
     */
    private void validSpaceUser(SpaceUserQueryRequest queryRequest) {
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(queryRequest, spaceUser);
        validSpaceUser(spaceUser, false);
    }

    /**
     * 校验空间-用户参数信息
     *
     * @param spaceUser 空间-用户对象
     * @param isCreate  是否为创建时
     */
    private void validSpaceUser(SpaceUser spaceUser, boolean isCreate) {
        Long spaceId = spaceUser.getSpaceId();
        Long userId = spaceUser.getUserId();
        String roleString = spaceUser.getSpaceRole();
        MrsSpaceRole spaceRole = EnumUtil.fromValue(roleString, MrsSpaceRole.class);
        // 创建时
        if (isCreate) {
            log.info("团队空间用户信息: {}", spaceUser);
            ThrowUtils.throwIf(spaceRole == null,
                    ErrorCode.PARAMS_ERROR, "未知的空间角色");
            if (spaceId == null || spaceId <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间ID不能为空");
            }
            if (userId == null || userId <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
            }
            return;
        }
        // 修改时
        if (spaceId != null && spaceId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间ID不能为空");
        }
        if (userId != null && userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }
        if (StrUtil.isNotBlank(roleString) && spaceRole == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未知的空间角色");
        }
        // TODO：校验该空间是否已经添加了该成员，但需要查DB 或 缓存
    }

    /**
     * 根据查询请求，组装查询条件
     *
     * @param queryRequest 查询参数DTO封装
     * @return 查询条件对象
     */
    private LambdaQueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest queryRequest) {
        LambdaQueryWrapper<SpaceUser> queryWrapper = new LambdaQueryWrapper<>();
        // 主键ID
        Long id = queryRequest.getId();
        if (id != null && id > 0) {
            queryWrapper.eq(SpaceUser::getId, id);
        }
        // 用户ID
        Long userId = queryRequest.getUserId();
        if (userId != null && userId > 0) {
            queryWrapper.eq(SpaceUser::getUserId, userId);
        }
        // 空间ID
        Long spaceId = queryRequest.getSpaceId();
        if (spaceId != null && spaceId > 0) {
            queryWrapper.eq(SpaceUser::getSpaceId, spaceId);
        }
        // 角色
        String role = queryRequest.getSpaceRole();
        if (StrUtil.isNotBlank(role)) {
            queryWrapper.eq(SpaceUser::getSpaceRole, role);
        }
        return queryWrapper;
    }
}




