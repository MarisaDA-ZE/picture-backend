package cloud.marisa.picturebackend.entity.dto.user;

import cloud.marisa.picturebackend.entity.dto.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author MarisaDAZE
 * @description 查询用户的DTO对象
 * @date 2025/3/29
 */

@Data
@ToString
@EqualsAndHashCode(callSuper = true)
public class QueryUserRequest extends PageRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long id;

    /** 账户名 */
    private String userAccount;

    /** 昵称 */
    private String userName;

    /** 角色 */
    private String userRole;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;
}
