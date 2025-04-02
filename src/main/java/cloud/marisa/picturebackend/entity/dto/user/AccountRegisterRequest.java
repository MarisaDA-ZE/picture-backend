package cloud.marisa.picturebackend.entity.dto.user;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author MarisaDAZE
 * @description 账号注册的DTO对象
 * @date 2025/3/28
 */
@Data
@ToString
public class AccountRegisterRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 注册账号
     */
    private String userAccount;

    /**
     * 用户密码
     */
    private String userPassword;

    /**
     * 二次确认的密码
     */
    private String checkPassword;
}
