package cloud.marisa.picturebackend.enums;

import lombok.Getter;

/**
 * @author MarisaDAZE
 * @description 用户角色的枚举
 * @date 2025/3/28
 */
@Getter
// 关于接口 MrsBaseEnum<T> ，实现后可以用枚举工具类中的某些方法
public enum UserRole implements MrsBaseEnum<String> {

    /**
     * 被封禁的用户
     */
    BAN("ban", -1),

    /**
     * 游客（或未登录的访问）
     */
    GUEST("guest", 1),

    /**
     * 用户
     */
    USER("user", 2),

    /**
     * 管理员
     */
    ADMIN("admin", 5);

    UserRole(String value, int level) {
        this.level = level;
        this.value = value;
    }

    /** 字符串值 */
    private final String value;

    /** 角色等级，等级越高权限越大 */
    private final Integer level;
}
