package cloud.marisa.picturebackend.mapper;

import cloud.marisa.picturebackend.entity.dao.Picture;
import cloud.marisa.picturebackend.entity.dto.analyze.request.SpaceCategoryAnalyzeRequest;
import cloud.marisa.picturebackend.entity.dto.analyze.request.SpaceUserAnalyzeRequest;
import cloud.marisa.picturebackend.entity.dto.analyze.response.SpaceCategoryAnalyzeResponse;
import cloud.marisa.picturebackend.entity.dto.analyze.response.SpaceUserAnalyzeResponse;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Marisa
 * @description 针对表【picture(图片表)】的数据库操作Mapper
 * @createDate 2025-03-29 22:12:31
 * @Entity cloud.marisa.picturebackend.entity.dao.Picture
 */
public interface PictureMapper extends BaseMapper<Picture> {

    /**
     * 统计满足条件的分类信息的数量
     * <p>按照 分类名称-数量 的方式进行统计</p>
     *
     * @param request 参数DTO
     * @return 结果
     */
    List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(
            @Param("request") SpaceCategoryAnalyzeRequest request);
}




