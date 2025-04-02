package cloud.marisa.picturebackend.entity.vo;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * @author MarisaDAZE
 * @description 用户的VO类
 * @date 2025/3/28
 */
@Data
@ToString
public class UserVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 昵称
     */
    private String userName;

    /**
     * 用户角色（user、admin）
     */
    private String userRole;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱号
     */
    private String email;

    /**
     * 个人描述
     */
    private String profile;

    /**
     * 用户头像（URL地址）
     */
    private String userAvatar;

    /**
     * 逻辑删除（0：未删除，1：已删除）
     */
    private Integer isDelete;

    /**
     * 注册时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 编辑时间
     */
    private Date editTime;
}
