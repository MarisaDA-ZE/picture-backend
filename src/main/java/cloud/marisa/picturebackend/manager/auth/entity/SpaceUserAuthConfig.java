package cloud.marisa.picturebackend.manager.auth.entity;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * @author MarisaDAZE
 * @description 团队空间权限配置
 * @date 2025/4/14
 */
@Data
@ToString
public class SpaceUserAuthConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 角色列表
     */
    private List<SpaceUserRole> roles;

    /**
     * 权限列表
     */
    private List<SpaceUserPermission> permissions;
}
