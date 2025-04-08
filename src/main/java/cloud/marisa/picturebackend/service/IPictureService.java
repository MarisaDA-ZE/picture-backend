package cloud.marisa.picturebackend.service;

import cloud.marisa.picturebackend.entity.dao.Picture;
import cloud.marisa.picturebackend.entity.dao.User;
import cloud.marisa.picturebackend.entity.dto.common.DeleteRequest;
import cloud.marisa.picturebackend.entity.dto.picture.*;
import cloud.marisa.picturebackend.entity.vo.PictureVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.util.List;

/**
 * @author Marisa
 * @description 针对表【picture(图片表)】的数据库操作Service
 * @createDate 2025-03-29 22:12:31
 */
public interface IPictureService extends IService<Picture> {


    /**
     * 上传一张图片到存储桶
     *
     * @param multipartFile 图片文件
     * @return 访问地址
     */
    String upload(MultipartFile multipartFile);

    /**
     * 上传一张图片到数据库
     *
     * @param inputSource   上传源（文件对象/URL）
     * @param uploadRequest 上传参数的DTO封装
     * @param loginUser     登录用户信息
     * @return 图片VO
     */
    PictureVo uploadPicture(Object inputSource, PictureUploadRequest uploadRequest, User loginUser);

    /**
     * 批量上传图片
     *
     * @param uploadBatchRequest 批量上传参数的DTO封装
     * @param loggedUser         当前登录用户
     * @return 入库成功的数量
     */
    Integer uploadPictureBatch(PictureUploadBatchRequest uploadBatchRequest, User loggedUser);

    /**
     * 用户更新图片信息
     *
     * @param editRequest 更新参数的DTO封装
     * @param loggedUser  登录用户
     * @return 的图片ID
     */
    Long editPicture(PictureEditRequest editRequest, User loggedUser);

    /**
     * 管理员更新图片信息
     *
     * @param updateRequest 更新参数的DTO封装
     * @return 图片的ID
     */
    Long updatePicture(PictureUpdateRequest updateRequest);

    /**
     * 下载一张图片
     * <p>这个方法只能用于下载"pictures"目录下的图片</p>
     *
     * @param fileName 图片地址
     * @return 图片文件流
     */
    InputStream downloadPicture(String fileName);

    /**
     * 删除一张图片
     *
     * @param deleteRequest      删除请求的DTO
     * @param httpServletRequest HTTPServlet 请求对象
     * @return 是否删除成功
     */
    boolean deletePicture(DeleteRequest deleteRequest, HttpServletRequest httpServletRequest);

    /**
     * 图片DAO转VO
     *
     * @param pid                图片ID
     * @param httpServletRequest httpServlet请求对象
     * @return 图片VO对象
     */
    PictureVo getPictureVo(Long pid, HttpServletRequest httpServletRequest);

    /**
     * 获取图片信息（分页）
     *
     * @param queryRequest 查询请求的DTO对象
     * @return 分页对象
     */
    Page<Picture> getPicturePage(PictureQueryRequest queryRequest);

    /**
     * 获取图片信息（分页）
     *
     * @param picturePage    DAO的分页对象
     * @param servletRequest HttpServlet请求对象
     * @return VO的分页对象
     */
    Page<PictureVo> getPictureVoPage(Page<Picture> picturePage, HttpServletRequest servletRequest);

    /**
     * 根据颜色获取最接近的图片
     * <p>默认前12张</p>
     *
     * @param spaceId            空间ID
     * @param queryRequest       查询对象
     * @param httpServletRequest httpServlet请求对象
     * @return 图片列表
     */
    List<PictureVo> getPictureByColor(Long spaceId, PictureQueryRequest queryRequest, HttpServletRequest httpServletRequest);

    /**
     * 校验图片信息
     * <p>不通过时会抛出参数错误</p>
     *
     * @param picture 图片信息
     */
    void validPicture(Picture picture);

    /**
     * 审核图片
     *
     * @param reviewRequest  审核参数的DTO封装
     * @param servletRequest HTTPServlet请求对象
     */
    void doPictureReview(PictureReviewRequest reviewRequest, HttpServletRequest servletRequest);

    /**
     * 填充审核参数
     *
     * @param picture   图片DAO对象
     * @param loginUser 登录用户对象
     */
    void fillReviewParams(Picture picture, User loginUser);

    /**
     * 校验用户是否有操作该图片的权限
     *
     * @param picture   图片
     * @param loginUser 登录用户
     */
    void checkPictureAuth(Picture picture, User loginUser);
}
