package cloud.marisa.picturebackend.api.imagesearch.sub;

import cloud.marisa.picturebackend.exception.BusinessException;
import cloud.marisa.picturebackend.exception.ErrorCode;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * @author MarisaDAZE
 * @description 获取图片页面的URL
 * @date 2025/4/5
 */
public class GetImagePageUrlApi {
    private static final String URL = "https://graph.baidu.com/upload?uptime=";

    /**
     * 以图搜图（URL版本）
     *
     * @param imageURL 要搜索的URL路径
     * @return 近似图的URL路径
     */
    public static String getImagePageUrl(String imageURL) {
        Map<String, Object> params = new HashMap<>();
        params.put("image", imageURL);
        params.put("tn", "pc");
        params.put("from", "pc");
        params.put("image_source", "PC_UPLOAD_URL");
        long now = System.currentTimeMillis();
        try (HttpResponse execute = HttpRequest.post(URL + now)
                .header("acs-Token", "1")
                .form(params)
                .timeout(3000)
                .execute()) {
            JSONObject response = JSONObject.parseObject(execute.body());
            System.out.println(response);
            if (response == null || !Integer.valueOf(0).equals(response.getInteger("status"))) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR);
            }
            JSONObject data = response.getJSONObject("data");
            String rawURL = data.getString("url");
            String url = URLUtil.decode(rawURL);
            if (url == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "无有效的结果");
            }
            return url;
        }
    }

    public static void main(String[] args) {
        String url = getImagePageUrl("https://www.codefather.cn/logo.png");
        System.out.println("===============");
        System.out.println(url);
        System.out.println("===============");
    }
}
