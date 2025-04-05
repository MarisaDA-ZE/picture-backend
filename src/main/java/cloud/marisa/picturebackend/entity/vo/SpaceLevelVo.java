package cloud.marisa.picturebackend.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * @author MarisaDAZE
 * @description 空间级别
 * @date 2025/4/4
 */
@Data
@AllArgsConstructor
public class SpaceLevelVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 空间级别
     */
    private int value;

    /**
     * 空间名称
     */
    private String text;

    /**
     * 最大空间存储数量（张）
     */
    private long maxCount;

    /**
     * 最大空间大小（Byte）
     */
    private long maxSize;
}

