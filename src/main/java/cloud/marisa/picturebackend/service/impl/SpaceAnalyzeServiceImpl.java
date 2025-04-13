package cloud.marisa.picturebackend.service.impl;

import cloud.marisa.picturebackend.entity.dao.Picture;
import cloud.marisa.picturebackend.entity.dao.Space;
import cloud.marisa.picturebackend.entity.dao.User;
import cloud.marisa.picturebackend.entity.dto.analyze.request.*;
import cloud.marisa.picturebackend.entity.dto.analyze.request.base.SpaceAnalyzeRequest;
import cloud.marisa.picturebackend.entity.dto.analyze.response.*;
import cloud.marisa.picturebackend.enums.MrsTimeDimension;
import cloud.marisa.picturebackend.enums.UserRole;
import cloud.marisa.picturebackend.exception.BusinessException;
import cloud.marisa.picturebackend.exception.ErrorCode;
import cloud.marisa.picturebackend.exception.ThrowUtils;
import cloud.marisa.picturebackend.mapper.PictureMapper;
import cloud.marisa.picturebackend.service.IPictureService;
import cloud.marisa.picturebackend.service.ISpaceAnalyzeService;
import cloud.marisa.picturebackend.service.ISpaceService;
import cloud.marisa.picturebackend.service.IUserService;
import cloud.marisa.picturebackend.util.EnumUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author MarisaDAZE
 * @description 空间分析服务实现类
 * @date 2025/4/12
 */
@Log4j2
@Service
public class SpaceAnalyzeServiceImpl implements ISpaceAnalyzeService {

    @Autowired
    private IUserService userService;

    @Autowired
    private IPictureService pictureService;
    @Autowired
    private PictureMapper pictureMapper;
    @Autowired
    private ISpaceService spaceService;

    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(
            SpaceUsageAnalyzeRequest analyzeRequest,
            User loginUser) {
        // 基础参数校验
        checkSpaceAnalyzeAuth(analyzeRequest, loginUser);
        // 公共空间或者全部空间的使用统计
        if (analyzeRequest.isQueryPublic() || analyzeRequest.isQueryAll()) {
            LambdaQueryWrapper<Picture> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.select(Picture::getPicSize);
            // 是否 只查公共空间
            if (analyzeRequest.isQueryPublic()) {
                queryWrapper.isNull(Picture::getSpaceId);
            }
            // 所有的图片大小
            List<Object> selectObjs = pictureService.getBaseMapper().selectObjs(queryWrapper);
            long totalCount = selectObjs.size();
            long totalSize = selectObjs.stream()
                    .mapToLong(size -> (size instanceof Long) ? (Long) size : 0L)
                    .sum();
            return SpaceUsageAnalyzeResponse.builder()
                    .usedSize(totalSize)
                    .usedCount(totalCount)
                    // 公共空间无大小限制
                    .maxSize(null)
                    .maxCount(null)
                    .sizeUsageRatio(null)
                    .countUsageRatio(null)
                    .build();
        }
        // 用户空间的使用统计
        Long spaceId = analyzeRequest.getSpaceId();
        ThrowUtils.throwIf(spaceId == null || spaceId < 0, ErrorCode.PARAMS_ERROR);
        Space dbSpace = spaceService.getById(spaceId);
        ThrowUtils.throwIf(dbSpace == null, ErrorCode.NOT_FOUND);
        // 统计私人空间使用情况
        Long totalSize = dbSpace.getTotalSize();
        Integer totalCount = dbSpace.getTotalCount();
        Long maxSize = dbSpace.getMaxSize();
        Integer maxCount = dbSpace.getMaxCount();
        double sizeUsageRatio = NumberUtil.round(totalSize * 100.0 / maxSize, 2)
                .doubleValue();
        double countUsageRatio = NumberUtil.round(totalCount * 100.0 / maxCount, 2)
                .doubleValue();
        return SpaceUsageAnalyzeResponse.builder()
                .usedSize(totalSize)
                .usedCount(totalCount.longValue())
                .maxSize(maxSize)
                .maxCount(maxCount.longValue())
                .sizeUsageRatio(sizeUsageRatio)
                .countUsageRatio(countUsageRatio)
                .build();
    }

    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(
            SpaceCategoryAnalyzeRequest analyzeRequest,
            User loginUser) {
        // 基础参数校验
        checkSpaceAnalyzeAuth(analyzeRequest, loginUser);
        // 查询图片分类统计
        List<SpaceCategoryAnalyzeResponse> result = pictureMapper
                .getSpaceCategoryAnalyze(analyzeRequest);
        result.forEach(analyze -> {
            String category = (analyze.getCategory() != null) ? analyze.getCategory() : "未分类";
            analyze.setCategory(category);
        });
        return result;
    }

