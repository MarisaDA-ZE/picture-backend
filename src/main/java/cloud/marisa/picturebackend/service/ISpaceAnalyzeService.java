package cloud.marisa.picturebackend.service;

import cloud.marisa.picturebackend.entity.dao.Space;
import cloud.marisa.picturebackend.entity.dao.User;
import cloud.marisa.picturebackend.entity.dto.analyze.request.*;
import cloud.marisa.picturebackend.entity.dto.analyze.request.base.SpaceAnalyzeRequest;
import cloud.marisa.picturebackend.entity.dto.analyze.response.*;

import java.util.List;

/**
 * @author MarisaDAZE
 * @description 空间分析服务接口
 * @date 2025/4/12
 */
public interface ISpaceAnalyzeService {

    /**
     * 获取空间使用情况分析
     *
     * @param analyzeRequest 空间分析参数DTO封装
     * @param loginUser      当前登录用户
     * @return 分析结果的DTO封装
     */
    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest analyzeRequest, User loginUser);

    /**
     * 获取空间类别统计分析
     *
     * @param analyzeRequest 类别分析参数DTO封装
     * @param loginUser      当前登录用户
     * @return 分析结果的DTO封装
     */
    List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest analyzeRequest, User loginUser);

    /**
     * 获取空间标签统计分析
     *
     * @param analyzeRequest 标签分析参数DTO封装
     * @param loginUser      当前登录用户
     * @return 分析结果的DTO封装
     */
    List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest analyzeRequest, User loginUser);

    /**
     * 获取空间图片大小统计分析
     *
     * @param analyzeRequest 空间图片大小参数DTO封装
     * @param loginUser      当前登录用户
     * @return 分析结果的DTO封装
     */
    List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest analyzeRequest, User loginUser);

    /**
     * 获取空间用户上传行为统计分析
     *
     * @param analyzeRequest 空间图片大小参数DTO封装
     * @param loginUser      当前登录用户
     * @return 分析结果的DTO封装
     */
    List<SpaceUserAnalyzeResponse> getSpaceUploadAnalyze(SpaceUserAnalyzeRequest analyzeRequest, User loginUser);

    /**
     * 获取空间存储占用排名前n的统计分析
     *
     * @param analyzeRequest 占用情况参数DTO封装
     * @param loginUser      当前登录用户
     * @return 分析结果的DTO封装
     */
    List<Space> getSpaceSaveRankAnalyze(SpaceRankAnalyzeRequest analyzeRequest, User loginUser);


    /**
     * 校验是否拥有操作权限
     *
     * @param analyzeRequest 空间分析参数DTO封装
     * @param loginUser      当前登录用户
     */
    void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest analyzeRequest, User loginUser);
}
