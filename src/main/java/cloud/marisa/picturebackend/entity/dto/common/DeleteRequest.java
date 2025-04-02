package cloud.marisa.picturebackend.entity.dto.common;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * @author MarisaDAZE
 * @description 删除包装类
 * @date 2025/3/25
 */

@Data
@ToString
public class DeleteRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 要删除的id
     */
    private Long id;

    /**
     * 批量删除的id列表
     */
    private List<Long> batchIds;
}
