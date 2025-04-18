package cloud.marisa.picturebackend.common;


/**
 * @author MarisaDAZE
 * @description Constants.类
 * @date 2025/3/28
 */
public class Constants {

    /**
     * 登录用户的session名称
     */
    public static final String USER_LOGIN = "user_login";

    /**
     * 每页最大条数
     */
    public static final int MAX_PAGE_SIZE = 20;

    /**
     * 缩略图最大尺寸（最长边长）
     */
    public static final int THUMB_MAX_SIZE = 1000;

    /**
     * 图片缓存前缀
     */
    public static final String PICTURE_CACHE_PREFIX = "mrs_picture:picture_vo_page_cache";

    /**
     * 用户服务的缓存前缀
     */
    public static final String USER_CACHE_NAME = "user:";

    /**
     * 用户服务的缓存前缀
     */
    public static final String PICTURE_CACHE_NAME = "picture:";

    /**
     * 空间服务的缓存前缀
     */
    public static final String SPACE_CACHE_NAME = "space:";
}
