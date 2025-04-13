package cloud.marisa.picturebackend.service.impl;

import cloud.marisa.picturebackend.entity.dao.Space;
import cloud.marisa.picturebackend.entity.dao.SpaceUser;
import cloud.marisa.picturebackend.entity.dao.User;
import cloud.marisa.picturebackend.entity.dto.common.DeleteRequest;
import cloud.marisa.picturebackend.entity.dto.space.SpaceAddRequest;
import cloud.marisa.picturebackend.entity.dto.space.SpaceQueryRequest;
import cloud.marisa.picturebackend.entity.dto.space.SpaceUpdateRequest;
import cloud.marisa.picturebackend.entity.vo.SpaceVo;
import cloud.marisa.picturebackend.entity.vo.UserVo;
import cloud.marisa.picturebackend.enums.*;
import cloud.marisa.picturebackend.exception.BusinessException;
import cloud.marisa.picturebackend.exception.ErrorCode;
import cloud.marisa.picturebackend.mapper.SpaceMapper;
import cloud.marisa.picturebackend.service.ISpaceUserService;
import cloud.marisa.picturebackend.service.IUserService;
import cloud.marisa.picturebackend.service.ISpaceService;
import cloud.marisa.picturebackend.util.EnumUtil;
import cloud.marisa.picturebackend.util.MrsAuthUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author Marisa
 * @description 针对表【space(空间表)】的数据库操作Service实现
 * @createDate 2025-04-04 11:02:54
 */