    @Override
    public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(
            SpaceTagAnalyzeRequest analyzeRequest,
            User loginUser) {
        // 基础参数校验
        checkSpaceAnalyzeAuth(analyzeRequest, loginUser);
        LambdaQueryWrapper<Picture> queryWrapper = new LambdaQueryWrapper<>();
        // 构造查询条件
        fillLambdaQueryWrapper(analyzeRequest, queryWrapper);
        queryWrapper.select(Picture::getTags);
        // 查询图片标签统计,并转换为Map（tag -> count）
        Map<String, Long> collect = pictureService.getBaseMapper()
                .selectObjs(queryWrapper)
                .stream()
                // .filter(ObjUtil::isNotNull)
                // .map(Object::toString)
                .map(tags -> {
                    if (tags == null) return "[\"无标签\"]";
                    return tags.toString();
                })
                .flatMap(json -> JSONUtil.toList(json, String.class).stream())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));
        // 从Map转换为DTO对象，并排序
        return collect.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .map(tag -> new SpaceTagAnalyzeResponse(tag.getKey(), tag.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(
            SpaceSizeAnalyzeRequest analyzeRequest,
            User loginUser) {
        // 基础参数校验
        checkSpaceAnalyzeAuth(analyzeRequest, loginUser);
        LambdaQueryWrapper<Picture> queryWrapper = new LambdaQueryWrapper<>();
        // 构造查询条件
        fillLambdaQueryWrapper(analyzeRequest, queryWrapper);
        queryWrapper.select(Picture::getPicSize);
        // 获取统计信息
        Map<String, Long> map = new LinkedHashMap<>();
        pictureService.getBaseMapper().selectObjs(queryWrapper)
                .forEach(e -> {
                    long size = (e instanceof Long) ? (Long) e : 0L;
                    String key;
                    if (size < 200 * 1024) {
                        key = "0-200KB";
                    } else if (size < 1024 * 1024) {
                        key = "200-1MB";
                    } else if (size < 5 * 1024 * 1024) {
                        key = "1-5MB";
                    } else if (size < 10 * 1024 * 1024) {
                        key = "5-10MB";
                    } else {
                        key = ">10MB";
                    }
                    Long count = map.getOrDefault(key, 0L);
                    map.put(key, count + 1L);
                });
        // 转换为适合展示的结果并返回
        return map.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .map(e -> new SpaceSizeAnalyzeResponse(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUploadAnalyze(
            SpaceUserAnalyzeRequest analyzeRequest,
            User loginUser) {
        // 基础参数校验
        checkSpaceAnalyzeAuth(analyzeRequest, loginUser);
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        // 填充基础查询条件
        fillQueryWrapper(analyzeRequest, queryWrapper);
        Long userId = analyzeRequest.getUserId();
        if (ObjUtil.isNotNull(userId) && userId > 0) {
            boolean isAdmin = userService.hasPermission(loginUser, UserRole.ADMIN);
            if (!loginUser.getId().equals(userId) && !isAdmin) {
                throw new BusinessException(ErrorCode.AUTHORIZATION_ERROR);
            }
            queryWrapper.eq("user_id", userId);
        }
        String s = analyzeRequest.getTimeDimension();
        MrsTimeDimension timeDimension = EnumUtil.fromValue(s, MrsTimeDimension.class);
        ThrowUtils.throwIf(timeDimension == null, ErrorCode.PARAMS_ERROR);
        // 分类添加时间SQL和统计字段
        switch (timeDimension) {
            case DAY:
                queryWrapper.select("date_format(create_time, '%Y-%m-%d') AS period", "COUNT(*) AS count");
                break;
            case WEEK:
                queryWrapper.select("yearweek(create_time) AS period", "COUNT(*) AS count");
                break;
            case MONTH:
                queryWrapper.select("date_format(create_time, '%Y-%m') AS period", "COUNT(*) AS count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 分组并排序
        queryWrapper.groupBy("period").orderByAsc("period");
        log.info("最终SQL {}", queryWrapper.getTargetSql());
        // 查询并返回统计结果
        return pictureService.getBaseMapper().selectMaps(queryWrapper)
                .stream()
                .map(res -> {
                    String period = res.get("period").toString();
                    Object c = res.get("count");
                    long count = (c instanceof Long) ? (Long) c : 0L;
                    return new SpaceUserAnalyzeResponse(period, count);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Space> getSpaceSaveRankAnalyze(
            SpaceRankAnalyzeRequest analyzeRequest,
            User loginUser) {
        ThrowUtils.throwIf(analyzeRequest == null, ErrorCode.PARAMS_ERROR);
        LambdaQueryWrapper<Space> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(Space::getId, Space::getSpaceName,
                        Space::getUserId, Space::getTotalSize)
                .orderByDesc(Space::getTotalSize)
                // 取前N个
                .last("LIMIT " + analyzeRequest.getTopN());

        return spaceService.list(queryWrapper);
    }

    @Override
    public void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest analyzeRequest, User loginUser) {
        // 基础参数校验
        ThrowUtils.throwIf(analyzeRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        boolean queryPublic = analyzeRequest.isQueryPublic();
        boolean queryAll = analyzeRequest.isQueryAll();
        boolean isAdmin = userService.hasPermission(loginUser, UserRole.ADMIN);
        // 分析公共空间或者分析全部空间需要管理员权限
        if ((queryPublic || queryAll) && !isAdmin) {
            throw new BusinessException(ErrorCode.AUTHORIZATION_ERROR, "暂无权限");
        }
        Long spaceId = analyzeRequest.getSpaceId();
        // 统计公共空间或者全部数据，但违规传递了空间ID
        if ((queryPublic || queryAll) && spaceId != null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 仅分析个人空间，校验空间ID信息
        if ((!queryPublic && !queryAll) && (spaceId == null || spaceId <= 0)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // Space dbSpace = spaceService.getById(spaceId);
        // ThrowUtils.throwIf(dbSpace == null, ErrorCode.NOT_FOUND);
        // 后续交给空间服务的权限校验方法
        // spaceService.checkAuthorization(dbSpace, loginUser);
    }

    /**
     * 填充查询图片的条件
     *
     * @param analyzeRequest 空间分析请求参数DTO
     * @param queryWrapper   匿名查询条件构造器
     */
    private void fillLambdaQueryWrapper(
            SpaceAnalyzeRequest analyzeRequest,
            LambdaQueryWrapper<Picture> queryWrapper) {
        // 如果要查全部，那不用加条件
        if (analyzeRequest.isQueryAll()) {
            return;
        }
        // 只查公共空间（空间ID列为空的就是公共空间图片）
        if (analyzeRequest.isQueryPublic()) {
            queryWrapper.isNull(Picture::getSpaceId);
            return;
        }
        // 查私有空间
        Long spaceId = analyzeRequest.getSpaceId();
        if (spaceId != null && spaceId > 0) {
            queryWrapper.eq(Picture::getSpaceId, spaceId);
            return;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "请指定查询范围");
    }

    /**
     * 填充查询图片的条件
     *
     * @param analyzeRequest 空间分析请求参数DTO
     * @param queryWrapper   匿名查询条件构造器
     */
    private void fillQueryWrapper(
            SpaceAnalyzeRequest analyzeRequest,
            QueryWrapper<Picture> queryWrapper) {
        // 如果要查全部，那不用加条件
        if (analyzeRequest.isQueryAll()) {
            return;
        }
        // 只查公共空间（空间ID列为空的就是公共空间图片）
        if (analyzeRequest.isQueryPublic()) {
            queryWrapper.isNull("space_id");
            return;
        }
        // 查私有空间
        Long spaceId = analyzeRequest.getSpaceId();
        if (spaceId != null && spaceId > 0) {
            queryWrapper.eq("space_id", spaceId);
            return;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "请指定查询范围");
    }
}
