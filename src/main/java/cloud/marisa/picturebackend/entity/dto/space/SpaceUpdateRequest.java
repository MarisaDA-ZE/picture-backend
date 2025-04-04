package cloud.marisa.picturebackend.entity.dto.space;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author MarisaDAZE
 * @description 更新空间参数的DTO封装
 * @date 2025/4/4
 */
@Data
@ToString
public class SpaceUpdateRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

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
}
