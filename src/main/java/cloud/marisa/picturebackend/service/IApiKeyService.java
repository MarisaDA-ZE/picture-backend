package cloud.marisa.picturebackend.service;

import cloud.marisa.picturebackend.entity.dao.ApiKey;
import cloud.marisa.picturebackend.entity.dto.api.ApiKeyCreateRequest;
import cloud.marisa.picturebackend.entity.dto.api.ApiKeyListResponse;
import cloud.marisa.picturebackend.entity.dto.api.ApiKeyResponse;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author Marisa
* @description 针对表【api_key(图床API密钥表)】的数据库操作Service
* @createDate 2025-06-05 01:33:26
*/
public interface IApiKeyService extends IService<ApiKey> {

    /**
     * 创建API密钥
     * @param request 创建请求
     * @param userId 用户ID
     * @return 创建结果
     */
    ApiKeyResponse createApiKey(ApiKeyCreateRequest request, Long userId);

    /**
     * 列出API密钥
     * @param userId 用户ID
     * @return 密钥列表
     */
    ApiKeyListResponse listApiKeys(Long userId);

    /**
     * 删除API密钥
     * @param apiKeyId API密钥ID
     * @param userId 用户ID
     * @return 删除结果
     */
    boolean deleteApiKey(Long apiKeyId, Long userId);

    /**
     * 获取API密钥关联的空间ID列表
     * @param apiKeyId API密钥ID
     * @return 空间ID列表
     */
    List<Long> getApiKeySpaces(Long apiKeyId);

    /**
     * 更新API密钥状态
     * @param apiKeyId API密钥ID
     * @param status 状态
     * @param userId 用户ID
     * @return 更新结果
     */
    boolean updateApiKeyStatus(Long apiKeyId, Integer status, Long userId);

    /**
     * 检查API密钥限制
     * @param accessKey 访问密钥
     * @param secretKey 密钥
     * @return 是否限制
     */
    boolean checkApiKeyLimit(String accessKey, String secretKey);

    /**
     * 记录API调用
     * @param accessKey 访问密钥
     * @param callType 调用类型
     * @param ip IP地址
     */
    void recordApiCall(String accessKey, String callType, String ip);

}
