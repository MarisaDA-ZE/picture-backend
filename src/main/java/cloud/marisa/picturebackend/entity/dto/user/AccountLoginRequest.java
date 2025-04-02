package cloud.marisa.picturebackend.entity.dto.user;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author MarisaDAZE
 * @description 账号登录的DTO对象
 * @date 2025/3/28
 */
@Data
@ToString
public class AccountLoginRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 登录账号
     */
    private String userAccount;

    /**
     * 登录密码
     */
    private String userPassword;
}
