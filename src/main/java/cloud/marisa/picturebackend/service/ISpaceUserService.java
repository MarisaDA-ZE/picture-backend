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
 * @description 针对表【space_user(团队空间用户表)】的数据库操作Service
 * @createDate 2025-04-13 16:46:09
 */
public interface ISpaceUserService extends IService<SpaceUser> {

    /**
     * 根据条件 获取一个符合条件的 团队空间成员
     *
     * @param queryRequest 查询数据的DTO封装
     * @param loginUser    当前登录用户
     * @return 符合条件的 团队空间成员
     */
    SpaceUser getSpaceUser(SpaceUserQueryRequest queryRequest, User loginUser);

    /**
     * 根据 团队空间成员基础信息，补全其对应的用户信息和空间信息
     *
     * @param spaceUser 团队空间成员对象
     * @return 补全后的团队空间成员Vo
     */
    SpaceUserVo getSpaceUserVo(SpaceUser spaceUser);

    /**
     * 根据条件，查询满足条件的团队空间成员列表
     * <p>这里底层是直接调的list方法</p>
     * <p>但在添加时有做限制，一个空间最多不会超过50个成员</p>
     *
     * @param queryRequest 查询数据的DTO封装
     * @param loginUser    当前登录用户
     * @return 符合条件的 团队空间成员列表
     */
    List<SpaceUser> getSpaceUserList(SpaceUserQueryRequest queryRequest, User loginUser);

    /**
     * 将空间成员列表转换成对应的Vo列表
     *
     * @param spaceUsers 团队空间成员列表
     * @return 团队空间成员的Vo列表
     */
    List<SpaceUserVo> getSpaceUserVoList(List<SpaceUser> spaceUsers);

    /**
     * 向（自己的）团队空间添加一名成员
     *
     * @param addRequest 创建空间参数的DTO封装
     * @param loginUser  当前登录用户
     * @return 团队空间成员的ID（不是人的ID，而是关联表的ID）
     */
    long addSpaceUser(SpaceUserAddRequest addRequest, User loginUser);

    /**
     * 修改（自己的）团队空间的一名成员的信息
     * <p>主要是修改 他在团队空间中的 角色或其它信息</p>
     *
     * @param editRequest 修改空间参数的DTO封装
     * @param loginUser   当前登录用户
     * @return 是否修改成功
     */
    boolean editSpaceUser(SpaceUserEditRequest editRequest, User loginUser);

    /**
     * 根据ID，删除加入（我的）团队空间的一名成员
     * <p>仅空间创建者 或 管理员可以删除</p>
     *
     * @param deleteRequest 删除数据的DTO封装
     * @return 是否删除成功
     */
    boolean deleteSpaceUserById(DeleteRequest deleteRequest, User loggedUser);
}
