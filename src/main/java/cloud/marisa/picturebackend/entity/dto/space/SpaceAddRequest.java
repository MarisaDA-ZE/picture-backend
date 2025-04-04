package cloud.marisa.picturebackend.entity.dto.space;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author MarisaDAZE
 * @description 新增空间参数的DTO封装
 * @date 2025/4/4
 */
@Data
@ToString
public class SpaceAddRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间等级（0-普通版、1-专业版、2-旗舰版）
     */
    private Integer spaceLevel;
}
