package cloud.marisa.picturebackend.entity.dto.space;

import cloud.marisa.picturebackend.entity.dto.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author MarisaDAZE
 * @description 查询空间参数的DTO封装
 * @date 2025/4/4
 */

@Data
@ToString
@EqualsAndHashCode(callSuper = true)
public class SpaceQueryRequest extends PageRequest implements Serializable {
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
}
