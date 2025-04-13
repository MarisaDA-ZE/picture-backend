package cloud.marisa.picturebackend.entity.dto.spaceuser;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author MarisaDAZE
 * @description 空间用户修改请求DTO
 * @date 2025/4/13
 */
@Data
@ToString
public class SpaceUserEditRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 空间-用户关联ID
     */
    private Long id;

    /**
     * 空间角色（viewer、editor、admin）
     */
    private String spaceRole;
}
