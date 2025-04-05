package cloud.marisa.picturebackend.mapper;

import cloud.marisa.picturebackend.entity.dao.Space;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

/**
 * @author Marisa
 * @description 针对表【space(空间表)】的数据库操作Mapper
 * @createDate 2025-04-04 11:02:54
 * @Entity cloud.marisa.picturebackend.entity.dao.Space
 */
public interface SpaceMapper extends BaseMapper<Space> {

    /**
     * 根据ID查询用户ID
     *
     * @param spaceId 空间ID
     * @return 用户ID
     */
    @Select("SELECT `user_id` FROM `spcae` where `id` = ${spaceId} AND `is_delete` = 0;")
    Long getUserIdBySpaceId(Long spaceId);
}




