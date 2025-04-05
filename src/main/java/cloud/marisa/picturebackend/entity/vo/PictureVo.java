package cloud.marisa.picturebackend.entity.vo;

import cloud.marisa.picturebackend.entity.dao.Picture;
import cloud.marisa.picturebackend.entity.dao.User;
import cloud.marisa.picturebackend.enums.UserRole;
import cloud.marisa.picturebackend.util.EnumUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.Data;
import lombok.ToString;
import org.springframework.beans.BeanUtils;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * @author MarisaDAZE
 * @description 图片的VO类
 * @date 2025/3/29
 */
@Data
@ToString
public class PictureVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户信息
     */
    private UserVo userVo;

    /**
     * 图片URL地址
     */
    private String url;

    /**
     * 缩略图 url
     */
    private String thumbnailUrl;
    /**
     * 缩略图 url
     */
    private String originalUrl;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 权限操作列表
     */
    private List<String> permissionList;

    /**
     * 图片描述
     */
    private String introduction;

    /**
     * 图片分类
     */
    private String category;

    /**
     * 图片标签数组
     */
    private List<String> tags;

    /**
     * 图片大小
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 图片长宽比
     */
    private Double picScale;

    /**
     * 图片格式（.jpg）
     */
    private String picFormat;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * DAO转VO
     *
     * @param picture 图片dao
     * @return 图片vo
     */
    public static PictureVo toVO(Picture picture) {
        if (ObjectUtils.isEmpty(picture)) {
            return null;
        }
        if (StrUtil.isBlank(picture.getIntroduction())) {
            picture.setIntroduction(null);
        }
        if (StrUtil.isBlank(picture.getCategory())) {
            picture.setCategory(null);
        }
        PictureVo pictureVo = new PictureVo();
        BeanUtils.copyProperties(picture, pictureVo);
        List<String> collect = JSONUtil.toList(picture.getTags(), String.class);
        pictureVo.setTags(collect);
        return pictureVo;
    }

    /**
     * VO转DAO
     *
     * @param pictureVo 图片vo
     * @return 图片dao
     */
    public static Picture toDAO(PictureVo pictureVo) {
        if (ObjectUtils.isEmpty(pictureVo)) {
            return null;
        }
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureVo, picture);
        picture.setTags(JSONUtil.toJsonStr(pictureVo.getTags()));
        return picture;
    }
}