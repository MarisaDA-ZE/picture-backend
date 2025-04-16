package cloud.marisa.picturebackend.enums;

import cloud.marisa.picturebackend.enums.base.MrsBaseEnum;
import lombok.Getter;

/**
 * @author MarisaDAZE
 * @description 用户角色的枚举
 * @date 2025/3/28
 */
@Getter
// 关于接口 MrsBaseEnum<T> ，实现后可以用枚举工具类中的某些方法
public enum MrsUserRole implements MrsBaseEnum<String> {

    /**
     * 被封禁的用户
     * <p>可以：</p>
     * <p>①浏览、下载公共图片库的图片</p>
     */
    BAN("ban", -1),

    /**
     * 游客（或未登录的访问）
     * <p>可以：</p>
     * <p>①浏览、下载公共图片库的图片</p>
     */
    GUEST("guest", 1),

    /**
     * 用户角色
     * <p>可以：</p>
     * <p>①浏览、下载、分享公共图片库的图片</p>
     * <p>②管理自己的图片库数据</p>
     * <p>③管理公共图片库自己上传的、通过审核的数据</p>
     */
    USER("user", 2),

    /**
     * 管理员角色
     * <p>可以：</p>
     * <p>①浏览、下载、分享公共图片库的图片</p>
     * <p>②管理自己的图片库数据</p>
     * <p>③管理公共图片库的数据</p>
     * <p>④审核用户上传（公共库和私有库）的图片</p>
     */
    ADMIN("admin", 5),

    /**
     * 站长角色
     * <p style="color: #EA5774;">拥有所有权限</p>
     */
    MASTER("master", 10);

    MrsUserRole(String value, int level) {
        this.level = level;
        this.value = value;
    }

    /**
     * 字符串值
     */
    private final String value;

    /**
     * 角色等级，等级越高权限越大
     */
    private final Integer level;

    /**
     * 当前角色权限 ≥ 指定角色权限
     *
     * @param mustRole 需要的权限
     * @return true:大于等于，false:小于
     */
    public boolean moreThanRole(MrsUserRole mustRole) {
        if (mustRole == null) {
            return false;
        }
        return this.getLevel() >= mustRole.getLevel();
    }

    /**
     * 当前角色权限＜指定角色权限
     * @param mustRole  需要的权限
     * @return  true:比需要权限小，false:比需要权限大
     */
    public boolean notThanRole(MrsUserRole mustRole) {
        return !moreThanRole(mustRole);
    }
}
