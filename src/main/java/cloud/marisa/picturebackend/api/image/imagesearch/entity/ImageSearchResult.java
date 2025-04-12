package cloud.marisa.picturebackend.api.image.imagesearch.entity;

import lombok.Data;

/**
 * @author MarisaDAZE
 * @description 图片搜索封装
 * @date 2025/4/5
 */
@Data
public class ImageSearchResult {

    /**
     * 缩略图地址
     */
    private String thumbUrl;

    /**
     * 来源地址
     */
    private String fromUrl;
}

