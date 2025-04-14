package cloud.marisa.picturebackend.manager.auth.entity;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * @author MarisaDAZE
 * @description 团队成员角色
 * @date 2025/4/14
 */
@Data
@ToString
public class SpaceUserRole implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 键名
     */
    private String key;

    /**
     * 权限名称
     */
    private String name;

    /**
     * 权限键列表
     */
    private List<String> permissions;

    /**
     * 权限描述
     */
    private String description;
}
