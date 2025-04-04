package cloud.marisa.picturebackend.entity.dto.space;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author MarisaDAZE
 * @description 修改空间参数的DTO封装
 * @date 2025/4/4
 */
@Data
@ToString
public class SpaceEditRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;

}
