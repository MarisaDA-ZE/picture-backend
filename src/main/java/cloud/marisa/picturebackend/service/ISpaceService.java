package cloud.marisa.picturebackend.service;

import cloud.marisa.picturebackend.entity.dao.Space;
import cloud.marisa.picturebackend.entity.dao.User;
import cloud.marisa.picturebackend.entity.dto.common.DeleteRequest;
import cloud.marisa.picturebackend.entity.dto.space.SpaceAddRequest;
import cloud.marisa.picturebackend.entity.dto.space.SpaceQueryRequest;
import cloud.marisa.picturebackend.entity.dto.space.SpaceUpdateRequest;
import cloud.marisa.picturebackend.entity.vo.SpaceVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;


/**
 * @author Marisa
 * @description 针对表【space(空间表)】的数据库操作Service
 * @createDate 2025-04-04 11:02:54
 */
public interface ISpaceService extends IService<Space> {

    /**
     * 查询空间分页信息
     *
     * @param queryRequest 查询数据的DTO封装
     * @param loggedUser   登录用户
     * @return 空间数据的分页对象
     */
    Page<Space> getSpacePage(SpaceQueryRequest queryRequest, User loggedUser);

    /**
     * 查询空间分页Vo信息
     *
     * @param spacePage  空间分页对象
     * @param loggedUser 登录用户
     * @return 空间数据的分页对象
     */
    Page<SpaceVo> getSpacePageVo(Page<Space> spacePage, User loggedUser);

    /**
     * 管理员更新空间信息
     *
     * @param updateRequest 更新空间参数的DTO封装
     * @return 是否更新成功
     */
    boolean updateSpace(SpaceUpdateRequest updateRequest);

    /**
     * 创建一个空间
     *
     * @param addRequest 创建空间参数的DTO封装
     * @param loggedUser 当前登录用户
     * @return 空间id
     */
    long addSpace(SpaceAddRequest addRequest, User loggedUser);

    /**
     * 根据空间类型填充空间信息
     *
     * @param space 空间DAO
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 根据空间ID删除一个空间
     * <p>仅空间创建者 或 管理员可以删除</p>
     *
     * @param deleteRequest 删除数据的DTO封装
     * @return true:删除成功，false:删除失败
     */
    boolean deleteSpaceById(DeleteRequest deleteRequest, User loggedUser);

}
