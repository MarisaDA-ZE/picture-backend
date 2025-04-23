package cloud.marisa.picturebackend.controller.picture;

import cloud.marisa.picturebackend.annotations.AuthCheck;
import cloud.marisa.picturebackend.annotations.SaSpaceCheckPermission;
import cloud.marisa.picturebackend.api.image.imageexpand.ImageOutPaintingApi;
import cloud.marisa.picturebackend.api.image.imageexpand.entity.response.create.CreateTaskResponse;
import cloud.marisa.picturebackend.api.image.imageexpand.entity.response.query.TaskQueryResponse;
import cloud.marisa.picturebackend.api.image.imagesearch.ImageSearchApiFacade;
import cloud.marisa.picturebackend.api.image.imagesearch.entity.ImageSearchResult;
import cloud.marisa.picturebackend.common.MrsResult;
import cloud.marisa.picturebackend.entity.dao.Picture;
import cloud.marisa.picturebackend.entity.dao.User;
import cloud.marisa.picturebackend.entity.dto.common.DeleteRequest;
import cloud.marisa.picturebackend.entity.dto.picture.*;
import cloud.marisa.picturebackend.entity.vo.PictureVo;
import cloud.marisa.picturebackend.enums.MrsUserRole;
import cloud.marisa.picturebackend.enums.ReviewStatus;
import cloud.marisa.picturebackend.exception.BusinessException;
import cloud.marisa.picturebackend.exception.ErrorCode;
import cloud.marisa.picturebackend.exception.ThrowUtils;
import cloud.marisa.picturebackend.manager.auth.StpKit;
import cloud.marisa.picturebackend.manager.auth.constant.SpaceUserPermissionConstants;
import cloud.marisa.picturebackend.service.IPictureService;
import cloud.marisa.picturebackend.service.ISpaceAnalyzeService;
import cloud.marisa.picturebackend.service.IUserService;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static cloud.marisa.picturebackend.common.Constants.MAX_PAGE_SIZE;
import static cloud.marisa.picturebackend.common.Constants.PICTURE_CACHE_PREFIX;

/**
 * @author MarisaDAZE
 * @description 图片控制类
 * @date 2025/3/29
 */
@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/picture")
public class PictureController {

    @Value("${mrs.color-search.similarity}")
    private Float colorSimilarity;

    /**
     * 用户服务
     */
    private final IUserService userService;

    /**
     * 图片服务
     */
    private final IPictureService pictureService;

    /**
     * 空间统计服务
     */
    private final ISpaceAnalyzeService spaceAnalyzeService;

    /**
     * AI扩图服务API
     */
    private final ImageOutPaintingApi imageOutPaintingApi;

    /**
     * redis客户端
     */
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 本地缓存Caffeine
     */
    private final Cache<String, String> PICTURE_LOCAL_CACHE = Caffeine.newBuilder()
            .maximumSize(10000L)
            // 5分钟后过期（5*60=300）
            .expireAfterWrite(300, TimeUnit.SECONDS)
            .build();

    /**
     * 上传图片文件
     * <p>允许重传（覆盖）</p>
     *
     * @param multipartFile 文件对象
     * @return 图片VO对象
     */
    @PostMapping("/upload")
    @SaSpaceCheckPermission(SpaceUserPermissionConstants.PICTURE_UPLOAD)
    public MrsResult<?> uploadPicture(
            @RequestPart(name = "file") MultipartFile multipartFile,
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest httpServletRequest) {
        log.info("前端算的MD5： {}", pictureUploadRequest.getMd5());
        User loginUser = userService.getLoginUser(httpServletRequest);
        PictureVo pictureVo = pictureService.saveOrUpdatePicture(multipartFile, pictureUploadRequest, loginUser);
        return MrsResult.ok(pictureVo);
    }

