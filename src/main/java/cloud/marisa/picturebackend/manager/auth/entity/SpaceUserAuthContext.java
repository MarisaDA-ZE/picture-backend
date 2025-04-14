package cloud.marisa.picturebackend.manager.auth.entity;

import cloud.marisa.picturebackend.entity.dao.Picture;
import cloud.marisa.picturebackend.entity.dao.Space;
import cloud.marisa.picturebackend.entity.dao.SpaceUser;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author MarisaDAZE
 * @description 用户在特定空间的授权上下文，内有用户信息、空间信息和图片信息
 * @date 2025/4/14
 */
@Data
@Builder
@ToString
public class SpaceUserAuthContext implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 临时参数，不同请求对应的 id 可能不同
     */
    private Long id;

    /**
     * 图片 ID
     */
    private Long pictureId;

    /**
     * 空间 ID
     */
    private Long spaceId;

    /**
     * 空间用户 ID
     */
    private Long spaceUserId;

    /**
     * 图片信息
     */
    private Picture picture;

    /**
     * 空间信息
     */
    private Space space;

    /**
     * 空间用户信息
     */
    private SpaceUser spaceUser;
}
