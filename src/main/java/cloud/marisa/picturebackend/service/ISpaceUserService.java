package cloud.marisa.picturebackend.service;

import cloud.marisa.picturebackend.entity.dao.SpaceUser;
import cloud.marisa.picturebackend.entity.dao.User;
import cloud.marisa.picturebackend.entity.dto.common.DeleteRequest;
import cloud.marisa.picturebackend.entity.dto.spaceuser.SpaceUserAddRequest;
import cloud.marisa.picturebackend.entity.dto.spaceuser.SpaceUserEditRequest;
import cloud.marisa.picturebackend.entity.dto.spaceuser.SpaceUserQueryRequest;
import cloud.marisa.picturebackend.entity.vo.SpaceUserVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @author Marisa
 * @description 针对表【space_user(空间-用户的关联表)】的数据库操作Service
 * @createDate 2025-04-13 16:46:09
 */
public interface ISpaceUserService extends IService<SpaceUser> {

    /**
     * 查询空间-用户信息
     *
     * @param queryRequest 查询数据的DTO封装
     * @param loginUser    登录用户
     * @return 空间数据对象
     */
    SpaceUser getSpaceUser(SpaceUserQueryRequest queryRequest, User loginUser);

    /**
     * 根据空间-用户ID查询一条对应的记录
     *
     * @param spaceUser 空间-用户DAO
     * @return 空间用户Vo
     */
    SpaceUserVo getSpaceUserVo(SpaceUser spaceUser);

    /**
     * 查询空间-用户分页信息
     *
     * @param queryRequest 查询数据的DTO封装
     * @param loginUser    登录用户
     * @return 空间数据的分页对象
     */
    List<SpaceUser> getSpaceUserList(SpaceUserQueryRequest queryRequest, User loginUser);

    /**
     * 从空间-用户DAO中获取空间-用户Vo列表
     *
     * @param spaceUsers 空间-用户DAO
     * @return 空间-用户Vo列表
     */
    List<SpaceUserVo> getSpaceUserVoList(List<SpaceUser> spaceUsers);

    /**
     * 创建一个空间-用户对应关系
     *
     * @param addRequest 创建空间参数的DTO封装
     * @param loginUser  当前登录用户
     * @return 空间id
     */
    long addSpaceUser(SpaceUserAddRequest addRequest, User loginUser);

    /**
     * 修改空间-用户对应关系
     *
     * @param editRequest 修改空间参数的DTO封装
     * @param loginUser  当前登录用户
     * @return 空间id
     */
    boolean editSpaceUser(SpaceUserEditRequest editRequest, User loginUser);

    /**
     * 根据空间-用户ID删除一个空间-用户记录
     * <p>仅空间创建者 或 管理员可以删除</p>
     *
     * @param deleteRequest 删除数据的DTO封装
     * @return true:删除成功，false:删除失败
     */
    boolean deleteSpaceUserById(DeleteRequest deleteRequest, User loggedUser);

    /**
     * 根据空间类型填充空间信息
     *
     * @param spaceUser 空间-用户DAO
     */
    void fillSpaceBySpaceLevel(SpaceUser spaceUser);

}
