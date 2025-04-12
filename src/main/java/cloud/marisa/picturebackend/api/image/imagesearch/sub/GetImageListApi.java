package cloud.marisa.picturebackend.api.image.imagesearch.sub;

import cloud.marisa.picturebackend.api.image.imagesearch.entity.ImageSearchResult;
import cloud.marisa.picturebackend.exception.BusinessException;
import cloud.marisa.picturebackend.exception.ErrorCode;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.util.List;

/**
 * @author MarisaDAZE
 * @description 获取图片链接列表的API
 * @date 2025/4/5
 */
public class GetImageListApi {

    /**
     * 获取图片链接列表
     * <p style="color: red;">
     * 用不了，百度API升级接口了
     * </p>
     *
     * @param url 页面URL
     * @return 图片链接列表
     */
    public static List<ImageSearchResult> getImageList(String url) {
        try (HttpResponse response = HttpUtil.createGet(url).execute()) {
            if (response == null || !response.isOk()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR);
            }
            String body = response.body();
            System.out.println(body);
            return processResponse(JSONObject.parseObject(body));
        }
    }

    /**
     * 解析页面结果
     *
     * @param body 页面内容
     * @return 图片列表
     */
    public static List<ImageSearchResult> processResponse(JSONObject body) {
        if (body == null || !body.containsKey("data")) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "无有效结果");
        }
        JSONObject data = body.getJSONObject("data");
        if (data == null || !data.containsKey("list")) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "无有效结果");
        }
        JSONArray list = data.getJSONArray("list");
        list.forEach(System.out::println);
        return list.toJavaList(ImageSearchResult.class);
    }

    public static void main(String[] args) {
        String pageURL = "https://graph.baidu.com/s?card_key=&entrance=" +
                "GENERAL&extUiData[isLogoShow]=1&f=all&isLogoShow=1&session_id=13378364723297179360&" +
                "sign=1218ae97cd54acd88139901743838342&tpl_from=pc";
        List<ImageSearchResult> imageList = getImageList(pageURL);
        System.out.println("==============");
        imageList.forEach(System.out::println);
        System.out.println("==============");
    }
}