@Service
public class SpaceServiceImpl
        extends ServiceImpl<SpaceMapper, Space>
        implements ISpaceService {

    @Autowired
    private IUserService userService;

    @Autowired
    private ISpaceUserService spaceUserService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private final Map<Long, Object> locksMap = new ConcurrentHashMap<>();

    @Override
    public Page<Space> getSpacePage(SpaceQueryRequest queryRequest, User loggedUser) {
        int current = queryRequest.getCurrent();
        int size = queryRequest.getPageSize();
        LambdaQueryWrapper<Space> queryWrapper = getQueryWrapper(queryRequest, loggedUser);
        return this.page(new Page<>(current, size), queryWrapper);
    }

    @Override
    public Page<SpaceVo> getSpacePageVo(Page<Space> spacePage, User loggedUser) {
        long size = spacePage.getSize();
        long current = spacePage.getCurrent();
        long total = spacePage.getTotal();

        Long userId = loggedUser.getId();
        UserRole userRole = EnumUtil.fromValue(loggedUser.getUserRole(), UserRole.class);
        Page<SpaceVo> voPage = new Page<>(current, size, total);
        List<Space> records = spacePage.getRecords();
        Set<Long> ids = records.stream()
                .map(Space::getUserId)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            voPage.setRecords(new ArrayList<>());
            return voPage;
        }
        Map<Long, List<UserVo>> userVos = userService.listByIds(ids)
                .stream().map(User::toVO)
                .collect(Collectors.groupingBy(userVo -> (userVo != null) ? userVo.getId() : -1));
        List<SpaceVo> spaceVos = records.stream().map(space -> {
            SpaceVo vo = SpaceVo.toVo(space);
            Long spaceUserId = space.getUserId();
            if (userVos.containsKey(spaceUserId)) {
                vo.setUser(userVos.get(space.getUserId()).get(0));
            } else {
                vo.setUser(null);
            }
            List<String> permissions = MrsAuthUtil.getPermissions(spaceUserId, userId, userRole);
            vo.setPermissionList(permissions);
            return vo;
        }).collect(Collectors.toList());
        voPage.setRecords(spaceVos);
        return voPage;
    }

    @Override
    public boolean updateSpace(SpaceUpdateRequest updateRequest) {
        Space space = new Space();
        BeanUtils.copyProperties(updateRequest, space);
        fillSpaceBySpaceLevel(space);
        validSpace(space, false);
        Long sid = space.getId();
        if (sid == null || sid < 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "空间不存在");
        }
        // 判断空间是否存在
        boolean exists = this.lambdaQuery().eq(Space::getId, sid).exists();
        if (!exists) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "空间不存在");
        }
        boolean updated = this.updateById(space);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return true;
    }

    @Override
    public long addSpace(SpaceAddRequest addRequest, User loggedUser) {
        Space space = new Space();
        BeanUtils.copyProperties(addRequest, space);
        space.setUserId(loggedUser.getId());
        if (StrUtil.isBlank(addRequest.getSpaceName())) {
            space.setSpaceName("未命名空间");
        }
        // 空间等级
        SpaceLevelEnum spaceLevel = EnumUtil.fromValue(addRequest.getSpaceLevel(), SpaceLevelEnum.class);
        if (spaceLevel == null) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        // 空间类型
        MrsSpaceType spaceType = EnumUtil.fromValue(addRequest.getSpaceType(), MrsSpaceType.class);
        if (spaceType == null) {
            space.setSpaceType(MrsSpaceType.PRIVATE.getValue());
        }
        fillSpaceBySpaceLevel(space);
        validSpace(space, true);
        Long uid = loggedUser.getId();
        // 游客、封禁用户 -> 不能创建空间
        // 普通用户   -> 普通空间
        // 管理员用户  -> 所有空间
        boolean isUser = userService.hasPermission(loggedUser, UserRole.USER);
        boolean isAdmin = userService.hasPermission(loggedUser, UserRole.ADMIN);
        if (!isUser || (spaceLevel != SpaceLevelEnum.COMMON && !isAdmin)) {
            throw new BusinessException(ErrorCode.AUTHORIZATION_ERROR, "暂无权限");
        }
        Object lock = locksMap.computeIfAbsent(uid, k -> new Object());
        // 这里可以进一步使用Redisson分布式锁
        synchronized (lock) {
            try {
                // 开启一个事务
                Long sid = transactionTemplate.execute(status -> {
                    MrsSpaceType sType = spaceType;
                    if (sType == null) {
                        sType = MrsSpaceType.PRIVATE;
                    }
                    boolean exists = this.lambdaQuery()
                            .eq(Space::getUserId, uid)
                            .eq(Space::getSpaceType, sType.getValue())
                            .exists();
                    if (exists) {
                        throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户只能创建一个空间");
                    }
                    boolean spaceSaved = this.save(space);
                    // 团队空间，创建相应记录
                    if (sType == MrsSpaceType.TEAM) {
                        SpaceUser spaceUser = new SpaceUser();
                        spaceUser.setUserId(uid);
                        spaceUser.setSpaceId(space.getId());
                        spaceUser.setSpaceRole(MrsSpaceRole.ADMIN.getValue());
                        boolean spaceUserSaved = spaceUserService.save(spaceUser);
                        if (!spaceUserSaved) {
                            log.error("创建团队成员记录失败");
                            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建团队成员记录失败");
                        }
                    }
                    if (!spaceSaved) {
                        log.error("创建空间失败");
                        throw new BusinessException(ErrorCode.OPERATION_ERROR);
                    }
                    return space.getId();
                });
                return (sid != null) ? sid : -1L;
            } finally {
                locksMap.remove(uid);
            }
        }
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        SpaceLevelEnum spaceLevelEnum = EnumUtil.fromValue(space.getSpaceLevel(), SpaceLevelEnum.class);
        if (spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未知的空间类型");
        }
        if (space.getMaxCount() == null) {
            space.setMaxCount(spaceLevelEnum.getMaxCount());
        }
        if (space.getMaxSize() == null) {
            space.setMaxSize(spaceLevelEnum.getMaxSize());
        }
    }

    @Override
    public boolean deleteSpaceById(DeleteRequest deleteRequest, User loggedUser) {
        Long spaceId = deleteRequest.getId();
        if (spaceId == null || spaceId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Space space = this.getById(spaceId);
        if (space == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "空间不存在");
        }
        Long userId = loggedUser.getId();
        boolean isAdmin = userService.hasPermission(loggedUser, UserRole.ADMIN);
        if (!Objects.equals(space.getUserId(), userId) && !isAdmin) {
            throw new BusinessException(ErrorCode.AUTHORIZATION_ERROR, "无空间访问权限");
        }
        return this.removeById(spaceId);
    }

    @Override
    public SpaceVo getSpaceVo(Space space) {
        if (space == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (ObjectUtils.isEmpty(space)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "空间不存在");
        }
        SpaceVo vo = SpaceVo.toVo(space);
        UserVo userVo = userService.getUserVoById(space.getUserId());
        vo.setUser(userVo);
        return vo;
    }

    /**
     * 校验空间信息
     *
     * @param space    空间DAO对象
     * @param isCreate 是否要创建空间
     */
    void validSpace(Space space, boolean isCreate) {
        if (ObjectUtils.isEmpty(space)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String spaceName = space.getSpaceName();
        SpaceLevelEnum spaceLevelEnum = EnumUtil.fromValue(space.getSpaceLevel(), SpaceLevelEnum.class);
        MrsSpaceType spaceTypeEnum = EnumUtil.fromValue(space.getSpaceType(), MrsSpaceType.class);
        // TODO:感觉这里的逻辑有点问题？但鱼哥是这么写的
        // 需要创建空间
        if (isCreate) {
            if (StrUtil.isBlank(spaceName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            }
            if (spaceLevelEnum == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "未知的空间类型");
            }
            if (spaceTypeEnum == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "未知的空间类型");
            }
        }
        // 需要修改空间
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 32) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
        }
        if (space.getSpaceLevel() != null && spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未知的空间类型");
        }
        if (space.getSpaceType() != null && spaceTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未知的空间类型");
        }
    }

    LambdaQueryWrapper<Space> getQueryWrapper(SpaceQueryRequest queryRequest, User loggedUser) {
        LambdaQueryWrapper<Space> queryWrapper = new LambdaQueryWrapper<>();
        // 空间ID，等值匹配
        Long spaceId = queryRequest.getId();
        if (spaceId != null && spaceId > 0) {
            queryWrapper.eq(Space::getId, spaceId);
        }

        boolean isAdmin = userService.hasPermission(loggedUser, UserRole.ADMIN);
        // 管理员可以查所有人的
        if (isAdmin) {
            // 用户ID，等值匹配
            Long userId = queryRequest.getUserId();
            if (userId != null && userId > 0) {
                queryWrapper.eq(Space::getUserId, userId);
            }
        } else {
            // 用户只能查自己的
            queryWrapper.eq(Space::getUserId, loggedUser.getId());
        }
        // 空间名称，模糊匹配
        String spaceName = queryRequest.getSpaceName();
        if (StrUtil.isNotBlank(spaceName)) {
            queryWrapper.like(Space::getSpaceName, spaceName);
        }
        // 空间等级，等值匹配
        Integer spaceLevel = queryRequest.getSpaceLevel();
        if (spaceLevel != null) {
            queryWrapper.eq(Space::getSpaceLevel, spaceLevel);
        }
        // 空间类型，等值匹配
        Integer spaceType = queryRequest.getSpaceType();
        if (spaceType != null) {
            queryWrapper.eq(Space::getSpaceType, spaceType);
        }
        // 排序字段
        String sortField = queryRequest.getSortField();
        if (StrUtil.isNotBlank(sortField)) {
            SortEnum sortType = EnumUtil.fromValue(queryRequest.getSortOrder(), SortEnum.class);
            boolean isAsc = sortType == SortEnum.ASC;
            queryWrapper.orderBy(true, isAsc, getSortField(queryRequest.getSortField()));
        }
        return queryWrapper;
    }

    /**
     * 获取可排序字段
     *
     * @param sortField 字段名
     * @return 对应的字段
     */
    private SFunction<Space, ?> getSortField(String sortField) {
        switch (sortField) {
            case "spaceName":
                return Space::getSpaceName;
            case "spaceLevel":
                return Space::getSpaceLevel;
            case "maxSize":
                return Space::getMaxSize;
            case "maxCount":
                return Space::getMaxCount;
            case "totalCount":
                return Space::getTotalCount;
            case "totalSize":
                return Space::getTotalSize;
            case "createTime":
                return Space::getCreateTime;
            case "updateTime":
                return Space::getUpdateTime;
            case "editTime":
                return Space::getEditTime;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "未知的排序字段");
        }
    }
}




