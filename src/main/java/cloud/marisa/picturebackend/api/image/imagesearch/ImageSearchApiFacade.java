package cloud.marisa.picturebackend.api.image.imagesearch;

import cloud.marisa.picturebackend.api.image.imagesearch.entity.ImageSearchResult;

import java.util.ArrayList;
import java.util.List;

/**
 * @author MarisaDAZE
 * @description 图片搜索服务API
 * @date 2025/4/5
 */
public class ImageSearchApiFacade {

    /**
     * 根据图片URL获取相似图片数据
     * <p style="color:orange;">用不了，应该接入第三方API服务</p>
     * <p style="color:orange;">本地那个百度做了反制</p>
     *
     * @param url 图片URL
     * @return 相似图片列表
     */
    public static List<ImageSearchResult> searchImageByURL(String url) {
        System.out.println("URL:" + url);
        return new ArrayList<>();
    }
}
