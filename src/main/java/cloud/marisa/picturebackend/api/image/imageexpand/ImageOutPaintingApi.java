package cloud.marisa.picturebackend.api.image.imageexpand;

import cloud.marisa.picturebackend.api.image.imageexpand.entity.request.ImagePaintingParameters;
import cloud.marisa.picturebackend.api.image.imageexpand.entity.request.ImageRequestParam;
import cloud.marisa.picturebackend.api.image.imageexpand.entity.response.create.CreateTaskResponse;
import cloud.marisa.picturebackend.api.image.imageexpand.entity.response.query.TaskQueryOutput;
import cloud.marisa.picturebackend.api.image.imageexpand.entity.response.query.TaskQueryResponse;
import cloud.marisa.picturebackend.api.image.imageexpand.enums.TaskStatusEnum;
import cloud.marisa.picturebackend.exception.BusinessException;
import cloud.marisa.picturebackend.exception.ErrorCode;
import cloud.marisa.picturebackend.util.EnumUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


/**
 * @author MarisaDAZE
 * @description AI图像扩展
 * @date 2025/4/10
 */
@Slf4j
@Component
public class ImageOutPaintingApi {
    @Value("${mrs.api.bailian-sk}")
    private String API_KEY;

    /**
     * 创建扩图任务
     *
     * @param requestParam 扩图参数
     * @return 任务结果
     */
    public CreateTaskResponse createTask(ImageRequestParam requestParam) {
        String host = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";
        HttpRequest post = HttpUtil.createPost(host);
        post.header("Authorization", String.format("Bearer %s", API_KEY));
        post.header("X-DashScope-Async", "enable");
        post.header("Content-Type", "application/json");
        String params = JSONObject.toJSONString(requestParam);
        try (HttpResponse response = post.body(params).execute()) {
            if (!response.isOk()) {
                System.out.println(response.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "扩图请求失败: " + response.getStatus());
            }
            String body = response.body();
            return JSONObject.parseObject(body, CreateTaskResponse.class);
        }

    }

    /**
     * 查询任务情况
     *
     * @param taskId 任务ID
     * @return 扩图结果
     */
    public TaskQueryResponse queryTask(String taskId) {
        String url = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";
        String host = String.format(url, taskId);
        HttpRequest get = HttpUtil.createGet(host);
        get.header("Authorization", String.format("Bearer %s", API_KEY));
        try (HttpResponse response = get.execute()) {
            System.out.println("taskId: " + taskId);
            System.out.println("============扩图结果============");
            System.out.println(response.body());
            System.out.println("===============================");
            if (!response.isOk()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取结果失败: " + response.getStatus());
            }
            String body = response.body();
            return JSONObject.parseObject(body, TaskQueryResponse.class);
        }
    }

    public static void main(String[] args) {
        ImageOutPaintingApi instance = new ImageOutPaintingApi();
        instance.API_KEY = "sk-bb1c214d64264e2d8c21a9e72b9d9e58";
//        String imageURL = "https://i0.hdslb.com/bfs/article/0c4d771459550a150f4725a0673d4daa9e32ba37.jpg@.webp";
        String imageURL = "http://kmarisa.icu:9000/picture-backend/picture/1905886838348619777/20250411_de667b7610578888f9ad98c69472b284.jpg" +
                "?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=kirisame%2F20250411%2Fcn-north-1%2Fs3%2Faws4_request" +
                "&X-Amz-Date=20250411T075931Z&X-Amz-Expires=604800&X-Amz-SignedHeaders=host" +
                "&X-Amz-Signature=0318530a1f23d473fb6d493fca0fb87944f5d6beee6e4321c3ec4e532ec92218";
        ImagePaintingParameters parameters = ImagePaintingParameters.builder()
                .xScale(1.5F)
                .yScale(1.5F)
                .build();
        ImageRequestParam params = new ImageRequestParam(imageURL, parameters);
        CreateTaskResponse createTaskResponse = instance.createTask(params);
        System.out.println("==========createTaskResponse==========");
        System.out.println(createTaskResponse);
        System.out.println("======================================");
        if (StrUtil.isNotBlank(createTaskResponse.getCode())) {
            System.err.println("出错了: " + createTaskResponse.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "扩图失败");
        }
        String taskId = createTaskResponse.getOutput().getTaskId();
//        String taskId = "e4b03852-781c-48a5-afa6-1e3a3eb06dfd";
        int count = 0;
        try {
            while (count < 30) {
                TaskQueryResponse queryResponse = instance.queryTask(taskId);
                System.out.println("==========queryResponse==========");
                System.out.println(queryResponse);
                System.out.println("=================================");
                TaskQueryOutput output = queryResponse.getOutput();
                TaskStatusEnum status = EnumUtil.fromValueNotNull(output.getTaskStatus(), TaskStatusEnum.UNKNOWN);
                switch (status) {
                    case PENDING:
                    case RUNNING:
                    case SUSPENDED:
                        count++;
                        System.out.println("任务未完成: " + status.getText());
                        Thread.sleep(1000);
                        continue;
                    case SUCCEEDED:
                        break;
                    case FAILED:
                    case UNKNOWN:
                        throw new BusinessException(ErrorCode.OPERATION_ERROR, "扩图失败 " + status.getText());
                }
                String outputImageURL = output.getOutputImageUrl();
                System.out.println("扩图成功: " + outputImageURL);
                return;
            }
            System.out.println("查询超时...");
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
