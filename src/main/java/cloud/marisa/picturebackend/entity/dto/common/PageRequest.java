package cloud.marisa.picturebackend.entity.dto.common;

import lombok.Data;
import lombok.ToString;

/**
 * @author MarisaDAZE
 * @description 分页对象
 * @date 2025/3/25
 */
@Data
@ToString
public class PageRequest {

    /**
     * 当前页
     */
    private int current = 1;

    /**
     * 每页条数
     */
    private int pageSize = 10;

    /**
     * 排序字段（如age、level这些）
     */
    private String sortField;

    /**
     * 排序顺序（desc 降序 默认，asc 升序）
     */
    private String sortOrder = "desc";
}
