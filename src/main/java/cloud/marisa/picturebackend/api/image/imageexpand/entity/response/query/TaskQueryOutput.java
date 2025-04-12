package cloud.marisa.picturebackend.api.image.imageexpand.entity.response.query;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;
import lombok.ToString;

import java.util.Date;

/**
 * @author MarisaDAZE
 * @description 任务查询输出信息
 * @date 2025/4/10
 */
@Data
@ToString
public class TaskQueryOutput {

    /**
     * <p>任务ID。</p>
     */
    @JSONField(name = "task_id")
    private String taskId;

    /**
     * 当前状态：
     * <ul>
     *     <li>PENDING：任务排队中</li>
     *     <li>RUNNING：任务处理中</li>
     *     <li>SUSPENDED：任务挂起</li>
     *     <li>SUCCEEDED：任务执行成功</li>
     *     <li>FAILED：任务执行失败</li>
     *     <li>UNKNOWN：任务不存在或状态未知</li>
     * </ul>
     */
    @JSONField(name = "task_status")
    private String taskStatus;

    /**
     * <p>任务结果统计。</p>
     */
    @JSONField(name = "task_metrics")
    private TaskQueryMetrics taskMetrics;

    /**
     * <p>任务提交时间。</p>
     */
    @JSONField(name = "submit_time")
    private Date submitTime;

    /**
     * <p>任务完成时间。</p>
     */
    @JSONField(name = "end_time")
    private Date endTime;

    /**
     * <p>计划时间</p>
     */
    @JSONField(name = "scheduled_time")
    private Date scheduledTime;

    /**
     * <p>输出图像URL地址。</p>
     */
    @JSONField(name = "output_image_url")
    private String outputImageUrl;

    /**
     * <p>请求失败的错误码。请求成功时不会返回此参数，详情请参见
     * <a href="https://bailian.console.aliyun.com/?spm=5176.12818093_47.console-base_search-panel.dtab-product_sfm.754616d0lltCnU&scm=20140722.S_sfm._.ID_sfm-RL_%E7%99%BE%E7%82%BC-LOC_console_console-OR_ser-V_4-P0_0&tab=api#/list/?type=model&url=https%3A%2F%2Fhelp.aliyun.com%2Fdocument_detail%2F2712216.html">错误信息</a>。
     * </p>
     */
    @JSONField(name = "code")
    private String code;

    /**
     * <p>请求失败的详细信息。请求成功时不会返回此参数，详情请参见
     * <a href="https://bailian.console.aliyun.com/?spm=5176.12818093_47.console-base_search-panel.dtab-product_sfm.754616d0lltCnU&scm=20140722.S_sfm._.ID_sfm-RL_%E7%99%BE%E7%82%BC-LOC_console_console-OR_ser-V_4-P0_0&tab=api#/list/?type=model&url=https%3A%2F%2Fhelp.aliyun.com%2Fdocument_detail%2F2712216.html">错误信息</a>。
     * </p>
     */
    @JSONField(name = "message")
    private String message;
}
