package cloud.marisa.picturebackend.api.image.imageexpand.entity.response.create;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;
import lombok.ToString;

/**
 * @author MarisaDAZE
 * @description AI扩图任务响应结果
 * @date 2025/4/10
 */
@Data
@ToString
public class CreateTaskResponse {

    /**
     * <p>请求唯一标识。可用于请求明细溯源和问题排查。</p>
     */
    @JSONField(name = "request_id")
    private String requestId;

    /**
     * <p>任务输出信息。</p>
     */
    @JSONField(name = "output")
    private CreateTaskOutput output;

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
