package cloud.marisa.picturebackend.entity.dto.user;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author MarisaDAZE
 * @description 更新用户的DTO对象
 * @date 2025/3/29
 */
@Data
@ToString
public class UpdateUserRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long id;

    /** 账户名 */
    private String userAccount;

    /** 昵称 */
    private String userName;

    /** 密码 */
    private String userPassword;

    /** 用户角色 */
    private String userRole;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 个人简介 */
    private String profile;

    /** 头像（URL地址） */
    private String userAvatar;
}
