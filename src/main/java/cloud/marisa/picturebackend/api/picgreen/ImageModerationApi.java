package cloud.marisa.picturebackend.api.picgreen;

import cloud.marisa.picturebackend.config.aliyun.green.MrsImageModeration;
import cloud.marisa.picturebackend.exception.BusinessException;
import cloud.marisa.picturebackend.exception.ErrorCode;
import cloud.marisa.picturebackend.util.EnumUtil;
import cn.hutool.http.HttpStatus;
import com.alibaba.fastjson2.JSON;
import com.aliyun.green20220302.Client;
import com.aliyun.green20220302.models.*;
import com.aliyun.teautil.models.RuntimeOptions;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI图片审核工具API
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class ImageModerationApi {

    private final Client client;

    /**
     * 发起一个AI审核任务
     *
     * @param url 图片链接地址
     * @return 任务执行结果
     */
    public ImageAsyncModerationResponse createTask(String url) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("imageUrl", url);
        parameters.put("dataId", UUID.randomUUID().toString());
        try {
            return createTask(parameters);
        } catch (Exception e) {
            log.error("AI审核调用失败", e);
            return null;
        }
    }

    /**
     * 发起一个AI审核任务
     *
     * @param url  图片链接地址
     * @param uuid 任务唯一ID
     * @return 任务执行结果
     */
    public ImageAsyncModerationResponse createTask(String url, String uuid) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("imageUrl", url);
        parameters.put("dataId", uuid);
        try {
            return createTask(parameters);
        } catch (Exception e) {
            log.error("AI审核调用失败", e);
            return null;
        }
    }

    /**
     * 发起一个AI审核任务
     *
     * @param parameters 任务参数列表
     * @return 任务执行结果
     * @throws Exception 可能出现的异常
     */
    public ImageAsyncModerationResponse createTask(Map<String, String> parameters) throws Exception {
        if (parameters == null || parameters.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        RuntimeOptions runtime = new RuntimeOptions();
        ImageAsyncModerationRequest imageAsyncModerationRequest = new ImageAsyncModerationRequest();
        imageAsyncModerationRequest.setService("baselineCheck");
        String parametersJSON = JSON.toJSONString(parameters);
        imageAsyncModerationRequest.setServiceParameters(parametersJSON);
        return client.imageAsyncModerationWithOptions(imageAsyncModerationRequest, runtime);
    }

    /**
     * 解析异步任务请求的响应结果
     * <p>成功会返回供查询的任务ID，失败则会报错</p>
     *
     * @param response 任务响应结果
     * @return 任务ID
     */
    public String resolveResponse(ImageAsyncModerationResponse response) {
        if (response == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "请求发起失败 null");
        }
        Integer httpCode = response.getStatusCode();
        if (httpCode != HttpStatus.HTTP_OK) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "请求发起失败 " + httpCode);
        }
        ImageAsyncModerationResponseBody body = response.getBody();
        if (body == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "请求体为空 null");
        }
        Integer bodyCode = body.getCode();
        String msg = body.getMsg();
        String requestId = body.getRequestId();
        if (bodyCode != 200) {
            String errMsg = "image async mode ration not success. " +
                    "code: " + bodyCode + ", " +
                    "msg: " + msg + ", " +
                    "requestId" + requestId;
            log.error("图片审核异步任务创建失败 {}", body);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, errMsg);
        }
        ImageAsyncModerationResponseBody.ImageAsyncModerationResponseBodyData data = body.getData();
        log.info("图片审核异步任务创建成功 {}", data);
        return data.getReqId();
    }

    /**
     * 根据任务ID查询AI检测结果
     *
     * @param taskId 任务ID
     */
    public MrsImageModeration queryResultByTaskId(String taskId) {
        DescribeImageModerationResultRequest taskRequest = new DescribeImageModerationResultRequest();
        // 提交任务时返回的reqId
        taskRequest.setReqId(taskId);
        try {
            DescribeImageModerationResultResponse response = client.describeImageModerationResult(taskRequest);
            Integer httpCode = response.getStatusCode();
            if (httpCode != HttpStatus.HTTP_OK) {
                log.error("查询请求发起失败 {}", response);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "查询请求发起失败 " + httpCode);
            }
            DescribeImageModerationResultResponseBody body = response.getBody();
            if (body == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "请求体为空 null");
            }
            String requestId = body.getRequestId();
            Integer bodyCode = body.getCode();
            String msg = body.getMsg();
            DescribeImageModerationResultResponseBody.DescribeImageModerationResultResponseBodyData data = body.getData();
            MrsImageModeration result = MrsImageModeration.builder()
                    .code(bodyCode)
                    .requestId(requestId)
                    .msg(msg)
                    .data(data)
                    .build();
            // 请求成功
            if (bodyCode == 200) {
                result.setSuccess(true);
                result.setProcessing(false);
                return result;
            }
            // 处理等待中
            if (bodyCode == 280) {
                result.setSuccess(false);
                result.setProcessing(true);
                return result;
            }
            // 其它情况
            result.setSuccess(false);
            result.setProcessing(false);
            return result;
        } catch (Exception e) {
            String errMsg = e.getMessage();
            log.error("查询审核任务失败 ", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "查询审核任务失败 " + errMsg);
        }
    }

    /**
     * 根据结果，解析图片是否合规
     *
     * @param moderation AI审核的结果对象
     * @return 是否合规的结果封装
     */
    public MrsPictureIllegal isIllegal(MrsImageModeration moderation) {
        if (moderation == null) {
            log.error("AI审核结果为空 null");
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        DescribeImageModerationResultResponseBody.DescribeImageModerationResultResponseBodyData data = moderation.getData();
        String riskLevel = data.getRiskLevel();
        List<DescribeImageModerationResultResponseBody.DescribeImageModerationResultResponseBodyDataResult> result = data.getResult();
        switch (riskLevel) {
            // 无风险
            case "none":
                return MrsPictureIllegal.builder()
                        .legal(true)
                        .reason("未检测出风险")
                        .build();
            // 高风险
            case "high":
            default:
                String reason = "";
                List<String> reasons = new ArrayList<>();
                if (result != null && !result.isEmpty()) {
                    reason = result.stream()
                            .map(
                                    DescribeImageModerationResultResponseBody.
                                            DescribeImageModerationResultResponseBodyDataResult
                                            ::getDescription)
                            .collect(Collectors.joining(","));
                    reasons = result.stream()
                            .map(
                                    DescribeImageModerationResultResponseBody.
                                            DescribeImageModerationResultResponseBodyDataResult
                                            ::getDescription)
                            .collect(Collectors.toList());
                }
                return MrsPictureIllegal.builder()
                        .legal(false)
                        .reason(reason)
                        .reasons(reasons)
                        .result(result)
                        .riskLevel(riskLevel)
                        .build();
        }
    }

    /**
     * 这个风险是否是在可控范围内
     * <p>这个方法建议在isIllegal的legal为false时调用</p>
     * <p>某些图片的违规情况是可以接收的</p>
     *
     * @param pictureIllegal 图片违规结果对象封装
     * @return 是否是真的违规（true-真违规，false-假违规）
     */
    public boolean isAllowedRisk(MrsPictureIllegal pictureIllegal) {
        List<MrsAliyunGreenLabels> defaultBlackList = MrsAliyunGreenLabels.getDefaultBlackList();
        return isAllowedRisk(pictureIllegal, defaultBlackList);

    }

    /**
     * 这个风险是否是在可控范围内
     * <p>这个方法建议在isIllegal的legal为false时调用</p>
     * <p>某些图片的违规情况是可以接收的</p>
     *
     * @param pictureIllegal 图片违规结果对象封装
     * @param blackList      黑名单标签列表
     * @return 是否是真的违规（true-真违规，false-假违规）
     */
    public boolean isAllowedRisk(MrsPictureIllegal pictureIllegal, List<MrsAliyunGreenLabels> blackList) {
        // 将黑名单列表按照Label进行分组
        Map<String, List<MrsGreenLabelTemp>> collect = blackList.stream()
                .map(item -> {
                    MrsGreenLabelTemp temp = new MrsGreenLabelTemp();
                    temp.setLabel(item.getValue());
                    temp.setDescription(item.getDescription());
                    temp.setConfidence(item.getConfidence());
                    return temp;
                })
                .collect(Collectors.groupingBy(MrsGreenLabelTemp::getLabel));

        // 创建label-confidence的映射关系列表
        Map<String, Float> labels = new HashMap<>();
        // 遍历并添加到映射关系列表中
        // 这里忽略掉了对应的违规描述信息
        collect.keySet().forEach(key -> {
            List<MrsGreenLabelTemp> temps = collect.get(key);
            if (temps == null || temps.isEmpty()) return;
            MrsGreenLabelTemp temp = temps.get(0);
            labels.put(temp.getLabel(), temp.getConfidence());
        });

        return isAllowedRisk(pictureIllegal, labels);
    }


    /**
     * 这个风险是否是在可控范围内
     * <p>这个方法建议在isIllegal的legal为false时调用</p>
     * <p>某些图片的违规情况是可以接收的</p>
     *
     * @param pictureIllegal 图片违规结果对象封装
     * @param blackLabels    这些标签的违规是不允许的
     *                       <p>Map的键时违规标签，值是允许的评分，超过这个分数的结果将不被允许</p>
     *                       <p>Value的取值范围[0,100]</p>
     * @return 是否是真的违规（true-真违规，false-假违规）
     */
    public boolean isAllowedRisk(MrsPictureIllegal pictureIllegal, Map<String, Float> blackLabels) {
        return pictureIllegal.getResult().stream().anyMatch(item -> {
            String key = item.getLabel();
            Float minConfidence = blackLabels.get(key);
            // 如果没有这个key，则说明这个标签不在黑名单中
            if (minConfidence == null) {
                // 检查和对应的枚举类的阈值是否冲突
                MrsAliyunGreenLabels currentLabel = EnumUtil.fromValue(key, MrsAliyunGreenLabels.class);
                // 不在枚举中，可能是新增的违规类型
                if (currentLabel == null) {
                    log.info("未知的违规类型，未被加入到MrsAliyunGreenLabels枚举中：{}", key);
                    return true;
                }
                // 在枚举中，则比较置信度
                float confidence = currentLabel.getConfidence();
                return (item.getConfidence() > confidence);
            }
            // 不为空，说明标签在黑名单中，比较置信度
            // 置信度高于阈值的图片判定为违规
            return (item.getConfidence() > minConfidence);
        });
    }

    @Data
    @ToString
    public static class MrsGreenLabelTemp {
        /**
         * 标签名称
         */
        private String label;

        /**
         * 标签的中文含义
         */
        private String description;

        /**
         * 标签的置信度
         * <p>0~100分，分数越高置信度越高</p>
         * <p>你认为某个违规项多少分才是真违规 就写多少</p>
         * <p>比如你觉得”疑似含有刀具、枪支等内容“置信度达到95才算违规，那你就写95</p>
         */
        private float confidence;
    }
}
