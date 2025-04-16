package cloud.marisa.picturebackend.entity.dao;

import cloud.marisa.picturebackend.annotations.MrsFieldName;
import cloud.marisa.picturebackend.entity.vo.UserVo;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Data;
import lombok.ToString;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * 用户表
 *
 * @TableName user
 */
@Data
@ToString
@TableName(value = "user")
public class User implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（ASSIGN_ID：模式为雪花ID）
     */
    @TableId(type = IdType.ASSIGN_ID)
    @MrsFieldName("id")
    private Long id;

    /**
     * 账号
     */
    @TableField("account")
    @MrsFieldName("account")
    private String userAccount;

    /**
     * 昵称
     */
    @TableField("nick_name")
    @MrsFieldName("userName")
    private String userName;

    /**
     * 密码
     */
    @TableField("password")
    private String userPassword;

    /**
     * 用户角色（user、admin、...）
     * 具体参考UserRole枚举类中的类型
     */
    @TableField("role")
    @MrsFieldName("role")
    private String userRole;

    /**
     * 手机号
     */
    @MrsFieldName("phone")
    private String phone;

    /**
     * 邮箱号
     */
    @MrsFieldName("email")
    private String email;

    /**
     * 个人描述
     */
    private String profile;

    /**
     * 用户头像（URL地址）
     */
    @TableField("avatar")
    private String userAvatar;

    /**
     * 逻辑删除（0：未删除，1：已删除）
     */
    @TableField("is_delete")
    private Integer isDelete;

    /**
     * 注册时间
     */
    @TableField("create_time")
    @MrsFieldName("createTime")
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    @MrsFieldName("updateTime")
    private Date updateTime;

    /**
     * 编辑时间
     */
    @TableField("edit_time")
    @MrsFieldName("editTime")
    private Date editTime;
}