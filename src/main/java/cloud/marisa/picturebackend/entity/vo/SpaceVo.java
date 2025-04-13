package cloud.marisa.picturebackend.entity.vo;

import cloud.marisa.picturebackend.entity.dao.Space;
import lombok.Data;
import lombok.ToString;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
     * 空间类型：0-私有 1-团队
     */
    private Integer spaceType;


    /**
     * 权限操作列表
     */
    private List<String> permissionList;

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
        // 0 私有空间，1 团队空间，2 不知道，不显示，应该不是spaceLevel
        // spaceVo.setSpaceType(0);
        // spaceVo.setPermissionList(getPermissions());
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

    /**
     * 前端用的权限列表
     * <p>根据前端枚举出来的（气</p>
     *
     * @return .
     */
    private static List<String> getPermissions() {
        ArrayList<String> permissions = new ArrayList<>();
        // permissions.add("spaceUser:manage");
        permissions.add("picture:view");
        permissions.add("picture:upload");
        permissions.add("picture:edit");
        permissions.add("picture:delete");
        return permissions;
    }
}