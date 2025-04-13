package cloud.marisa.picturebackend.entity.vo;

import cloud.marisa.picturebackend.entity.dao.SpaceUser;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * 空间-用户的关联表视图对象
 */
@Data
@ToString
@EqualsAndHashCode
public class SpaceUserVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 空间ID
     */
    private Long spaceId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 空间角色（viewer、editor、admin）
     */
    private String spaceRole;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 用户VO对象
     */
    private UserVo user;

    /**
     * 空间VO对象
     */
    private SpaceVo space;

    public static SpaceUserVo toVo(SpaceUser dao) {
        if (dao == null) return null;
        SpaceUserVo vo = new SpaceUserVo();
        BeanUtils.copyProperties(dao, vo);
        return vo;
    }

    public static SpaceUser toDao(SpaceUserVo vo) {
        if (vo == null) return null;
        SpaceUser dao = new SpaceUser();
        BeanUtils.copyProperties(vo, dao);
        return dao;
    }
}