package cloud.marisa.picturebackend.entity.vo;

import cloud.marisa.picturebackend.entity.dao.Space;
import lombok.Data;
import lombok.ToString;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * 空间表
 *
 * @TableName space
 */
@Data
@ToString
public class SpaceVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间等级（0-普通版、1-专业版、2-旗舰版）
     */
    private Integer spaceLevel;

    /**
     * 最大存储空间（Byte）
     */
    private Long maxSize;

    /**
     * 最大存储数量（张）
     */
    private Integer maxCount;

    /**
     * 已使用存储空间（Byte）
     */
    private Long totalSize;

    /**
     * 已存储图片数量（张）
     */
    private Integer totalCount;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 修改时间
     */
    private Date editTime;

    /**
     * 用户信息
     */
    private UserVo user;

    /**
     * DAO转VO
     *
     * @param space 空间对象DAO
     * @return 空间VO
     */
    public static SpaceVo toVo(Space space) {
        if (space == null) {
            return null;
        }
        SpaceVo spaceVo = new SpaceVo();
        BeanUtils.copyProperties(space, spaceVo);
        return spaceVo;
    }

    /**
     * VO转DAO
     *
     * @param spaceVo 空间VO
     * @return 空间DAO
     */
    public static Space toDao(SpaceVo spaceVo) {
        if (spaceVo == null) {
            return null;
        }
        Space space = new Space();
        BeanUtils.copyProperties(spaceVo, space);
        return space;
    }
}