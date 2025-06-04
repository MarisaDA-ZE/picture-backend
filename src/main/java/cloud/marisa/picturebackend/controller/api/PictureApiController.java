package cloud.marisa.picturebackend.controller.api;

import cloud.marisa.picturebackend.annotations.SaSpaceCheckPermission;
import cloud.marisa.picturebackend.common.MrsResult;
import cloud.marisa.picturebackend.entity.dao.User;
import cloud.marisa.picturebackend.entity.dto.api.*;
import cloud.marisa.picturebackend.entity.dto.common.DeleteRequest;
import cloud.marisa.picturebackend.entity.vo.PictureVo;
import cloud.marisa.picturebackend.manager.auth.constant.SpaceUserPermissionConstants;
import cloud.marisa.picturebackend.service.IApiKeyService;
import cloud.marisa.picturebackend.service.IPictureService;
import cloud.marisa.picturebackend.service.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;


/**
 * @author MarisaDAZE
 * @description API控制类
 * @date 2025/6/5
 */
@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/picture")
public class PictureApiController {

    private final IUserService userService;

    private final IApiKeyService apiKeyService;

    private final IPictureService pictureService;

    @PostMapping("/key/create")
    public MrsResult<ApiKeyResponse> createApiKey(
            @RequestBody @Valid ApiKeyCreateRequest request,
            HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        Long userId = loginUser.getId();
        ApiKeyResponse response = apiKeyService.createApiKey(request, userId);
        return MrsResult.ok(response);
    }

    @GetMapping("/key/list")
    public MrsResult<ApiKeyListResponse> listApiKeys(HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        Long userId = loginUser.getId();
        ApiKeyListResponse response = apiKeyService.listApiKeys(userId);
        return MrsResult.ok(response);
    }

    @PostMapping("/key/delete")
    @SaSpaceCheckPermission(SpaceUserPermissionConstants.PICTURE_DELETE)
    public MrsResult<?> deleteApiKey(@RequestBody DeleteRequest deleteRequest,
                                     HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        Long userId = loginUser.getId();
        boolean result = apiKeyService.deleteApiKey(deleteRequest.getId(), userId);
        return MrsResult.ok(result);
    }

    @PostMapping("/key/status/{id}")
    public MrsResult<Boolean> updateApiKeyStatus(
            @PathVariable Long id,
            @RequestParam Integer status,
            HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        Long userId = loginUser.getId();

        boolean result = apiKeyService.updateApiKeyStatus(id, status, userId);
        return MrsResult.ok(result);
    }

    @PostMapping("/key/update")
    public MrsResult<Boolean> updateApiKeyById(
            @RequestBody @Valid ApiKeyUpdateRequest request,
            HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        Long userId = loginUser.getId();
        Long id = request.getId();
        Integer status = request.getStatus();
        System.out.println(id + " status: " + status + " userId: " + userId);
        boolean result = apiKeyService.updateApiKeyStatus(id, status, userId);
        return MrsResult.ok(result);
    }

    @PostMapping("/random")
    public MrsResult<List<PictureVo>> getRandomPictures(
            @RequestBody @Valid RandomPictureRequest request,
            HttpServletRequest httpRequest) {
        // 检查API密钥和调用限制
        if (!apiKeyService.checkApiKeyLimit(request.getAccessKey(), request.getSecretKey())) {
            return MrsResult.failed("今日调用次数已耗尽");
        }

        // 记录调用
        apiKeyService.recordApiCall(request.getAccessKey(), "RANDOM", getClientIp(httpRequest));

        // 获取随机图片
        List<PictureVo> pictures = pictureService.getRandomPictures(request);

        return MrsResult.ok(pictures);
    }

    @PostMapping("/search")
    public MrsResult<List<PictureVo>> searchPictures(
            @RequestBody @Valid SearchPictureRequest request,
            HttpServletRequest httpRequest) {
        // 检查API密钥和调用限制
        if (!apiKeyService.checkApiKeyLimit(request.getAccessKey(), request.getSecretKey())) {
            return MrsResult.failed("今日调用次数已耗尽");
        }
        // 保存记录调用，方便分析
        apiKeyService.recordApiCall(request.getAccessKey(), "SEARCH", getClientIp(httpRequest));
        // 搜索图片
        List<PictureVo> pictures = pictureService.searchPictures(request);
        return MrsResult.ok(pictures);
    }

    /**
     * 尝试获取客户端IP
     *
     * @param request 请求
     * @return .
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}