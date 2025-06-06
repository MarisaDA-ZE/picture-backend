package cloud.marisa.picturebackend.entity.vo;

import cloud.marisa.picturebackend.entity.dao.User;
import lombok.Data;
import lombok.ToString;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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



    /**
     * 将User对象转换为UserVo对象
     *
     * @param user 用户DAO对象
     * @return VO对象
     */
    public static UserVo toVO(User user) {
        if (ObjectUtils.isEmpty(user)) {
            return null;
        }
        UserVo userVo = new UserVo();
        BeanUtils.copyProperties(user, userVo);
        return userVo;
    }

    /**
     * 将User对象转换为UserVo对象
     *
     * @param users 用户DAO 列表
     * @return VO列表
     */
    public static List<UserVo> toVoList(List<User> users) {
        if (CollectionUtils.isEmpty(users)) {
            return new ArrayList<>();
        }
        return users.stream()
                .map(UserVo::toVO)
                .collect(Collectors.toList());
    }
}
