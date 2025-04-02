package cloud.marisa.picturebackend.util;

import cn.hutool.core.util.StrUtil;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author MarisaDAZE
 * @description 路径工具类
 * @date 2025/4/1
 */
public class MrsPathUtil {

    /**
     * 修复路径
     * <p>路径结尾不是"/"的自动添加"/"</p>
     *
     * @param path 路径
     * @return 结果
     */
    public static String repairPath(String path) {
        String split = path.endsWith("/") ? "" : "/";
        return path + split;
    }

    /**
     * 检查一个字符串是否是合法的URL地址
     *
     * @param url url字符串
     * @return true:是URL地址；false:不是URL地址
     */
    public static boolean isURL(String url) {
        if (StrUtil.isBlank(url)) {
            return false;
        }
        try {
            new URL(url);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    /**
     * 检查一个字符串是否是合法的webURL地址
     *
     * @param url url地址
     * @return true:是webURL地址；false:不是webURL地址
     */
    public static boolean isWebURL(String url) {
        // 不是URL地址
        if (!isURL(url)) {
            return false;
        }
        // 是否采用http或https协议
        return (url.startsWith("http://")
                || url.startsWith("HTTP://")
                || url.startsWith("https://")
                || url.startsWith("HTTPS://"));
    }
}
