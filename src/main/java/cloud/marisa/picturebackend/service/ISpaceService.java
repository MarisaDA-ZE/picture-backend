package cloud.marisa.picturebackend.service;

import cloud.marisa.picturebackend.entity.dao.Space;
import cloud.marisa.picturebackend.entity.dao.User;
import cloud.marisa.picturebackend.entity.dto.space.SpaceAddRequest;
import cloud.marisa.picturebackend.entity.dto.space.SpaceUpdateRequest;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author Marisa
 * @description 针对表【space(空间表)】的数据库操作Service
 * @createDate 2025-04-04 11:02:54
 */
public interface ISpaceService extends IService<Space> {


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

}
