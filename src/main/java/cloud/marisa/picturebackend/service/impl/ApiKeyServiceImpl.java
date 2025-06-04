package cloud.marisa.picturebackend.service.impl;

import cloud.marisa.picturebackend.entity.dao.ApiKey;
import cloud.marisa.picturebackend.mapper.ApiCallRecordMapper;
import cloud.marisa.picturebackend.mapper.ApiKeySpaceMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cloud.marisa.picturebackend.service.IApiKeyService;
import cloud.marisa.picturebackend.mapper.ApiKeyMapper;
import org.springframework.stereotype.Service;

import cloud.marisa.picturebackend.entity.dao.ApiCallRecord;
import cloud.marisa.picturebackend.entity.dao.ApiKeySpace;
import cloud.marisa.picturebackend.entity.dto.api.*;
import cloud.marisa.picturebackend.exception.BusinessException;
import cloud.marisa.picturebackend.exception.ErrorCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.transaction.annotation.Transactional;
import cn.hutool.core.util.RandomUtil;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Marisa
 * @description 针对表【api_key(图床API密钥表)】的数据库操作Service实现
 * @createDate 2025-06-05 01:33:26
 */
@Service
@RequiredArgsConstructor
public class ApiKeyServiceImpl extends ServiceImpl<ApiKeyMapper, ApiKey>
        implements IApiKeyService {

    private final ApiKeySpaceMapper apiKeySpaceMapper;
    private final ApiCallRecordMapper apiCallRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiKeyResponse createApiKey(ApiKeyCreateRequest request, Long userId) {
        // 检查用户是否已有相同类型的API密钥
        List<ApiKey> apiKeys = this.list(new LambdaQueryWrapper<ApiKey>()
                .eq(ApiKey::getUserId, userId)
        );
        List<Long> apiKeyIds = apiKeys.stream().map(ApiKey::getId).collect(Collectors.toList());
        if (!apiKeyIds.isEmpty()) {
            LambdaQueryWrapper<ApiKeySpace> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(ApiKeySpace::getApiKeyId, apiKeyIds);
            List<ApiKeySpace> apiKeySpaces = apiKeySpaceMapper.selectList(queryWrapper);

            if (!apiKeySpaces.isEmpty()) {
                Set<Long> collect = apiKeySpaces.stream()
                        .map(ApiKeySpace::getSpaceId)
                        .collect(Collectors.toSet());
                boolean has = collect.containsAll(request.getSpaceIds());
                if (has) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "已存在API密钥，请先删除后再创建");
                }
            }
        }
        // if (!existingKeys.isEmpty()) {
        //     throw new BusinessException(ErrorCode.OPERATION_ERROR, "已存在API密钥，请先删除后再创建");
        // }

        // 创建API密钥
        ApiKey apiKey = new ApiKey();
        apiKey.setUserId(userId);
        apiKey.setName(request.getName());
        apiKey.setDescription(request.getDescription());
        apiKey.setAccessKey(generateAccessKey());
        apiKey.setSecretKey(generateSecretKey());
        apiKey.setStatus(1);
        // 最多只能一千次每天
        apiKey.setDailyLimit(Math.min(request.getDailyLimit(), 1000));
        apiKey.setCreateTime(new Date());

        this.save(apiKey);

        // 保存空间关联
        for (Long spaceId : request.getSpaceIds()) {
            ApiKeySpace apiKeySpace = new ApiKeySpace();
            apiKeySpace.setApiKeyId(apiKey.getId());
            apiKeySpace.setSpaceId(spaceId);
            apiKeySpaceMapper.insert(apiKeySpace);
        }

        // 构建返回对象
        ApiKeyResponse response = new ApiKeyResponse();
        response.setId(apiKey.getId());
        response.setAccessKey(apiKey.getAccessKey());
        response.setSecretKey(apiKey.getSecretKey());
        response.setName(apiKey.getName());
        response.setDescription(apiKey.getDescription());
        response.setStatus(apiKey.getStatus());
        response.setDailyLimit(apiKey.getDailyLimit());
        response.setSpaceIds(request.getSpaceIds());

        return response;
    }

    @Override
    public ApiKeyListResponse listApiKeys(Long userId) {
        List<ApiKey> apiKeys = this.list(new LambdaQueryWrapper<ApiKey>()
                .eq(ApiKey::getUserId, userId)
                .eq(ApiKey::getIsDeleted, 0));

        List<ApiKeyResponse> responses = apiKeys.stream().map(apiKey -> {
            ApiKeyResponse response = new ApiKeyResponse();
            response.setId(apiKey.getId());
            response.setAccessKey(apiKey.getAccessKey());
            response.setSecretKey(apiKey.getSecretKey());
            response.setName(apiKey.getName());
            response.setDescription(apiKey.getDescription());
            response.setStatus(apiKey.getStatus());
            response.setDailyLimit(apiKey.getDailyLimit());

            // 获取关联的空间ID
            List<Long> spaceIds = apiKeySpaceMapper.selectList(
                            new LambdaQueryWrapper<ApiKeySpace>()
                                    .eq(ApiKeySpace::getApiKeyId, apiKey.getId()))
                    .stream()
                    .map(ApiKeySpace::getSpaceId)
                    .collect(Collectors.toList());
            response.setSpaceIds(spaceIds);

            return response;
        }).collect(Collectors.toList());

        ApiKeyListResponse listResponse = new ApiKeyListResponse();
        listResponse.setApiKeys(responses);
        listResponse.setTotal((long) responses.size());

        return listResponse;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteApiKey(Long apiKeyId, Long userId) {
        ApiKey apiKey = this.getById(apiKeyId);
        if (apiKey == null || !apiKey.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "API密钥不存在");
        }
        // 删除空间关联
        apiKeySpaceMapper.delete(new LambdaQueryWrapper<ApiKeySpace>()
                .eq(ApiKeySpace::getApiKeyId, apiKeyId));
        // 逻辑删除API密钥
        return this.removeById(apiKeyId);
    }

    @Override
    public List<Long> getApiKeySpaces(Long apiKeyId) {
        return apiKeySpaceMapper.selectList(
                        new LambdaQueryWrapper<ApiKeySpace>()
                                .eq(ApiKeySpace::getApiKeyId, apiKeyId))
                .stream()
                .map(ApiKeySpace::getSpaceId)
                .collect(Collectors.toList());
    }

    @Override
    public boolean updateApiKeyStatus(Long apiKeyId, Integer status, Long userId) {
        ApiKey apiKey = this.getById(apiKeyId);
        if (apiKey == null || !apiKey.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "API密钥不存在");
        }
        apiKey.setStatus(status);
        return this.updateById(apiKey);
    }

    @Override
    public boolean checkApiKeyLimit(String accessKey, String secretKey) {
        ApiKey apiKey = this.getOne(new LambdaQueryWrapper<ApiKey>()
                .eq(ApiKey::getAccessKey, accessKey)
                .eq(ApiKey::getSecretKey, secretKey)
                .eq(ApiKey::getStatus, 1)
                .eq(ApiKey::getIsDeleted, 0));

        if (apiKey == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "API密钥不存在或已禁用");
        }

        // 检查今日调用次数
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        Long todayCount = apiCallRecordMapper.selectCount(new LambdaQueryWrapper<ApiCallRecord>()
                .eq(ApiCallRecord::getApiKeyId, apiKey.getId())
                .ge(ApiCallRecord::getCallTime, today));

        return todayCount < apiKey.getDailyLimit();
    }

    @Override
    public void recordApiCall(String accessKey, String callType, String ip) {
        ApiKey apiKey = this.getOne(new LambdaQueryWrapper<ApiKey>()
                .eq(ApiKey::getAccessKey, accessKey)
                .eq(ApiKey::getStatus, 1)
                .eq(ApiKey::getIsDeleted, 0));

        if (apiKey == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "API密钥不存在或已禁用");
        }

        ApiCallRecord record = new ApiCallRecord();
        record.setApiKeyId(apiKey.getId());
        record.setUserId(apiKey.getUserId());
        record.setCallType(callType);
        record.setIp(ip);
        record.setStatus(1);

        apiCallRecordMapper.insert(record);
    }

    private String generateAccessKey() {
        return "ak_" + RandomUtil.randomString(32);
    }

    private String generateSecretKey() {
        return "sk_" + RandomUtil.randomString(32);
    }
}




