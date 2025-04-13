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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Marisa
 * @description 针对表【space_user(空间-用户的关联表)】的数据库操作Service实现
 * @createDate 2025-04-13 16:46:09
 */
@Service
public class SpaceUserServiceImpl
        extends ServiceImpl<SpaceUserMapper, SpaceUser>
        implements ISpaceUserService {

    @Autowired
    private IUserService userService;

    @Autowired
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
        LambdaQueryWrapper<SpaceUser> queryWrapper = new LambdaQueryWrapper<>();
        // 填充查询条件
        getQueryWrapper(queryRequest, queryWrapper);
        return this.getOne(queryWrapper);
    }

    @Override
    public SpaceUserVo getSpaceUserVo(SpaceUser spaceUser) {
        SpaceUserVo vo = SpaceUserVo.toVo(spaceUser);
        Long userId = spaceUser.getUserId();
        if (userId != null && userId > 0) {
            UserVo userVo = userService.getUserVoById(userId);
            vo.setUser(userVo);
        }
        Long spaceId = spaceUser.getSpaceId();
        if (spaceId != null && spaceId > 0) {
            Space space = spaceService.getById(spaceId);
            SpaceVo spaceVo = spaceService.getSpaceVo(space);
            vo.setSpace(spaceVo);
        }
        return vo;
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
        LambdaQueryWrapper<SpaceUser> queryWrapper = new LambdaQueryWrapper<>();
        return this.list(queryWrapper);
    }

    @Override
    public List<SpaceUserVo> getSpaceUserVoList(List<SpaceUser> spaceUsers) {
        if (spaceUsers == null || spaceUsers.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> userIdSet = new HashSet<>();
        Set<Long> spaceIdSet = new HashSet<>();
        List<SpaceUserVo> voList = spaceUsers.stream()
                .map(spaceUser -> {
                    Long userId = spaceUser.getUserId();
                    if (userId != null) userIdSet.add(userId);
                    Long spaceId = spaceUser.getSpaceId();
                    if (spaceId != null) spaceIdSet.add(spaceId);
                    return SpaceUserVo.toVo(spaceUser);
                })
                .collect(Collectors.toList());
        // 批量查询用户
        Map<Long, List<User>> usersMap = userService.listByIds(userIdSet)
                .stream()
                .collect(Collectors.groupingBy(User::getId));
        // 批量查询空间
        Map<Long, List<Space>> spacesMap = spaceService.listByIds(spaceIdSet)
                .stream()
                .collect(Collectors.groupingBy(Space::getId));
        // 组装结果
        voList.forEach(vo -> {
            Long userId = vo.getUserId();
            if (userId != null && userId > 0) {
                User user = usersMap.get(userId).get(0);
                vo.setUser(User.toVO(user));
            }
            Long spaceId = vo.getSpaceId();
            if (spaceId != null && spaceId > 0) {
                Space space = spacesMap.get(spaceId).get(0);
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
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(addRequest, spaceUser);
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
        Long id = editRequest.getId();
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SpaceUser dbSpaceUser = this.getById(id);
        if(dbSpaceUser == null){
            throw new BusinessException(ErrorCode.NOT_FOUND);
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

    @Override
    public void fillSpaceBySpaceLevel(SpaceUser spaceUser) {

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
        // TODO：校验该空间是否已经添加了该成员，但需要查DB　或　缓存
    }

    /**
     * 获取查询条件
     *
     * @param queryRequest 查询参数DTO封装
     * @param queryWrapper 查询条件对象
     */
    private void getQueryWrapper(
            SpaceUserQueryRequest queryRequest,
            LambdaQueryWrapper<SpaceUser> queryWrapper) {
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
    }


}




