package cloud.marisa.picturebackend.service.impl;

import cloud.marisa.picturebackend.entity.dao.Space;
import cloud.marisa.picturebackend.entity.dao.User;
import cloud.marisa.picturebackend.entity.dto.space.SpaceAddRequest;
import cloud.marisa.picturebackend.entity.dto.space.SpaceUpdateRequest;
import cloud.marisa.picturebackend.enums.SpaceLevel;
import cloud.marisa.picturebackend.enums.UserRole;
import cloud.marisa.picturebackend.exception.BusinessException;
import cloud.marisa.picturebackend.exception.ErrorCode;
import cloud.marisa.picturebackend.mapper.SpaceMapper;
import cloud.marisa.picturebackend.service.IUserService;
import cloud.marisa.picturebackend.service.ISpaceService;
import cloud.marisa.picturebackend.util.EnumUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ObjectUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Marisa
 * @description 针对表【space(空间表)】的数据库操作Service实现
 * @createDate 2025-04-04 11:02:54
 */
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements ISpaceService {

    @Autowired
    private IUserService userService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private final Map<Long, Object> locksMap = new ConcurrentHashMap<>();

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
        if (StrUtil.isBlank(addRequest.getSpaceName())) {
            space.setSpaceName("未命名空间");
        }
        SpaceLevel spaceLevel = EnumUtil.fromValue(addRequest.getSpaceLevel(), SpaceLevel.class);
        if (spaceLevel == null) {
            space.setSpaceLevel(SpaceLevel.COMMON.getValue());
        }
        fillSpaceBySpaceLevel(space);
        validSpace(space, true);
        Long uid = loggedUser.getId();
        space.setId(uid);
        // 游客、封禁用户 -> 不能创建空间
        // 普通用户   -> 普通空间
        // 管理员用户  -> 所有空间
        boolean isUser = userService.hasPermission(loggedUser, UserRole.USER);
        boolean isAdmin = userService.hasPermission(loggedUser, UserRole.ADMIN);
        if (!isUser || (spaceLevel != SpaceLevel.COMMON && !isAdmin)) {
            throw new BusinessException(ErrorCode.AUTHORIZATION_ERROR, "暂无权限");
        }
        Object lock = locksMap.computeIfAbsent(uid, k -> new Object());
        // 这里可以进一步使用Redisson分布式锁
        synchronized (lock) {
            try {
                // 开启一个事务
                transactionTemplate.execute(status -> {
                    boolean exists = this.lambdaQuery().eq(Space::getUserId, uid).exists();
                    if (exists) {
                        throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户只能创建一个空间");
                    }
                    boolean saved = this.save(space);
                    if (!saved) {
                        throw new BusinessException(ErrorCode.OPERATION_ERROR);
                    }
                    return space.getId();
                });
                return -1L;
            } finally {
                locksMap.remove(uid);
            }
        }
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        SpaceLevel spaceLevel = EnumUtil.fromValue(space.getSpaceLevel(), SpaceLevel.class);
        if (spaceLevel == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未知的空间类型");
        }
        if (space.getMaxCount() == null) {
            space.setMaxCount(spaceLevel.getMaxCount());
        }
        if (space.getMaxSize() == null) {
            space.setMaxSize(spaceLevel.getMaxSize());
        }
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
        SpaceLevel spaceLevel = EnumUtil.fromValue(space.getSpaceLevel(), SpaceLevel.class);
        // TODO:感觉这里的逻辑有点问题？但鱼哥是这么写的
        // 需要创建空间
        if (isCreate) {
            if (StrUtil.isBlank(spaceName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            }
            if (spaceLevel == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "未知的空间类型");
            }
        }
        // 需要修改空间
        if (space.getSpaceLevel() != null && spaceLevel == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未知的空间类型");
        }
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 32) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
        }
    }
}




