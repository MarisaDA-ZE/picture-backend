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
import cloud.marisa.picturebackend.exception.ThrowUtils;
import cloud.marisa.picturebackend.mapper.SpaceMapper;
import cloud.marisa.picturebackend.service.ISpaceUserService;
import cloud.marisa.picturebackend.service.IUserService;
import cloud.marisa.picturebackend.service.ISpaceService;
import cloud.marisa.picturebackend.util.EnumUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.log4j.Log4j2;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 当有循环依赖的地方时，不要使用构造函数注入，此时应该使用属性注入或者Setter注入
 *
 * @author Marisa
 * @description 针对表【space(空间表)】的数据库操作Service实现
 * @createDate 2025-04-04 11:02:54
 */
@Log4j2
@Service
public class SpaceServiceImpl
        extends ServiceImpl<SpaceMapper, Space>
        implements ISpaceService {

    /**
     * 用户服务
     */
    @Resource
    private IUserService userService;

    /**
     * 团队空间服务
     */
    @Lazy
    @Resource
    private ISpaceUserService spaceUserService;

    /**
     * 区域事务模板
     */
    @Resource
    private TransactionTemplate transactionTemplate;

    /**
     * Redisson分布式锁
     */
    @Resource
    private RedissonClient redissonClient;

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
        // 根据空间的分页信息，分离出用户ID
        Page<SpaceVo> voPage = new Page<>(current, size, total);
        List<Space> records = spacePage.getRecords();
        Set<Long> ids = records.stream()
                .map(Space::getUserId)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            voPage.setRecords(new ArrayList<>());
            return voPage;
        }
        // 根据用户ID，批量查询用户信息并转换成UserVo对象，最后按照UserId进行分组
        Map<Long, List<UserVo>> userVos = userService.listByIds(ids)
                .stream().map(User::toVO)
                .collect(Collectors.groupingBy(userVo -> (userVo != null) ? userVo.getId() : -1));
        // 往空间Vo对象列表中添加用户信息
        List<SpaceVo> spaceVos = records.stream().map(space -> {
            SpaceVo vo = SpaceVo.toVo(space);
            Long spaceUserId = space.getUserId();
            if (userVos.containsKey(spaceUserId)) {
                vo.setUser(userVos.get(space.getUserId()).get(0));
            } else {
                vo.setUser(null);
            }
            /* 空间图片权限，但这里不加好像也没事
             * Long userId = loggedUser.getId();
             * MrsUserRole userRole = EnumUtil.fromValue(loggedUser.getUserRole(), MrsUserRole.class);
             * List<String> permissions = MrsAuthUtil.getPermissions(spaceUserId, userId, userRole);
             * vo.setPermissionList(permissions);
             * */
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
        long spaceId = space.getId() == null ? 0L : space.getId();
        ThrowUtils.throwIf(spaceId < 0, ErrorCode.PARAMS_ERROR);
        // 判断空间是否存在
        boolean exists = this.lambdaQuery().eq(Space::getId, spaceId).exists();
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
        MrsSpaceType mst = EnumUtil.fromValue(addRequest.getSpaceType(), MrsSpaceType.class);
        MrsSpaceType spaceType = (mst == null) ? MrsSpaceType.PRIVATE : mst;
        space.setSpaceType(spaceType.getValue());
        // 填充空间等级信息
        fillSpaceBySpaceLevel(space);
        validSpace(space, true);
        Long uid = loggedUser.getId();
        // 游客、封禁用户 -> 不能创建空间
        // 普通用户 -> 普通空间
        // 管理员用户 -> 所有空间
        boolean isUser = userService.hasPermission(loggedUser, MrsUserRole.USER);
        boolean isAdmin = userService.hasPermission(loggedUser, MrsUserRole.ADMIN);
        if (!isUser || (spaceLevel != SpaceLevelEnum.COMMON && !isAdmin)) {
            throw new BusinessException(ErrorCode.AUTHORIZATION_ERROR, "暂无权限");
        }
        // 获取分布式锁
        String key = "space:create:lock:" + uid;
        log.info("获取分布式锁: {}", key);
        RLock lock = redissonClient.getLock(key);
        try {
            // 获取锁失败时最多等待5秒，15秒后锁自动释放，防止死锁
            if (!lock.tryLock(5, 15, TimeUnit.SECONDS)) {
                log.error("获取分布式锁失败 {}", key);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取分布式锁失败");
            }
            // 开启一个事务
            Long sid = transactionTemplate.execute(status -> {
                boolean exists = this.lambdaQuery()
                        .eq(Space::getUserId, uid)
                        .eq(Space::getSpaceType, spaceType.getValue())
                        .exists();
                if (exists) {
                    log.error("一个用户只能创建一个私人空间个一个团队空间");
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户只能创建一个空间");
                }
                boolean spaceSaved = this.save(space);
                // 创建的是团队空间，应该在 团队成员表 中创建相应记录
                if (spaceType == MrsSpaceType.TEAM) {
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
                // 分库分表，目前内容少所以不分，分了性能很差
                // 建议单表数据量200万以上再考虑分库分表
                // 但目前设计的数据量是50万以内
                // dynamicShardingManager.createSpacePictureTable(space);
                if (!spaceSaved) {
                    log.error("创建空间失败");
                    throw new BusinessException(ErrorCode.OPERATION_ERROR);
                }
                return space.getId();
            });
            return (sid != null) ? sid : -1L;
        } catch (InterruptedException e) {
            log.error("获取分布式锁失败, key=" + key, e);
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
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
        Long _id = deleteRequest.getId();
        Long spaceId = _id == null ? 0L : _id;
        if (spaceId < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Space space = this.getById(spaceId);
        if (space == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "空间不存在");
        }
        Long userId = loggedUser.getId();
        boolean isAdmin = userService.hasPermission(loggedUser, MrsUserRole.ADMIN);
        if (!userId.equals(space.getUserId()) && !isAdmin) {
            throw new BusinessException(ErrorCode.AUTHORIZATION_ERROR, "无空间访问权限");
        }
        return this.removeById(spaceId);
    }

    @Override
    public SpaceVo getSpaceVo(Space space) {
        if (space == null) {
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
        // 需要创建空间，参数都不应该为空
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
        // 部分参数如果不传，就沿用之前的
        // 但如果传了但解析失败，就是坏参数，应该抛出异常
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

    /**
     * 根据参数条件 构建一个查询Wrapper
     *
     * @param queryRequest 查询参数信息
     * @param loggedUser   当前登录用户
     * @return 构造好 的查询条件对象
     */
    LambdaQueryWrapper<Space> getQueryWrapper(SpaceQueryRequest queryRequest, User loggedUser) {
        LambdaQueryWrapper<Space> queryWrapper = new LambdaQueryWrapper<>();
        // 空间ID，等值匹配
        Long spaceId = queryRequest.getId();
        if (spaceId != null && spaceId > 0) {
            queryWrapper.eq(Space::getId, spaceId);
        }
        boolean isAdmin = userService.hasPermission(loggedUser, MrsUserRole.ADMIN);
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




