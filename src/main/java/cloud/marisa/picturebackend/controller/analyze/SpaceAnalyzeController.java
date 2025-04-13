package cloud.marisa.picturebackend.controller.analyze;

import cloud.marisa.picturebackend.annotations.AuthCheck;
import cloud.marisa.picturebackend.common.MrsResult;
import cloud.marisa.picturebackend.entity.dao.Space;
import cloud.marisa.picturebackend.entity.dao.User;
import cloud.marisa.picturebackend.entity.dto.analyze.request.*;
import cloud.marisa.picturebackend.entity.dto.analyze.response.*;
import cloud.marisa.picturebackend.enums.UserRole;
import cloud.marisa.picturebackend.exception.ErrorCode;
import cloud.marisa.picturebackend.exception.ThrowUtils;
import cloud.marisa.picturebackend.service.ISpaceAnalyzeService;
import cloud.marisa.picturebackend.service.IUserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author MarisaDAZE
 * @description 空间统计信息
 * @date 2025/3/25
 */
@RestController
@RequestMapping("/space/analyze")
public class SpaceAnalyzeController {

    @Resource
    private IUserService userService;

    @Resource
    private ISpaceAnalyzeService spaceAnalyzeService;

    /**
     * * 获取空间使用情况
     *
     * @param analyzeRequest 请求参数DTO封装
     * @param servletRequest servlet请求对象
     * @return 空间使用情况
     */
    @PostMapping("/usage")
    public MrsResult<?> getSpaceUsageAnalyze(
            @RequestBody SpaceUsageAnalyzeRequest analyzeRequest,
            HttpServletRequest servletRequest) {
        ThrowUtils.throwIf(analyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(servletRequest);
        SpaceUsageAnalyzeResponse analyze = spaceAnalyzeService.getSpaceUsageAnalyze(analyzeRequest, loginUser);
        return MrsResult.ok(analyze);
    }

    /**
     * 获取空间图片分类分析结果
     *
     * @param analyzeRequest 请求参数DTO封装
     * @param servletRequest servlet请求对象
     * @return 分类分析结果
     */
    @PostMapping("/category")
    public MrsResult<?> getSpaceCategoryAnalyze(
            @RequestBody SpaceCategoryAnalyzeRequest analyzeRequest,
            HttpServletRequest servletRequest) {
        ThrowUtils.throwIf(analyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(servletRequest);
        List<SpaceCategoryAnalyzeResponse> analyzeList = spaceAnalyzeService.getSpaceCategoryAnalyze(analyzeRequest, loginUser);
        return MrsResult.ok(analyzeList);
    }

    /**
     * 获取空间图片标签类型结果
     *
     * @param analyzeRequest 请求参数DTO封装
     * @param servletRequest servlet请求对象
     * @return 标签类型结果
     */
    @PostMapping("/tag")
    public MrsResult<?> getSpaceTagAnalyze(
            @RequestBody SpaceTagAnalyzeRequest analyzeRequest,
            HttpServletRequest servletRequest) {
        ThrowUtils.throwIf(analyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(servletRequest);
        List<SpaceTagAnalyzeResponse> analyzeList = spaceAnalyzeService.getSpaceTagAnalyze(analyzeRequest, loginUser);
        return MrsResult.ok(analyzeList);
    }

    /**
     * 获取空间占用大小分析结果
     *
     * @param analyzeRequest 请求参数DTO封装
     * @param servletRequest servlet请求对象
     * @return 空间占用大小分析结果
     */
    @PostMapping("/size")
    public MrsResult<?> getSpaceSizeAnalyze(
            @RequestBody SpaceSizeAnalyzeRequest analyzeRequest,
            HttpServletRequest servletRequest) {
        ThrowUtils.throwIf(analyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(servletRequest);
        List<SpaceSizeAnalyzeResponse> analyzeList = spaceAnalyzeService.getSpaceSizeAnalyze(analyzeRequest, loginUser);
        return MrsResult.ok(analyzeList);
    }

    /**
     * 获取用户上传行为分析结果
     *
     * @param analyzeRequest 请求参数DTO封装
     * @param servletRequest servlet请求对象
     * @return 用户上传行为分析结果
     */
    @PostMapping("/user")
    public MrsResult<?> getSpaceUserUploadAnalyze(
            @RequestBody SpaceUserAnalyzeRequest analyzeRequest,
            HttpServletRequest servletRequest) {
        ThrowUtils.throwIf(analyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(servletRequest);
        List<SpaceUserAnalyzeResponse> analyzeList = spaceAnalyzeService.getSpaceUploadAnalyze(analyzeRequest, loginUser);
        return MrsResult.ok(analyzeList);
    }

    /**
     * 获取用户上传行为分析结果
     *
     * @param analyzeRequest 请求参数DTO封装
     * @param servletRequest servlet请求对象
     * @return 用户上传行为分析结果
     */
    @PostMapping("/rank")
    @AuthCheck(mustRole = UserRole.ADMIN)
    public MrsResult<?> getSpaceRankAnalyze(
            @RequestBody SpaceRankAnalyzeRequest analyzeRequest,
            HttpServletRequest servletRequest) {
        ThrowUtils.throwIf(analyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(servletRequest);
        List<Space> analyzeList = spaceAnalyzeService.getSpaceSaveRankAnalyze(analyzeRequest, loginUser);
        return MrsResult.ok(analyzeList);
    }

}
