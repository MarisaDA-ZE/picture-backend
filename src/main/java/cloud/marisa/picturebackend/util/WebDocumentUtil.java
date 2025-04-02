package cloud.marisa.picturebackend.util;

import cloud.marisa.picturebackend.exception.BusinessException;
import cloud.marisa.picturebackend.exception.ErrorCode;
import cn.hutool.core.util.StrUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author MarisaDAZE
 * @description WebDocumentUtil.类
 * @date 2025/4/2
 */
public class WebDocumentUtil {

    /**
     * 从https://cn.bing.com下获取图片URL列表
     *
     * @param url bing.image的URL地址
     * @return 图片URL列表
     */
    public static List<String> getPictureUrlInBing(String url) {
        Document document;
        try {
            document = Jsoup.connect(url).get();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }
        Element dgControl = document.getElementsByClass("dgControl").first();
        if (dgControl == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        Elements imgList = dgControl.select("img.mimg");
        List<String> urls = new ArrayList<>();
        for (Element element : imgList) {
            String src = element.attr("src");
            // 空src，跳过
            if (StrUtil.isBlank(src)) continue;
            // 防止转义问题，裁掉多余的参数
            int questionMarkIndex = src.indexOf("?");
            if (questionMarkIndex > -1) {
                src = src.substring(0, questionMarkIndex);
            }
            urls.add(src);
        }
        return urls;
    }
}