    /**
     * 上传图片文件
     * <p>允许重传（覆盖）</p>
     *
     * @param pictureUploadRequest 上传数据的DTO封装
     * @param httpServletRequest   HttpServlet请求对象
     * @return 一个PictureVo
     */
    @PostMapping("/upload/url")
    @SaSpaceCheckPermission(SpaceUserPermissionConstants.PICTURE_UPLOAD)
    public MrsResult<?> uploadURLPicture(
            @RequestBody PictureUploadRequest pictureUploadRequest,
            HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        String fileURL = pictureUploadRequest.getFileUrl();
        PictureVo pictureVo = pictureService.saveOrUpdatePicture(fileURL, pictureUploadRequest, loginUser);
        return MrsResult.ok(pictureVo);
    }

    /**
     * 批量上传图片
     * <p>仅管理员可用</p>
     *
     * @param uploadBatchRequest 批量上传参数的DTO封装
     * @param httpServletRequest HttpServlet请求对象
     * @return 成功上传的条数
     */
    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = MrsUserRole.ADMIN)
    public MrsResult<?> uploadBatchPicture(
            @RequestBody PictureUploadBatchRequest uploadBatchRequest,
            HttpServletRequest httpServletRequest) {
        if (uploadBatchRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loggedUser = userService.getLoginUser(httpServletRequest);
        Integer successCount = pictureService.uploadPictureBatch(uploadBatchRequest, loggedUser);
        return MrsResult.ok(successCount);
    }

    /**
     * 删除图片
     *
     * @param deleteRequest      删除请求的DTO
     * @param httpServletRequest HTTPServlet 请求对象
     * @return .
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(SpaceUserPermissionConstants.PICTURE_DELETE)
    public MrsResult<?> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest httpServletRequest) {
        boolean removed = pictureService.deletePicture(deleteRequest, httpServletRequest);
        return removed ? MrsResult.ok("删除成功") : MrsResult.failed("删除失败");
    }

    /**
     * 管理员更新图片
     *
     * @param updateRequest 更新请求的DTO
     * @return .
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = MrsUserRole.ADMIN)
    public MrsResult<?> updatePicture(@RequestBody PictureUpdateRequest updateRequest) {
        if (updateRequest == null || updateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long pictureId = pictureService.updatePicture(updateRequest);
        return MrsResult.ok(pictureId);
    }

    /**
     * 根据ID查询一张图片
     * <p>仅管理员可用</p>
     *
     * @param pid 图片ID
     * @return 图片对象
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = MrsUserRole.ADMIN)
    public MrsResult<?> getPictureById(@RequestParam(name = "id") Long pid) {
        if (pid == null || pid <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Picture picture = pictureService.getPictureByIdCache(pid);
        return MrsResult.ok(picture);
    }

    /**
     * 根据ID查询一张图片
     *
     * @param pid 图片ID
     * @return 图片VO
     */
    @GetMapping("/get/vo")
    public MrsResult<?> getPictureVoById(
            @RequestParam(name = "id") Long pid,
            HttpServletRequest httpServletRequest) {
        if (pid == null || pid <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        PictureVo pictureVo = pictureService.getPictureVo(pid, httpServletRequest);
        return MrsResult.ok(pictureVo);
    }

    /**
     * 分页查找图片数据
     * <p>仅管理员可用</p>
     *
     * @param queryRequest 查询图片的DTO对象
     * @return 图片VO
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = MrsUserRole.ADMIN)
    public MrsResult<?> listPicturePage(@RequestBody PictureQueryRequest queryRequest) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Page<Picture> picturePage = pictureService.getPicturePage(queryRequest);
        return MrsResult.ok(picturePage);
    }

    /**
     * 分页查找图片数据
     *
     * @param queryRequest 查询图片的DTO对象
     * @return 图片VO
     */
    @PostMapping("/list/page/vo")
    public MrsResult<?> listPicturePageVo(
            @RequestBody PictureQueryRequest queryRequest,
            HttpServletRequest httpServletRequest) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        int size = queryRequest.getPageSize();
        if (size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分页参数过大");
        }
        Long spaceId = queryRequest.getSpaceId();
        if (spaceId == null) {
            // 公共空间
            queryRequest.setNullSpaceId(true);
            queryRequest.setReviewStatus(ReviewStatus.PASS.getValue());
        } else {
            // 私有空间
            queryRequest.setNullSpaceId(false);
            /* 使用sa-token框架提供的权限校验替换原本根据用户的权限校验
             * User loggedUser = userService.getLoginUser(httpServletRequest);
             * Space space = spaceService.getById(spaceId);
             * ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND, "空间不存在");
             * if (!Objects.equals(space.getUserId(), loggedUser.getId())) {
             *     throw new BusinessException(ErrorCode.AUTHORIZATION_ERROR, "无空间访问权限");
             * }
             */
            boolean canView = StpKit.SPACE.hasPermission(SpaceUserPermissionConstants.PICTURE_VIEW);
            ThrowUtils.throwIf(!canView, ErrorCode.AUTHORIZATION_ERROR);
        }
        Page<Picture> picturePage = pictureService.getPicturePage(queryRequest);
        Page<PictureVo> voPage = pictureService.getPictureVoPage(picturePage, httpServletRequest);
        return MrsResult.ok(voPage);
    }

    /**
     * 分页查找图片数据
     * <p>有二级缓存</p>
     *
     * @param queryRequest 查询图片的DTO对象
     * @return 图片VO
     */
    @PostMapping("/list/page/vo/cache")
    public MrsResult<?> listPicturePageVoCache(
            @RequestBody PictureQueryRequest queryRequest,
            HttpServletRequest httpServletRequest) {
        // TODO: 缓存方案不能直接这样用，得改
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (queryRequest.getPageSize() > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分页参数过大");
        }
        queryRequest.setReviewStatus(ReviewStatus.PASS.getValue());
        String queryCondition = JSONUtil.toJsonStr(queryRequest);
        String md5Hex = DigestUtils.md5DigestAsHex(queryCondition.getBytes(StandardCharsets.UTF_8));
        String hashKey = PICTURE_CACHE_PREFIX + md5Hex;
        // 二级缓存架构
        String localCached = PICTURE_LOCAL_CACHE.getIfPresent(hashKey);
        // 是否在本地缓存中，注意：不要用isNotBlank， 这可能会有缓存穿透的问题
        if (localCached != null) {
            Page<?> pageCache = JSONUtil.toBean(localCached, Page.class);
            return MrsResult.ok(pageCache);
        }
        String redisCached = redisTemplate.opsForValue().get(hashKey);
        // 是否在redis缓存中
        if (redisCached != null) {
            // 更新本地缓存
            PICTURE_LOCAL_CACHE.put(hashKey, redisCached);
            Page<?> pageCache = JSONUtil.toBean(redisCached, Page.class);
            return MrsResult.ok(pageCache);
        }
        // 均未命中，从数据库获取
        Page<Picture> picturePage = pictureService.getPicturePage(queryRequest);
        Page<PictureVo> voPage = pictureService.getPictureVoPage(picturePage, httpServletRequest);
        String cacheJSON = JSONUtil.toJsonStr(voPage);
        // 更新本地缓存
        PICTURE_LOCAL_CACHE.put(hashKey, cacheJSON);
        // 5分钟的随机过期，防止缓存雪崩
        int offset = RandomUtil.randomInt(0, 300);
        redisTemplate.opsForValue().set(hashKey, cacheJSON, 300 + offset, TimeUnit.SECONDS);
        return MrsResult.ok(voPage);
    }

    /**
     * 以图搜图接口
     *
     * @param pictureRequest 源图请求参数的DTO封装
     * @return 相似图列表
     */
    @PostMapping("/search/picture")
    public MrsResult<?> searchPictureByPicture(
            @RequestBody SearchPictureByPictureRequest pictureRequest,
            HttpServletRequest httpServletRequest) {
        if (pictureRequest == null ||
                pictureRequest.getPictureId() == null ||
                pictureRequest.getPictureId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long pictureId = pictureRequest.getPictureId();
        Picture dbPicture = pictureService.getById(pictureId);
        if (dbPicture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "图片不存在");
        }
        // 图片空间ID不为空，说明是私有图片
        Long spaceId = dbPicture.getSpaceId();
        if (spaceId != null) {
            /* TODO: 已改版，以图搜图可以在公开图库使用，但需登录且每天有使用次数限制
             Long userId = dbPicture.getUserId();
             User loginUser = userService.getLoginUser(httpServletRequest);
             if (!loginUser.getId().equals(userId)) {
                 throw new BusinessException(ErrorCode.AUTHORIZATION_ERROR, "无空间访问权限");
             }
            */
            MrsUserRole currentRole = userService.getCurrentUserRole(httpServletRequest);
            if (currentRole == MrsUserRole.BAN) {
                throw new BusinessException(ErrorCode.AUTHORIZATION_ERROR, "该账号已被封禁");
            }
            if (!userService.hasPermission(currentRole, MrsUserRole.USER)) {
                throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "登录后可使用该功能");
            }
        }
        List<ImageSearchResult> imageSearchResults = ImageSearchApiFacade.searchImageByURL(dbPicture.getUrl());
        return MrsResult.ok(imageSearchResults);
    }

    /**
     * 根据颜色搜索相近色调的图片
     *
     * @param queryRequest 查询请求DTO对象
     * @param request      httpServlet请求对象
     * @return 搜索到的图片结果（分页）
     */
    @PostMapping("/search/color")
    public MrsResult<?> searchPictureByColor(
            @RequestBody PictureQueryRequest queryRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);
        // 颜色近似度（越小越耗费性能）
        if (colorSimilarity != null && colorSimilarity != 0) {
            queryRequest.setPicSimilarity(colorSimilarity);
        }
        Long spaceId = queryRequest.getSpaceId();
        List<PictureVo> pictureByColor = pictureService.getPictureByColor(spaceId, queryRequest, request);
        return MrsResult.ok(pictureByColor);
    }


    /**
     * 编辑图片
     *
     * @param editRequest    编辑图片的DTO对象
     * @param servletRequest HttpServlet请求对象
     * @return .
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(SpaceUserPermissionConstants.PICTURE_EDIT)
    public MrsResult<?> editPicture(
            @RequestBody PictureEditRequest editRequest,
            HttpServletRequest servletRequest) {
        if (editRequest == null || editRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(servletRequest);
        Long pictureId = pictureService.editPicture(editRequest, loginUser);
        return MrsResult.ok(pictureId);
    }

    /**
     * 批量编辑图片
     *
     * @param editRequest    批量编辑图片的DTO封装
     * @param servletRequest HttpServlet请求对象
     * @return .
     */
    @PostMapping("/edit/batch")
    @SaSpaceCheckPermission(SpaceUserPermissionConstants.PICTURE_EDIT)
    public MrsResult<?> editBatchPicture(
            @RequestBody PictureEditBatchRequest editRequest,
            HttpServletRequest servletRequest) {
        User loginUser = userService.getLoginUser(servletRequest);
        boolean updated = pictureService.editPictureBatch(editRequest, loginUser);
        if (updated) {
            return MrsResult.ok("更新成功", true);
        }
        return MrsResult.failed();
    }

    /**
     * AI扩图服务
     *
     * @param taskRequest    AI扩图的请求参数封装
     * @param servletRequest HttpServlet请求对象
     * @return .
     */
    @PostMapping("/out_painting/create_task")
    @SaSpaceCheckPermission(SpaceUserPermissionConstants.PICTURE_EDIT)
    public MrsResult<?> createOutPaintingTask(
            @RequestBody PictureOutPaintingTaskRequest taskRequest,
            HttpServletRequest servletRequest) {
        if (taskRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(servletRequest);
        CreateTaskResponse createTaskResponse = pictureService.createOutPaintingTask(taskRequest, loginUser);
        // TODO: 对不同用户应该有次数限制
        return MrsResult.ok(createTaskResponse);
    }

    /**
     * AI扩图服务的异步任务查询
     *
     * @param taskId AI扩图的任务ID
     * @return .
     */
    @GetMapping("/out_painting/get_task")
    @SaSpaceCheckPermission(SpaceUserPermissionConstants.PICTURE_EDIT)
    public MrsResult<?> queryOutPaintingTask(
            @RequestParam(name = "taskId") String taskId) {
        if (StrUtil.isBlank(taskId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        TaskQueryResponse taskQueryResponse = imageOutPaintingApi.queryTask(taskId);
        return MrsResult.ok(taskQueryResponse);
    }

    /**
     * 图片信息审核
     * <p>仅管理员可用</p>
     *
     * @param reviewRequest  图片审核参数的DTO封装
     * @param servletRequest HTTPServlet请求对象
     * @return .
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = MrsUserRole.ADMIN)
    public MrsResult<?> reviewPicture(
            @RequestBody PictureReviewRequest reviewRequest,
            HttpServletRequest servletRequest) {
        if (reviewRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        pictureService.doPictureReview(reviewRequest, servletRequest);
        return MrsResult.ok();
    }

    /**
     * 生成默认标签和分类
     *
     * @return 标签和分类
     */
    @GetMapping("/tag_category")
    public MrsResult<PictureTagCategory> listPictureTagCategory() {
        List<String> categoryList = null;
        List<String> tagList = null;
        String categoriesKey = "hot:picture-categories";
        String tagsKey = "hot:picture-tags";
        String categoriesJSON = redisTemplate.opsForValue().get(categoriesKey);
        String tagsJSON = redisTemplate.opsForValue().get(tagsKey);
        if (categoriesJSON != null) {
            categoryList = JSONUtil.toList(categoriesJSON, String.class);
        }
        if (tagsJSON != null) {
            tagList = JSONUtil.toList(tagsJSON, String.class);
        }

        if (categoryList == null) {
            categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
            List<String> hotCategories = spaceAnalyzeService.getPublicHotCategories(6);
            log.info("hot categories: {}", hotCategories);
            if (hotCategories != null && !hotCategories.isEmpty()) {
                categoryList = hotCategories;
            }
            // 更新缓存
            String value = JSONUtil.toJsonStr(categoryList);
            // 分类一天变更一次
            redisTemplate.opsForValue().set(categoriesKey, value, 1, TimeUnit.DAYS);
        }

        if (tagList == null) {
            tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
            List<String> hotTags = spaceAnalyzeService.getPublicHotTags(10);
            log.info("hot tags: {}", hotTags);
            if (hotTags != null && !hotTags.isEmpty()) {
                tagList = hotTags;
            }
            // 更新缓存
            String value = JSONUtil.toJsonStr(tagList);
            // 分类一天变更一次
            redisTemplate.opsForValue().set(tagsKey, value, 1, TimeUnit.DAYS);
        }
        PictureTagCategory tagCategory = new PictureTagCategory();
        tagCategory.setTagList(tagList);
        tagCategory.setCategoryList(categoryList);
        return MrsResult.ok(tagCategory);
    }

    /**
     * 测试上传文件
     *
     * @param multipartFile 文件对象
     * @return 文件URL地址
     */
    @PostMapping("/test/upload")
    @AuthCheck(mustRole = MrsUserRole.ADMIN)
    public MrsResult<?> testUpload(@RequestPart(name = "file") MultipartFile multipartFile) {
        String url = pictureService.upload(multipartFile);
        return MrsResult.ok(url);
    }

    /**
     * 测试下载文件
     *
     * @param fileName 图片的文件名（example.jpg，不含图片在minIO中的路径）
     * @param response HTTP响应体
     */
    @GetMapping("/test/download")
    @AuthCheck(mustRole = MrsUserRole.ADMIN)
    public void testDownload(@RequestParam("name") String fileName, HttpServletResponse response) {
        try (InputStream is = pictureService.downloadPicture(fileName)) {
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
            ServletOutputStream os = response.getOutputStream();
            byte[] bytes = new byte[2048];
            int read;
            while ((read = is.read(bytes)) != -1) {
                os.write(bytes, 0, read);
            }
            os.flush();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
    }
}
