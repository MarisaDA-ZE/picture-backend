package cloud.marisa.picturebackend.entity.dto.analyze.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author MarisaDAZE
 * @description 空间统计-标签信息DTO
 * @date 2025/4/12
 */
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SpaceTagAnalyzeResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 标签名称
     */
    private String tag;

    /**
     * 图片数量
     */
    private Long count;

}
