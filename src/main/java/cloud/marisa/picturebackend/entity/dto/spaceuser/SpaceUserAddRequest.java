package cloud.marisa.picturebackend.entity.dto.spaceuser;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author MarisaDAZE
 * @description 空间用户添加请求DTO
 * @date 2025/4/13
 */
@Data
@ToString
public class SpaceUserAddRequest implements Serializable {
    private static final long serialVersionUID = 1L;

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
}
