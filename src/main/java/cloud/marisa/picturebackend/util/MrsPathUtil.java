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

    /**
     * 去除URL协议头
     *
     * @param url URL地址
     * @return 去掉协议头的URL地址
     */
    public static String trimAgreementWeb(String url) {
        if (url.startsWith("http://")
                || url.startsWith("HTTP://")
                || url.startsWith("https://")
                || url.startsWith("HTTPS://")) {
            return url.substring(url.indexOf("//") + 2);
        } else {
            return url;
        }
    }


    /**
     * 获取文件相对路径
     *
     * @param path 文件名（/、path/to/...、/path/to/.../example.jpg）
     * @return 文件名称（path/to/...）
     */
    public static String trimPathUnRoot(String path) {
        String ph = trimPath(path);
        if (StrUtil.isBlank(ph)) return "";
        // 大于1，说明至少是/x
        if (ph.length() > 1) {
            // 以/开头就去除/
            if (ph.startsWith("/")) {
                return ph.substring(1);
            }
        } else {
            // 等于1，有可能是/ 也有可能是x
            if (ph.startsWith("/")) {
                return "";
            }
        }
        return ph;
    }

    /**
     * 获取文件绝对路径
     *
     * @param path 文件名（/、path/to/...、/path/to/.../example.jpg）
     * @return 文件名称（/path/to/...）
     */
    public static String trimPathRoot(String path) {
        String ph = trimPath(path);
        // 空字符串
        if (StrUtil.isBlank(ph)) {
            return "/";
        }
        // 不以/开头，则添加/
        if (!ph.startsWith("/")) {
            return "/" + ph;
        }
        return ph;
    }

    /**
     * 获取文件绝对路径
     *
     * @param path 文件名（/、path/to/...、/path/to/.../example.jpg）
     * @return 文件名称（/path/to/...）
     */
    public static String trimPath(String path) {
        if (path == null) return null;
        int pathLIx = path.lastIndexOf("/");
        if (pathLIx == -1) {
            return path;
        }
        return path.substring(0, pathLIx);
    }

    /**
     * 获取文件名
     *
     * @param fileName 文件名（example、example.jpg、/path/to/example.jpg）
     * @return 文件名称（example）
     */
    public static String trimName(String fileName) {
        if (fileName == null) return null;
        int pathLIx = fileName.lastIndexOf("/");
        int splitLIx = fileName.lastIndexOf(".");
        // -example.jpg
        fileName = (pathLIx == -1) ? fileName : fileName.substring(pathLIx + 1);
        // -example
        return (splitLIx == -1) ? fileName : fileName.substring(0, splitLIx);
    }

    /**
     * 去除文件后缀名中的.
     *
     * @param suffix 文件后缀名（xxx.jpg、.png、.webp、...）
     * @return 文件后缀名（jpg、png、webp、...）
     */
    public static String trimSuffix(String suffix) {
        if (StrUtil.isBlank(suffix)) return null;
        int splitIndex = suffix.lastIndexOf(".");
        // xxx
        if (splitIndex == -1) {
            return suffix;
        }
        // .xxx
        if (splitIndex == 0) {
            return suffix.substring(1);
        }
        // filename.suffix
        return suffix.substring(splitIndex + 1);
    }

}
