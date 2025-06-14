package cloud.marisa.picturebackend.service.impl;

import cloud.marisa.picturebackend.api.image.imageexpand.ImageOutPaintingApi;
import cloud.marisa.picturebackend.api.image.imageexpand.entity.request.ImagePaintingParameters;
import cloud.marisa.picturebackend.api.image.imageexpand.entity.request.ImageRequestParam;
import cloud.marisa.picturebackend.api.image.imageexpand.entity.response.create.CreateTaskResponse;
import cloud.marisa.picturebackend.common.ApiConstants;
import cloud.marisa.picturebackend.entity.dao.*;
import cloud.marisa.picturebackend.entity.dto.api.RandomPictureRequest;
import cloud.marisa.picturebackend.entity.dto.api.SearchPictureRequest;
import cloud.marisa.picturebackend.entity.dto.common.DeleteRequest;
import cloud.marisa.picturebackend.entity.dto.file.UploadPictureResult;
import cloud.marisa.picturebackend.entity.dto.picture.*;
import cloud.marisa.picturebackend.entity.vo.PictureVo;
import cloud.marisa.picturebackend.entity.vo.UserVo;
import cloud.marisa.picturebackend.enums.*;
import cloud.marisa.picturebackend.enums.notice.MrsNoticeRead;
import cloud.marisa.picturebackend.enums.notice.MrsNoticeType;
import cloud.marisa.picturebackend.exception.BusinessException;
import cloud.marisa.picturebackend.exception.ErrorCode;
import cloud.marisa.picturebackend.exception.ThrowUtils;
import cloud.marisa.picturebackend.manager.auth.SpaceUserAuthManager;
import cloud.marisa.picturebackend.manager.auth.StpKit;
import cloud.marisa.picturebackend.manager.auth.constant.SpaceUserPermissionConstants;
import cloud.marisa.picturebackend.manager.upload.AliyunPictureUploadMultipart;
import cloud.marisa.picturebackend.manager.upload.AliyunPictureUploadURL;
import cloud.marisa.picturebackend.mapper.PictureMapper;
import cloud.marisa.picturebackend.queue.OverflowStorageDao;
import cloud.marisa.picturebackend.service.*;
import cloud.marisa.picturebackend.upload.picture.PictureMultipartFileUpload;
import cloud.marisa.picturebackend.upload.picture.PictureUrlUpload;
import cloud.marisa.picturebackend.util.*;
import cloud.marisa.picturebackend.util.cache.MrsCacheUtil;
import cloud.marisa.picturebackend.util.colors.ColorUtils;
import cloud.marisa.picturebackend.util.colors.MrsColorHSV;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static cloud.marisa.picturebackend.common.Constants.PICTURE_CACHE_NAME;


/**
 * <p>这里 Spring 官方更推荐使用构造器的方式注入，而不是传统的属性注入</p>
 * <p>这也是为什么在使用@Autowired注解时IDE会提示不推荐这样用的原因</p>
 * <p>@Resource也可以，它能避免与IOC强耦合，但这里使用更推荐的构造函数注入</p>
 * <p>使用LomBok提供的注解@RequiredArgsConstructor注解可以方便的生成符合需求的构造函数</p>
 * <p>具体可以参考：<a href="https://zhuanlan.zhihu.com/p/25466474643">这篇文章</a></p>
 *
 * @author Marisa
 * @description 针对表【picture(图片表)】的数据库操作Service实现
 * @createDate 2025-03-29 22:12:31
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class PictureServiceImpl
        extends ServiceImpl<PictureMapper, Picture>
        implements IPictureService {

    @Value("${minio.folders.pictures}")
    private String PICTURE_PATH;

    @Value("${mrs.picture-bath-url}")
    private String pictureBathUrl;

    @Value("${mrs.color-search.similarity}")
    private Float similarity;

    /**
     * MinIO分布式存储工具类
     */
    private final MinioUtil minioUtil;

    /**
     * 用户服务
     */
    private final IUserService userService;

    /**
     * 图片空间服务
     */
    private final ISpaceService spaceService;

    /**
     * 通知服务
     */
    private final INoticeService noticeService;

    /**
     * 区域事务模板
     */
    private final TransactionTemplate transactionTemplate;

    /**
     * 是否上传到自建文件存储服务器
     * <p>true-上传到MinIO；false-上传到阿里云</p>
     */
    @Value("${mrs.file.picture.is-use-oss:false}")
    private Boolean useOss;

    /**
     * 图片上传服务（通过URL上传）
     */
    private final PictureUrlUpload pictureUrlUpload;

    /**
     * 图片上传服务（通过文件上传）
     */
    private final PictureMultipartFileUpload pictureMultipartFileUpload;

    /**
     * OSS图片上传服务（通过URL上传）
     */
    private final AliyunPictureUploadURL aliyunPictureUploadURL;

    /**
     * OSS图片上传服务（通过文件上传）
     */
    private final AliyunPictureUploadMultipart aliyunPictureUploadMultipart;
    /**
     * 动态线程池
     */
    private final ThreadPoolExecutor editBatchPoolExecutor;

    /**
     * AI扩图API
     */
    private final ImageOutPaintingApi imageOutPaintingApi;

    /**
     * 团队空间权限管理器
     */
    private final SpaceUserAuthManager spaceUserAuthManager;

    /**
     * Redis缓存模板工具
     */
    public final RedisTemplate<String, Object> redisTemplate;

    /**
     * 简易任务队列
     * <p>如果任务满了，会持久化到Redis</p>
     */
    private final OverflowStorageDao<Picture> storage;

    /**
     * 图床API服务
     */
    private final IApiKeyService apiKeyService;

    /**
     * 以图片ID为键时的缓存前缀
     */
    public static final String PICTURE_ID = "picture-id:";

    /**
     * 图片信息的本地缓存Caffeine
     */
    public final Cache<String, String> PICTURE_LOCAL_CACHE = Caffeine.newBuilder()
            .maximumSize(10000L)
            // 5分钟后过期（5*60=300）
            .expireAfterWrite(300, TimeUnit.SECONDS)
            .build();

    @Override
    public String upload(MultipartFile multipartFile) {
        String fileName = multipartFile.getOriginalFilename();
        String filePath = MrsPathUtil.repairPath(PICTURE_PATH) + fileName;
        minioUtil.upload(multipartFile, filePath);
        return minioUtil.getFileUrl(filePath);
    }

    @Override
    public PictureVo saveOrUpdatePicture(Object inputSource, PictureUploadRequest uploadRequest, User loginUser) {
        log.info("原始上传信息： {}", uploadRequest);
        if (ObjectUtils.isEmpty(loginUser)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        //
        Long pictureId = uploadRequest.getId();
        if (pictureId != null && pictureId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "非法的图片ID");
        }
        // 空间ID为空 或 小于0，是错误的
        // 这里约定 空间ID==0 时是上传到公共图库
        Long spaceId = uploadRequest.getSpaceId();
        // 前端还是传的null，这里改了就不用改前端了
        if (spaceId == null) spaceId = 0L;
        log.info("当前空间ID： {}", spaceId);
        if (spaceId < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "非法的空间ID");
        }
        AtomicReference<Space> space = new AtomicReference<>(null);
        // 文件上传地址
        String suffixPath = "common/";
        // 上传到 私人空间 或 团队空间
        if (spaceId != 0L) {
            space.set(spaceService.getSpaceByIdCache(spaceId));
            ThrowUtils.throwIf(space.get() == null, ErrorCode.NOT_FOUND, "空间不存在");
            // 有空间ID，上传到对应的空间
            suffixPath = spaceId + "/";
            /* 用户只能上传到自己的空间，已废弃。
             * 因为团队成员可以上传图片到团队空间（如果他有上传权限）
             * Long userId = loginUser.getId();
             * if (!Objects.equals(space.getUserId(), userId)) {
             *     throw new BusinessException(ErrorCode.AUTHORIZATION_ERROR, "无空间访问权限");
             * }
             */
        }
        // 图片的保存位置（文件服务器上的相对路径前缀）
        String uploadPath = MrsPathUtil.repairPath(PICTURE_PATH) + suffixPath;
        // 图片ID不为空，是更新图片操作
        if (pictureId != null) {
            // 检查图片是否存在
            boolean exists = this.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .exists();
            ThrowUtils.throwIf(!exists, ErrorCode.NOT_FOUND, "图片不存在");
            /* 检查图片是否存在（已废弃）
             * Picture dbPic = this.getById(pictureId);
             * if (dbPic == null) {
             *     throw new BusinessException(ErrorCode.NOT_FOUND, "图片不存在");
             * }
             * // 仅允许本人 或者 管理员更新图片信息
             * // 已在Controller层的方法上使用 Sa-Token的注解 限定了只有具有 upload 权限的用户才能上传图片
             * Long dbPicUserId = dbPic.getUserId();
             * boolean isAdmin = userService.hasPermission(loginUser, MrsUserRole.ADMIN);
             * if (!dbPicUserId.equals(loginUser.getId()) && !isAdmin) {
             *     throw new BusinessException(ErrorCode.FORBIDDEN_ERROR);
             * }
             */
        }
        /* 上传图片（模板方法模式）
         * 修改后的代码不再适用于模板方法模式
         * PictureUploadTemplate uploadTemplate = pictureMultipartFileUpload;
         * if (inputSource instanceof String) {
         *     uploadTemplate = pictureUrlUpload;
         * }
         */
        long current = System.currentTimeMillis();
        Picture picture;
        // 为文件秒传准备的MD5，仅支持文件模式上传，URL模式暂不支持
        if (inputSource instanceof MultipartFile) {
            // 文件上传模式，支持秒传功能
            String hex = uploadRequest.getMd5();
            if (StrUtil.isBlank(hex)) {
                hex = MD5.create().digestHex(pictureMultipartFileUpload.getPictureStream(inputSource));
            }
            // 判断图片是否已经被其他用户上传过了（秒传是否可用）
            List<Picture> records = this.lambdaQuery()
                    .eq(Picture::getMd5, hex)
                    // 这里不要用.one()，它的底层是selectList().get(0)，因此并不能优化性能
                    .page(new Page<>(1, 1))
                    .getRecords();
            Picture repeatPicture = records.isEmpty() ? null : records.get(0);
            // 库中不存在，走正常上传逻辑
            if (repeatPicture == null) {
                // 保存图片到文件服务器
                UploadPictureResult uploadResult;
                if (useOss != null && useOss) {
                    uploadResult = aliyunPictureUploadMultipart.uploadPicture(inputSource, uploadPath);
                } else {
                    uploadResult = pictureMultipartFileUpload.uploadPictureObject(inputSource, uploadPath);
                }
                log.info("上传耗时: {}", (System.currentTimeMillis() - current));
                log.info("原始上传数据 {}", uploadResult);
                picture = getPicture(loginUser, uploadResult);
            } else {
                // TODO: 图片秒传功能有点BUG
                // 划分空间后，秒传时还需要将图片拷贝到自己的空间下
                // 而拷贝存在开销，秒传的意义是为了让用户上传图片时0等待
                // 拷贝操作或许应异步进行，先给用户一个可访问的图片链接
                // 但用户马上要编辑该怎么办？
                picture = getPicture(loginUser, repeatPicture);
                log.info("触发秒传");
                log.info("上传耗时: {}", (System.currentTimeMillis() - current));
            }
        } else {
            // 通过URL的方式上传图片，这个没办法做秒传，无法单凭一个URL就拿到目标文件的MD5
            // 此外还要兼容一些随机图的API，所以也没法把URL字符串直接MD5做标识
            UploadPictureResult uploadResult;
            if (useOss != null && useOss) {
                uploadResult = aliyunPictureUploadURL.uploadPicture(inputSource, uploadPath);
            } else {
                uploadResult = pictureUrlUpload.uploadPictureObject(inputSource, uploadPath);
            }
            picture = getPicture(loginUser, uploadResult);
            log.info("上传耗时: {}", (System.currentTimeMillis() - current));
            log.info("原始上传数据 {}", uploadResult);
        }
        // 公共空间 id=0L, 私人空间 id=<雪花ID>
        picture.setSpaceId(spaceId);
        // 上传到私有空间，需要限制大小和数量
        if (spaceId != 0L) {
            // picture.setSpaceId(spaceId);
            Space _space = space.get();
            // 这里只是校验，更新空间数据在最下面
            Long totalSize = _space.getTotalSize();
            Long maxSize = _space.getMaxSize();
            Integer totalCount = _space.getTotalCount();
            Integer maxCount = _space.getMaxCount();
            Long fileSize = picture.getPicSize();
            if ((totalSize + fileSize) > maxSize) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间容量不足");
            }
            if ((totalCount + 1) > maxCount) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "剩余可用数不足");
            }
        }
        // 自定义文件名（图片名不影响存储名）
        String picName = uploadRequest.getPicName();
        if (StrUtil.isNotBlank(picName)) {
            picture.setName(picName);
        }
        // 填充审核信息
        fillReviewParams(picture, loginUser);
        if (pictureId != null) {
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        log.info("最终图片信息 {}", picture);
        // 开启事务，保证图片和空间数据都保存成功
        transactionTemplate.execute(status -> {
            Long picId = picture.getId();
            List<String> cacheKeys = new ArrayList<>();
            // 删除缓存
            if (picId != null) {
                String cacheKey = PICTURE_CACHE_NAME + PICTURE_ID + picId;
                cacheKeys = Collections.singletonList(cacheKey);
                MrsCacheUtil.removeCache(PICTURE_LOCAL_CACHE, redisTemplate, cacheKeys);
            }
            // 更新图片
            boolean pictureSaved = this.saveOrUpdate(picture);
            // 通过线程池异步延迟删除缓存
            if (picId != null) {
                MrsCacheUtil.delayRemoveCache(PICTURE_LOCAL_CACHE, redisTemplate, cacheKeys, 3);
            }
            if (!pictureSaved) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存图片失败");
            }
            Space _space = space.get();
            // 上传的私有空间，更新空间信息
            if (_space != null) {
                Long totalSize = _space.getTotalSize() + picture.getPicSize();
                Integer totalCount = _space.getTotalCount() + 1;
                // 更新空间数据
                LambdaUpdateWrapper<Space> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(Space::getId, _space.getId());
                updateWrapper.set(Space::getTotalSize, totalSize);
                updateWrapper.set(Space::getTotalCount, totalCount);
                updateWrapper.set(Space::getEditTime, new Date());
                boolean spaceUpdated = spaceService.updateSpaceByCache(updateWrapper, _space.getId());
                if (!spaceUpdated) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新空间失败");
                }
            }
            return null;
        });
        return PictureVo.toVO(picture);
    }

    @Override
    public Integer uploadPictureBatch(PictureUploadBatchRequest uploadBatchRequest, User loggedUser) {
        String searchText = uploadBatchRequest.getSearchText();
        Integer count = uploadBatchRequest.getCount();
        if (StrUtil.isBlank(searchText)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请输入搜索关键词");
        }
        if (count == null || count <= 0 || count > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String pictureURL = String.format(pictureBathUrl, searchText);
        // 从预设站点获取图片链接
        // TODO: 修改图片服务时，这里应该改为从 Bing 提供的API服务，或者从Pixiv的API接口抓原图，直接爬质量太差了
        List<String> picturesURL = WebDocumentUtil.getPictureUrlInBing(pictureURL);
        PictureUploadRequest uploadRequest = new PictureUploadRequest();
        // 批量导入的名称前缀
        String namePrefix = uploadBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }
        int uploadCount = 0;
        for (String purl : picturesURL) {
            PictureVo vo = new PictureVo();
            try {
                uploadRequest.setPicName(namePrefix + "_" + (uploadCount + 1));
                vo = this.saveOrUpdatePicture(purl, uploadRequest, loggedUser);
                uploadCount++;
            } catch (Exception e) {
                System.err.println("图片上传失败 " + vo.getId());
                continue;
            }
            if (uploadCount >= count) {
                break;
            }
        }
        return uploadCount;
    }

    @Override
    public CreateTaskResponse createOutPaintingTask(PictureOutPaintingTaskRequest outPaintingTaskRequest, User loggedUser) {
        Long pictureId = outPaintingTaskRequest.getPictureId();
        if (pictureId == null || pictureId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ImagePaintingParameters parameters = outPaintingTaskRequest.getParameters();
        // 判断图片是否存在
        Picture dbPicture = this.lambdaQuery().eq(Picture::getId, pictureId).one();
        ThrowUtils.throwIf(dbPicture == null, ErrorCode.NOT_FOUND, "图片不存在");
        /* 判断是否有编辑权限
         * boolean isMaster = userService.hasPermission(loggedUser, MrsUserRole.MASTER);
         * if (!dbPicture.getUserId().equals(loggedUser.getId()) && !isMaster) {
         *     throw new BusinessException(ErrorCode.AUTHORIZATION_ERROR, "无操作权限");
         * }
         * */
        // 校验参数
        checkPictureOutPainting(dbPicture, parameters, loggedUser);
        String imageURL = dbPicture.getUrl();
        // 是否要使用原图进行AI扩图，原图扩图可能会导致效率变慢，但质量会更好
        Boolean useOriginal = outPaintingTaskRequest.getUseOriginal();
        if ((useOriginal != null) && useOriginal) {
            imageURL = dbPicture.getOriginalUrl();
        }
        ImageRequestParam params = new ImageRequestParam(imageURL, parameters);
        System.out.println(params);
        return imageOutPaintingApi.createTask(params);
    }

    /**
     * 校验AI扩图参数
     *
     * @param picture    图片对象
     * @param parameters 扩图参数
     * @param loggedUser 登录用户
     */
    private void checkPictureOutPainting(Picture picture, ImagePaintingParameters parameters, User loggedUser) {
        boolean isAdmin = userService.hasPermission(loggedUser, MrsUserRole.ADMIN);
        float xScale = parameters.getXScale();
        float yScale = parameters.getYScale();
        float xOffset = parameters.getLeftOffset() + parameters.getRightOffset();
        float yOffset = parameters.getTopOffset() + parameters.getBottomOffset();
        // 校验偏移量
        if (parameters.getLeftOffset() < 0 || parameters.getRightOffset() < 0
                || parameters.getTopOffset() < 0 || parameters.getBottomOffset() < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "偏移量不能为负数");
        }
        // 扩图宽度限制
        if ((xScale < 1 || xScale > 3) || (picture.getPicWidth() * 3 < xOffset)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "扩图宽度不能超过原来的3倍");
        }
        // 扩图高度限制
        if ((yScale < 1 || yScale > 3) || (picture.getPicHeight() * 3 < yOffset)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "扩图高度不能超过原来的3倍");
        }
        // 角度限制
        Integer rotateAngle = parameters.getAngle();
        if (rotateAngle < 0 || rotateAngle > 359) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "旋转角度不正确");
        }
        // 某些功能不开放给普通用户
        if (!isAdmin) {
            // 禁止普通用户使用 高质量功能 和 解除尺寸限制功能
            parameters.setBestQuality(false);
            parameters.setLimitImageSize(true);
        }
    }

    @Override
    public Long editPicture(PictureEditRequest editRequest, User loggedUser) {
        if (StrUtil.isBlank(editRequest.getName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片名称不能为空");
        }
        Picture picture = new Picture();
        BeanUtils.copyProperties(editRequest, picture);
        picture.setTags(JSONUtil.toJsonStr(editRequest.getTags()));
        picture.setEditTime(new Date());
        // 图片的用户ID为空，说明是新增
        if (picture.getUserId() == null) {
            picture.setUserId(loggedUser.getId());
        }
        // 校验图片参数
        this.validPicture(picture);
        // 校验用户是否具有操作权限
        // this.checkPictureAuth(picture, loggedUser);
        // 填充审核信息
        this.fillReviewParams(picture, loggedUser);
        // 检查图片是否存在
        Picture dbPicture = this.getPictureByIdCache(editRequest.getId());
        if (dbPicture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "图片不存在");
        }
        // 图片空间信息
        Long spaceId = editRequest.getSpaceId();
        if (spaceId == null) spaceId = 0L;  // 让前端不用处理空间ID了，之前公共空间以spaceId==null进行判断的
        ThrowUtils.throwIf(spaceId < 0, ErrorCode.PARAMS_ERROR, "空间ID错误");
        if (spaceId != 0L) {
            // 有空间ID，检查空间是否存在
            Space space = spaceService.getSpaceByIdCache(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND, "空间不存在");
        }
        picture.setSpaceId(spaceId);
        // 删除缓存
        String cacheKey = PICTURE_CACHE_NAME + PICTURE_ID + picture.getId();
        List<String> cacheKeys = Collections.singletonList(cacheKey);
        MrsCacheUtil.removeCache(PICTURE_LOCAL_CACHE, redisTemplate, cacheKeys);
        // 更新图片
        boolean updated = this.updateById(picture);
        // 通过线程池异步延迟删除缓存
        MrsCacheUtil.delayRemoveCache(PICTURE_LOCAL_CACHE, redisTemplate, cacheKeys, 3);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        // 啥都完成了，如果不是管理员上传图片，就将内容推给AI审核
        // 这里推到Redis，在AI审核服务那里进行取ID和图片进行处理
        Integer pending = ReviewStatus.PENDING.getValue();
        if (picture.getReviewStatus().equals(pending)) {
            log.info("非管理员上传，保存图片数据到Redis");
            storage.save(this.getById(picture));
        }
        return picture.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean editPictureBatch(PictureEditBatchRequest editRequest, User loggedUser) {
        // 校验修改参数
        validPictureEditBatch(editRequest);
        List<Picture> dbPictures = this.lambdaQuery()
                .eq(Picture::getSpaceId, editRequest.getSpaceId())
                .in(Picture::getId, editRequest.getPictureIdList())
                .list();
        if (dbPictures == null || dbPictures.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "图片不存在");
        }

        /* 修改图片需要本人或者管理员（已废弃）
         * 团队空间的其它具有权限成员可以修改图片
         * 但这里已经用 sa-token 在 controller层 进行过校验了
         * boolean isAdmin = userService.hasPermission(loggedUser, MrsUserRole.ADMIN);
         * Picture pic = dbPictures.get(0);
         * if (!pic.getUserId().equals(loggedUser.getId()) && !isAdmin) {
         *     throw new BusinessException(ErrorCode.AUTHORIZATION_ERROR, "没有权限修改图片");
         * }
         * */
        // 图片批量重命名
        String nameRole = editRequest.getNameRule();
        if (StrUtil.isNotBlank(nameRole)) {
            int count = 1;
            try {
                for (Picture picture : dbPictures) {
                    String picName = nameRole.replaceAll("\\{序号}", String.valueOf(count));
                    picture.setName(picName);
                    count++;
                }
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "名称解析失败");
            }
        }
        // 分片执行，避免长事务
        int batchSize = 100;
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < dbPictures.size(); i += batchSize) {
            List<Picture> pictures = dbPictures.subList(i, Math.min(i + batchSize, dbPictures.size()));
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {

                pictures.forEach(picture -> {
                    // 更新分类信息
                    String category = editRequest.getCategory();
                    if (StrUtil.isNotBlank(category)) {
                        picture.setCategory(category);
                    }
                    // 更新标签信息
                    List<String> tags = editRequest.getTags();
                    if (tags != null && !tags.isEmpty()) {
                        picture.setTags(JSONUtil.toJsonStr(tags));
                    }
                });
                // 删除缓存
                List<String> cacheKeys = pictures.stream()
                        .map(Picture::getId)
                        .map(pid -> PICTURE_CACHE_NAME + PICTURE_ID + pid)
                        .collect(Collectors.toList());
                MrsCacheUtil.removeCache(PICTURE_LOCAL_CACHE, redisTemplate, cacheKeys);
                // 更新图片
                boolean updated = this.updateBatchById(pictures);
                // 通过线程池异步延迟删除缓存
                MrsCacheUtil.delayRemoveCache(PICTURE_LOCAL_CACHE, redisTemplate, cacheKeys, 3);
                if (!updated) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR);
                }
            }, editBatchPoolExecutor);
            futures.add(future);
        }
        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return true;
    }

    /**
     * 校验图片批量修改的参数信息
     *
     * @param editRequest 批量编辑的参数封装
     */
    private void validPictureEditBatch(PictureEditBatchRequest editRequest) {
        List<Long> pictureIds = editRequest.getPictureIdList();
        Long spaceId = editRequest.getSpaceId();
        if (pictureIds == null || pictureIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片列表不能为空");
        }
        HashSet<Long> idSet = new HashSet<>(pictureIds);
        if (pictureIds.size() != idSet.size()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片重复修改");
        }
        if (spaceId == null || spaceId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片空间不能为空");
        }
    }

    @Override
    public Long updatePicture(PictureUpdateRequest updateRequest) {
        // TODO: 缓存暂时先加到这里，剩下的明天再加
        // 图片空间ID
        Long newSpaceId = updateRequest.getSpaceId();
        if (newSpaceId == null) newSpaceId = 0L;
        // copy基础属性并赋值
        Picture picture = new Picture();
        BeanUtils.copyProperties(updateRequest, picture);
        picture.setTags(JSONUtil.toJsonStr(updateRequest.getTags()));
        // 校验数据
        this.validPicture(picture);
        // 检查是否存在
        Picture dbPicture = this.getById(picture.getId());
        if (dbPicture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "图片不存在");
        }
        // 上传到私人空间
        if (!newSpaceId.equals(0L)) {
            boolean exists = spaceService.lambdaQuery()
                    .eq(Space::getId, newSpaceId).exists();
            ThrowUtils.throwIf(!exists, ErrorCode.NOT_FOUND, "空间不存在");
        }
        picture.setSpaceId(newSpaceId);
        // 更新图片信息
        boolean updated = this.updateById(picture);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return picture.getId();
    }

    @Override
    public Picture getPictureByIdCache(Long pid) {
        // 多级缓存
        String cacheKey = PICTURE_CACHE_NAME + PICTURE_ID + pid;
        log.info("缓存的key: {}", cacheKey);
        String cacheJson = PICTURE_LOCAL_CACHE.getIfPresent(cacheKey);
        log.info("cacheJson: {}", cacheJson);
        Picture picture = JSONUtil.toBean(cacheJson, Picture.class);
        log.info("picture: {}", picture);
        // 本地缓存中没有，尝试从redis缓存中取
        if (cacheJson == null) {
            Object cacheRedis = redisTemplate.opsForValue().get(cacheKey);
            log.info("cacheRedis: {}", cacheRedis);
            log.info("cacheRedis: {}", cacheRedis instanceof Picture);
            // redis缓存命中
            if (cacheRedis != null) {
                String jsonStr = JSONUtil.toJsonStr(cacheRedis);
                picture = JSONUtil.toBean(jsonStr, Picture.class);
            } else {
                // 两层缓存中都没有，去数据库中查
                picture = this.getById(pid);
                // 图片真的不存在
                if (picture == null) {
                    throw new BusinessException(ErrorCode.NOT_FOUND, "图片不存在");
                }
            }
        }
        PICTURE_LOCAL_CACHE.put(cacheKey, JSONUtil.toJsonStr(picture));
        // 设置随机过期时间（5分钟），防止雪崩
        int offset = RandomUtil.randomInt(0, 300);
        redisTemplate.opsForValue().set(cacheKey, picture, 300 + offset, TimeUnit.SECONDS);
        return picture;
    }

    /**
     * 封装图片信息
     *
     * @param loginUser    登录用户
     * @param uploadResult 上传图片信息
     * @return 图片的DAO对象
     */
    private static Picture getPicture(User loginUser, UploadPictureResult uploadResult) {
        Picture picture = new Picture();
        BeanUtils.copyProperties(uploadResult, picture);
        Picture res = getPicture(loginUser, picture);
        picture.setName(uploadResult.getPicName());
        return res;
    }

    /**
     * 封装图片信息
     *
     * @param loginUser 登录用户
     * @param dbPicture 数据库中的图片信息
     * @return 图片的DAO对象
     */
    private static Picture getPicture(User loginUser, Picture dbPicture) {
        Picture picture = new Picture();
        picture.setUserId(loginUser.getId());
        String originalPath = dbPicture.getOriginalPath();
        log.info("originalPath: {}", originalPath);
        String fileName = originalPath.substring(originalPath.lastIndexOf("/"));
        picture.setName(fileName);
        // 默认图
        picture.setUrl(dbPicture.getUrl());
        picture.setSavedPath(dbPicture.getSavedPath());
        // 拇指图
        picture.setThumbPath(dbPicture.getThumbPath());
        picture.setThumbnailUrl(dbPicture.getThumbnailUrl());
        // 原图
        picture.setOriginalPath(dbPicture.getOriginalPath());
        picture.setOriginalUrl(dbPicture.getOriginalUrl());
        // 颜色信息
        picture.setPicColor(dbPicture.getPicColor());
        picture.setMColorHue(dbPicture.getMColorHue());
        picture.setMColorSaturation(dbPicture.getMColorSaturation());
        picture.setMColorValue(dbPicture.getMColorValue());
        // 颜色桶(hsv)
        picture.setMHueBucket(dbPicture.getMHueBucket());
        picture.setMSaturationBucket(dbPicture.getMSaturationBucket());
        picture.setMValueBucket(dbPicture.getMValueBucket());
        // ...
        picture.setMd5(dbPicture.getMd5());
        picture.setPicSize(dbPicture.getPicSize());
        picture.setPicWidth(dbPicture.getPicWidth());
        picture.setPicHeight(dbPicture.getPicHeight());
        picture.setPicFormat(dbPicture.getPicFormat());
        picture.setPicScale(dbPicture.getPicScale());
        return picture;
    }

    @Override
    public InputStream downloadPicture(String fileName) {
        String minioPath = MrsPathUtil.repairPath(PICTURE_PATH) + fileName;
        System.out.println(minioPath);
        InputStream is = minioUtil.downloadInputStream(minioPath);
        if (is == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件不存在");
        }
        return is;
    }

    @Override
    public boolean deletePicture(DeleteRequest deleteRequest, HttpServletRequest httpServletRequest) {
        Long picId = deleteRequest.getId();
        if (picId == null || picId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // getPictureByIdCache 中有做判空操作，picture肯定能拿到
        Picture picture = this.getPictureByIdCache(picId);
        Long spaceId = picture.getSpaceId();
        if (spaceId == null) spaceId = 0L;
        AtomicReference<Space> atomicSpace = new AtomicReference<>(null);
        if (!spaceId.equals(0L)) {
            atomicSpace.set(spaceService.getSpaceByIdCache(spaceId));
        }
        /*
         * 校验操作权限（已废弃），统一使用sa-token 的图片权限校验系统
         * User loggedUser = userService.getLoginUser(httpServletRequest);
         * checkPictureAuth(picture, loggedUser);
         * */
        transactionTemplate.execute(status -> {
            // 删除缓存
            String cacheKey = PICTURE_CACHE_NAME + PICTURE_ID + picId;
            List<String> cacheKeys = Collections.singletonList(cacheKey);
            MrsCacheUtil.removeCache(PICTURE_LOCAL_CACHE, redisTemplate, cacheKeys);
            // 删除图片
            boolean picRemoved = this.removeById(picId);
            // 通过线程池异步延迟删除缓存
            MrsCacheUtil.delayRemoveCache(PICTURE_LOCAL_CACHE, redisTemplate, cacheKeys, 3);
            if (!picRemoved) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "数据库错误，图片删除失败");
            }
            // 空间不为空，回收空间占用
            Space space = atomicSpace.get();
            if (space != null) {
                LambdaUpdateWrapper<Space> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(Space::getId, space.getId());
                long totalSize = Long.max(space.getTotalSize() - picture.getPicSize(), 0L);
                updateWrapper.set(Space::getTotalSize, totalSize);
                updateWrapper.set(Space::getTotalCount, Math.max((space.getTotalCount() - 1), 0));
                boolean spaceUpdated = spaceService.update(updateWrapper);
                if (!spaceUpdated) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间回收失败");
                }
            }
            return null;
        });
        // TODO: 这里应该有个定时任务，定时删除图片，减少内存占用
        // minioUtil.delete(picture.getSavedPath());
        return true;
    }

    /**
     * 构造查询条件
     *
     * @param queryRequest 查询请求DTO对象
     * @return 查询wrapper
     */
    private LambdaQueryWrapper<Picture> getQueryWrapper(PictureQueryRequest queryRequest) {
        LambdaQueryWrapper<Picture> queryWrapper = new LambdaQueryWrapper<>();
        // 图片id，等值匹配
        if (queryRequest.getId() != null) {
            queryWrapper.eq(Picture::getId, queryRequest.getId());
        }
        // 用户id，等值匹配
        if (queryRequest.getUserId() != null) {
            queryWrapper.eq(Picture::getUserId, queryRequest.getUserId());
        }
        // 图片名称，模糊匹配
        if (StrUtil.isNotBlank(queryRequest.getName())) {
            queryWrapper.like(Picture::getName, queryRequest.getName());
        }
        // 空间id，等值匹配
        Long spaceId = queryRequest.getSpaceId();
        if (spaceId != null) {
            queryWrapper.eq(Picture::getSpaceId, spaceId);
        }
        // 空间id为空，说明要查公共空间，但公共空间约定ID=0
        if (queryRequest.isNullSpaceId()) {
            queryWrapper.eq(Picture::getSpaceId, 0L);
        }
        // 图片描述，模糊匹配
        if (StrUtil.isNotBlank(queryRequest.getIntroduction())) {
            queryWrapper.like(Picture::getIntroduction, queryRequest.getIntroduction());
        }
        // 从多字段中搜索，模糊匹配
        if (StrUtil.isNotBlank(queryRequest.getSearchText())) {
            // 图片名称和简介
            queryWrapper.and(qw -> qw
                    .like(Picture::getName, queryRequest.getSearchText())
                    .or()
                    .like(Picture::getIntroduction, queryRequest.getSearchText())
            );
        }
        // 图片分类，等值匹配
        if (StrUtil.isNotBlank(queryRequest.getCategory())) {
            queryWrapper.eq(Picture::getCategory, queryRequest.getCategory());
        }
        // 图片标签，等值匹配
        if (CollectionUtil.isNotEmpty(queryRequest.getTags())) {
            // ["tag1", "tag2", "tag3", ...]
            // StringBuilder sql = new StringBuilder();
            // List<String> params = new ArrayList<>();
            // for (String tag : queryRequest.getTags()) {
            //     if (StrUtil.isNotBlank(sql)) sql.append(" OR ");
            //     sql.append("JSON_CONTAINS(tags, {0}, '$')");
            //     params.add("\"" + tag + "\"");
            // }
            // queryWrapper.apply(sql.toString(), params.toArray());

            // 需要遍历，效率不高，上面那种效率高但没试过
            for (String tag : queryRequest.getTags()) {
                queryWrapper.like(Picture::getTags, "\"" + tag + "\"");
            }
        }
        // 审核状态，等值匹配
        if (queryRequest.getReviewStatus() != null) {
            boolean isReviewStatus = EnumUtil.hasEnumValue(queryRequest.getReviewStatus(), ReviewStatus.class);
            if (!isReviewStatus) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "未知的审核状态");
            }
            queryWrapper.eq(Picture::getReviewStatus, queryRequest.getReviewStatus());
        }
        // 审核消息，模糊匹配
        if (StrUtil.isNotBlank(queryRequest.getReviewMessage())) {
            queryWrapper.like(Picture::getReviewMessage, queryRequest.getReviewMessage());
        }
        // 审核员ID，等值匹配
        if (queryRequest.getReviewerId() != null && queryRequest.getReviewerId() > 0) {
            queryWrapper.eq(Picture::getReviewerId, queryRequest.getReviewerId());
        }
        // 图片大小（bit），等值匹配
        if (queryRequest.getPicSize() != null) {
            queryWrapper.eq(Picture::getPicSize, queryRequest.getPicSize());
        }
        // 宽高匹配类型
        MathComparator whTypeEnum = EnumUtil.fromValue(queryRequest.getWhType(), MathComparator.class);
        if (whTypeEnum == null) {
            whTypeEnum = MathComparator.EQ;
        }
        // 图片宽度，动态匹配
        if (queryRequest.getPicWidth() != null) {
            Integer picWidth = queryRequest.getPicWidth();
            switch (whTypeEnum) {
                case GT:
                    queryWrapper.gt(Picture::getPicWidth, picWidth);
                    break;
                case LT:
                    queryWrapper.lt(Picture::getPicWidth, picWidth);
                    break;
                case EQ:
                    queryWrapper.eq(Picture::getPicWidth, picWidth);
                    break;
            }
            // queryWrapper.eq(Picture::getPicWidth, queryRequest.getPicWidth());
        }
        // 图片高度，动态匹配
        if (queryRequest.getPicHeight() != null) {
            Integer picHeight = queryRequest.getPicHeight();
            switch (whTypeEnum) {
                case GT:
                    queryWrapper.gt(Picture::getPicHeight, picHeight);
                    break;
                case LT:
                    queryWrapper.lt(Picture::getPicHeight, picHeight);
                    break;
                case EQ:
                    queryWrapper.eq(Picture::getPicHeight, picHeight);
                    break;
            }
            // queryWrapper.eq(Picture::getPicHeight, queryRequest.getPicHeight());
        }
        // 图片长宽比，等值匹配
        if (queryRequest.getPicScale() != null) {
            queryWrapper.eq(Picture::getPicScale, queryRequest.getPicScale());
        }
        // 图片类型，等值匹配
        if (StrUtil.isNotBlank(queryRequest.getPicFormat())) {
            queryWrapper.eq(Picture::getPicFormat, queryRequest.getPicFormat());
        }
        // 起始编辑时间
        Date startEditTime = queryRequest.getStartEditTime();
        if (!ObjectUtils.isEmpty(startEditTime)) {
            queryWrapper.ge(Picture::getEditTime, startEditTime);
        }
        // 结束编辑时间
        Date endEditTime = queryRequest.getEndEditTime();
        if (!ObjectUtils.isEmpty(endEditTime)) {
            queryWrapper.lt(Picture::getEditTime, endEditTime);
        }
        // 近似色查找，范围匹配
        if (StrUtil.isNotBlank(queryRequest.getPicColor())) {
            String picColor = queryRequest.getPicColor();
            // 配置文件参数
            applyColorCondition(queryWrapper, picColor, similarity);
            // 前端传递相似度
            // applyColorCondition(queryWrapper, picColor, queryRequest.getPicSimilarity());
        }
        // 排序
        if (StrUtil.isNotBlank(queryRequest.getSortField())) {
            SortEnum sortType = EnumUtil.fromValue(queryRequest.getSortOrder(), SortEnum.class);
            boolean isAsc = sortType == SortEnum.ASC;
            queryWrapper.orderBy(true, isAsc, getSortField(queryRequest.getSortField()));
        }
        return queryWrapper;
    }

    /**
     * 获取排序字段
     *
     * @param fieldName 排序字段名
     * @return 排序字段的Lambda表达式
     */
    private SFunction<Picture, ?> getSortField(String fieldName) {
        switch (fieldName) {
            case "id":
                return Picture::getId;
            case "userId":
                return Picture::getUserId;
            case "name":
                return Picture::getName;
            case "introduction":
                return Picture::getIntroduction;
            case "category":
                return Picture::getCategory;
            case "tags":
                return Picture::getTags;
            case "picSize":
                return Picture::getPicSize;
            case "picWidth":
                return Picture::getPicWidth;
            case "picHeight":
                return Picture::getPicHeight;
            case "createTime":
                return Picture::getCreateTime;
            case "updateTime":
                return Picture::getUpdateTime;
            case "editTime":
                return Picture::getEditTime;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "未知的排序字段");
        }
    }

    @Override
    public PictureVo getPictureVo(Long pid, HttpServletRequest servletRequest) {
        if (pid == null || pid <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片ID不能为空");
        }
        // 根据图片ID获取图片对象
        Picture picture = this.getPictureByIdCache(pid);
        if (picture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "图片不存在");
        }
        Space space = null;
        Long spaceId = picture.getSpaceId();
        if (spaceId == null) spaceId = 0L;
        User loggedUser = userService.getLoginUserIfLogin(servletRequest);
        // 校验审核状态
        pictureReviewShow(picture, loggedUser);

        log.info("spaceId: {}", spaceId);
        // 是私人空间或团队空间，校验是否有相应权限
        if (!spaceId.equals(0L)) {
            // this.checkPictureAuth(picture, loggedUser);
            // 如果用户未登录，但又想访问私人图库，提示用户登录
            // 2期时这里能否访问应该看图库所属用户是否打算公开这个图库
            if (loggedUser == null) {
                throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
            }
            boolean canView = StpKit.SPACE.hasPermission(SpaceUserPermissionConstants.PICTURE_VIEW);
            ThrowUtils.throwIf(!canView, ErrorCode.AUTHORIZATION_ERROR);
            space = spaceService.getSpaceByIdCache(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND);
        }
        // 拷贝图片数据并转换为VO
        PictureVo vo = PictureVo.toVO(picture);
        Long userId = picture.getUserId();
        if (vo != null && userId != null) {
            UserVo userVo = userService.getUserVoByIdCache(userId);
            // 如果是未登录用户，则只有基础查看权限
            List<String> permissions = Collections.singletonList(SpaceUserPermissionConstants.PICTURE_VIEW);
            if (loggedUser != null) {
                permissions = spaceUserAuthManager.getPermissionList(space, loggedUser);
                // 公共空间
                if (spaceId.equals(0L)) {
                    boolean isAdmin = userService.hasPermission(loggedUser, MrsUserRole.ADMIN);
                    boolean isSelf = userId.equals(loggedUser.getId());
                    if (isAdmin || isSelf) {
                        permissions = spaceUserAuthManager.getPermissionsByRole(
                                MrsSpaceRole.ADMIN.getValue()
                        );
                    }
                }
            }
            vo.setPermissionList(permissions);
            vo.setUser(userVo);
        }
        log.info("pictureVo: {}", vo);
        return vo;
    }

    /**
     * 检查图片是否审核通过
     * <p>如果是审核中或审核未通过，则除本人或管理员外其他人无权访问</p>
     *
     * @param picture    图片对象
     * @param loggedUser 当前登录用户
     */
    public void pictureReviewShow(Picture picture, User loggedUser) {
        // 校验审核状态
        ReviewStatus reviewStatus = EnumUtil.fromValue(picture.getReviewStatus(), ReviewStatus.class);
        if (reviewStatus == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片审核状态异常");
        }
        // 如果在 审核中 或 未通过，则只有本人或管理员有权查看
        if (reviewStatus == ReviewStatus.PENDING || reviewStatus == ReviewStatus.REJECT) {
            boolean isSelf = loggedUser.getId().equals(picture.getUserId());
            boolean isAdmin = userService.hasPermission(loggedUser, MrsUserRole.ADMIN);
            if (!isSelf && !isAdmin) {
                throw new BusinessException(ErrorCode.AUTHORIZATION_ERROR, "您无权查看此图片");
            }
        }
    }

    @Override
    public Page<Picture> getPicturePage(PictureQueryRequest queryRequest) {
        int current = queryRequest.getCurrent();
        int size = queryRequest.getPageSize();
        LambdaQueryWrapper<Picture> queryWrapper = getQueryWrapper(queryRequest);
        return this.page(new Page<>(current, size), queryWrapper);
    }

    @Override
    public Page<PictureVo> getPictureVoPage(Page<Picture> picturePage, HttpServletRequest servletRequest) {
        Page<PictureVo> result = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        List<Picture> records = picturePage.getRecords();
        Set<Long> userIds = records.stream()
                .map(Picture::getUserId)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            result.setRecords(new ArrayList<>());
            return result;
        }
        // 根据ID查询用户列表，并按照 userId-User的形式映射成Map
        Map<Long, List<UserVo>> userVos = userService.listByIds(userIds).stream()
                .map(UserVo::toVO)
                .collect(Collectors.groupingBy(userVo -> (userVo != null) ? userVo.getId() : -1));
        // 组装图片Vo列表
        List<PictureVo> pictureVos = records.stream()
                .map(picture -> {
                    PictureVo vo = PictureVo.toVO(picture);
                    if (vo != null) {
                        // 不能在查列表的时候就拿到所有格式的图片信息
                        // 那样容易被爬，所以这里只返回缩略图的URL
                        vo.setUrl(null);
                        vo.setOriginalUrl(null);
                        if (userVos.containsKey(picture.getUserId())) {
                            vo.setUser(userVos.get(picture.getUserId()).get(0));
                        } else {
                            vo.setUser(null);
                        }
                        // 公共图库权限列表
                        List<String> permissions = spaceUserAuthManager.getPermissionsByRole(MrsSpaceRole.VIEWER.getValue());
                        vo.setPermissionList(permissions);
                    }
                    return vo;
                })
                .collect(Collectors.toList());
        result.setRecords(pictureVos);
        return result;
    }

    @Override
    public List<PictureVo> getPictureByColor(Long spaceId, PictureQueryRequest queryRequest, HttpServletRequest servletRequest) {
        queryRequest.setSpaceId(spaceId);
        LambdaQueryWrapper<Picture> queryWrapper = getQueryWrapper(queryRequest);
        String sql = queryWrapper.getTargetSql();
        System.out.println("=======================");
        System.out.println(sql);
        System.out.println("=======================");
        // 只要前12条
        Page<Picture> page = this.page(new Page<>(1, 12), queryWrapper);
        Page<PictureVo> pictureVoPage = this.getPictureVoPage(page, servletRequest);
        return pictureVoPage.getRecords();
    }

    @Override
    public void validPicture(Picture picture) {
        if (ObjUtil.isNull(picture)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (ObjUtil.isNull(picture.getId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "id不能为空");
        }
        if (StrUtil.isNotBlank(picture.getUrl()) && picture.getUrl().length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "URL过长");
        }
        if (StrUtil.isNotBlank(picture.getIntroduction()) && picture.getIntroduction().length() > 800) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    @Override
    public void doPictureReview(PictureReviewRequest reviewRequest, HttpServletRequest servletRequest) {
        if (reviewRequest.getId() == null || reviewRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ReviewStatus reviewStatus = EnumUtil.fromValue(reviewRequest.getReviewStatus(), ReviewStatus.class);
        if (reviewStatus == null || reviewStatus == ReviewStatus.PENDING) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Picture picture = this.getPictureByIdCache(reviewRequest.getId());
        if (picture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        ReviewStatus currentStatus = EnumUtil.fromValue(picture.getReviewStatus(), ReviewStatus.class);
        if (currentStatus == reviewStatus) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复发起审核");
        }
        User loginUser = userService.getLoginUser(servletRequest);
        picture.setReviewerId(loginUser.getId());
        picture.setReviewStatus(reviewStatus.getValue());
        picture.setReviewMessage(reviewRequest.getReviewMessage());
        picture.setReviewTime(new Date());
        Long pictureId = picture.getId();
        // 删除缓存
        String cacheKey = PICTURE_CACHE_NAME + PICTURE_ID + pictureId;
        List<String> cacheKeys = Collections.singletonList(cacheKey);
        MrsCacheUtil.removeCache(PICTURE_LOCAL_CACHE, redisTemplate, cacheKeys);
        // 更新图片
        boolean updated = this.updateById(picture);
        // 通过线程池异步延迟删除缓存
        MrsCacheUtil.delayRemoveCache(PICTURE_LOCAL_CACHE, redisTemplate, cacheKeys, 3);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "数据库错误，图片更新失败");
        }
        // 发消息
        pushMessage(picture, loginUser, reviewRequest);
    }

    private void pushMessage(Picture picture, User reviewUser, PictureReviewRequest reviewRequest) {
        // 消息对象
        Notice notice = new Notice();
        notice.setNoticeType(MrsNoticeType.SYSTEM.getValue());
        notice.setUserId(picture.getUserId());  // 接收方ID
        notice.setSenderId(reviewUser.getId()); // 审核员ID
        notice.setIsRead(MrsNoticeRead.UNREAD.getValue());
        // 构建消息内容
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日HH时mm分");
        Date getEditTime = picture.getEditTime();
        Date createTime = picture.getCreateTime();
        Date uploadTime = getEditTime == null ? createTime : getEditTime;
        String uploadTimeStr = sdf.format(uploadTime);
        Long spaceId = picture.getSpaceId();
        String spaceName = (spaceId == 0L) ? "公共图库" : "空间 " + spaceId + " ";
        String msg = String.format("尊敬的用户您好，您在%s向%s上传的图片%s，经%s，点击查看详情。",
                uploadTimeStr,
                spaceName,
                picture.getName(),
                reviewRequest.getReviewMessage()
        );
        notice.setContent(msg);
        Map<String, Object> params = new HashMap<>();
        params.put("pictureId", String.valueOf(picture.getId()));
        notice.setAdditionalParams(JSONUtil.toJsonStr(params));
        notice.setCreateTime(new Date());
        noticeService.save(notice);
        noticeService.pushMessage(picture.getUserId(), notice);
    }

    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        MrsUserRole currentRole = EnumUtil.fromValue(loginUser.getUserRole(), MrsUserRole.class);
        // 未知角色、封禁中、游客等不能上传图片
        if (currentRole == null || currentRole.notThanRole(MrsUserRole.USER)) {
            throw new BusinessException(ErrorCode.AUTHORIZATION_ERROR, "权限不足");
        }
        // 权限比管理员低
        if (currentRole.notThanRole(MrsUserRole.ADMIN)) {
            picture.setReviewStatus(ReviewStatus.PENDING.getValue());
            picture.setReviewMessage(ReviewStatus.PENDING.getDesc());
        } else {
            // 权限大于等于管理员权限
            picture.setReviewStatus(ReviewStatus.PASS.getValue());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewerId(loginUser.getId());
            picture.setReviewTime(new Date());
        }
    }

    /**
     * 检查用户是否有权限操作该图片
     * <p>公共空间-上传用户和管理员可操作</p>
     * <p>私人空间-仅空间管理员可操作</p>
     *
     * @param picture   图片对象
     * @param loginUser 登录用户
     * @deprecated 已废弃
     */
    @Deprecated
    @Override
    public void checkPictureAuth(Picture picture, User loginUser) {
        Long spaceId = picture.getSpaceId();
        Long puid = picture.getUserId();
        Long uuid = loginUser.getId();
        // TODO: 这里总感觉怪怪的，但它可以满足基础功能
        if (spaceId == null) {
            // 公共空间，仅本人和系统管理员可以操作
            boolean isAdmin = userService.hasPermission(loginUser, MrsUserRole.ADMIN);
            System.out.println(uuid);
            System.out.println(puid);
            System.out.println(Objects.equals(puid, uuid));

            if (!Objects.equals(puid, uuid) && !isAdmin) {
                throw new BusinessException(ErrorCode.AUTHORIZATION_ERROR);
            }
        } else {
            // 私有空间，仅空间管理员可以操作
            // 空间管理员就是图片的拥有者
            // 一个人拥有有多个空间时也适用，登录用户操作不是自己创建的图片就算越权访问
            if (!Objects.equals(puid, uuid)) {
                throw new BusinessException(ErrorCode.AUTHORIZATION_ERROR);
            }
        }
    }

    @Override
    public void removeCacheByKeys(List<Long> ids) {
        List<String> keys = ids.stream()
                .map(id -> PICTURE_CACHE_NAME + PICTURE_ID + id)
                .collect(Collectors.toList());
        // 立即删除缓存
        MrsCacheUtil.removeCache(PICTURE_LOCAL_CACHE, redisTemplate, keys);
    }

    @Override
    public void delayRemoveCacheByKeys(List<Long> ids) {
        List<String> keys = ids.stream()
                .map(id -> PICTURE_CACHE_NAME + PICTURE_ID + id)
                .collect(Collectors.toList());
        // 通过线程池异步延迟删除缓存
        MrsCacheUtil.delayRemoveCache(PICTURE_LOCAL_CACHE, redisTemplate, keys, 3);

    }

    @Override
    public List<PictureVo> getRandomPictures(RandomPictureRequest request) {
        // 1. 获取API密钥关联的空间ID列表
        ApiKey apiKey = apiKeyService.getOne(new LambdaQueryWrapper<ApiKey>()
                .eq(ApiKey::getAccessKey, request.getAccessKey())
                .eq(ApiKey::getSecretKey, request.getSecretKey())
                .eq(ApiKey::getStatus, 1)
                .eq(ApiKey::getIsDeleted, 0));

        if (apiKey == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "API密钥不存在或已禁用");
        }

        List<Long> spaceIds = apiKeyService.getApiKeySpaces(apiKey.getId());

        // 2. 构建查询条件
        LambdaQueryWrapper<Picture> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Picture::getReviewStatus, ReviewStatus.PASS.getValue());

        // 3. 处理空间ID条件
        if (spaceIds.isEmpty()) {
            // 如果没有关联空间，则只能访问公共空间
            queryWrapper.eq(Picture::getSpaceId, 0L);
        } else {
            // 可以访问关联的空间和公共空间
            queryWrapper.and(wrapper -> wrapper
                    .eq(Picture::getSpaceId, 0L)
                    .or()
                    .in(Picture::getSpaceId, spaceIds));
        }

        // 4. 处理图片尺寸条件
        if (request.getMinWidth() != null) {
            queryWrapper.ge(Picture::getPicWidth, request.getMinWidth());
        }
        if (request.getMaxWidth() != null) {
            queryWrapper.le(Picture::getPicWidth, request.getMaxWidth());
        }
        if (request.getMinHeight() != null) {
            queryWrapper.ge(Picture::getPicHeight, request.getMinHeight());
        }
        if (request.getMaxHeight() != null) {
            queryWrapper.le(Picture::getPicHeight, request.getMaxHeight());
        }

        // 5. 获取符合条件的图片总数
        long total = this.count(queryWrapper);
        if (total == 0) {
            return Collections.emptyList();
        }

        // 6. 随机获取指定数量的图片
        List<Picture> pictures = new ArrayList<>();
        int count = Math.min(request.getCount(), ApiConstants.MAX_PICTURE_COUNT);

        // 使用随机数生成器
        Random random = new Random();
        Set<Long> selectedIds = new HashSet<>();

        while (pictures.size() < count && selectedIds.size() < total) {
            // 生成随机偏移量
            long offset = random.nextInt((int) total);

            // 使用分页查询获取随机图片
            List<Picture> pagePictures = this.page(
                    new Page<>(offset / 10 + 1, 10),
                    queryWrapper
            ).getRecords();

            // 从未选择的图片中选择
            for (Picture picture : pagePictures) {
                if (!selectedIds.contains(picture.getId())) {
                    pictures.add(picture);
                    selectedIds.add(picture.getId());
                    if (pictures.size() >= count) {
                        break;
                    }
                }
            }
        }

        // 7. 转换为VO对象
        return pictures.stream()
                .map(PictureVo::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PictureVo> searchPictures(SearchPictureRequest request) {
        // 1. 获取API密钥关联的空间ID列表
        ApiKey apiKey = apiKeyService.getOne(new LambdaQueryWrapper<ApiKey>()
                .eq(ApiKey::getAccessKey, request.getAccessKey())
                .eq(ApiKey::getSecretKey, request.getSecretKey())
                .eq(ApiKey::getStatus, 1)
                .eq(ApiKey::getIsDeleted, 0));

        if (apiKey == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "API密钥不存在或已禁用");
        }

        List<Long> spaceIds = apiKeyService.getApiKeySpaces(apiKey.getId());

        // 2. 构建查询条件
        LambdaQueryWrapper<Picture> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Picture::getReviewStatus, ReviewStatus.PASS.getValue());

        // 3. 处理空间ID条件
        if (spaceIds.isEmpty()) {
            // 如果没有关联空间，则只能访问公共空间
            queryWrapper.eq(Picture::getSpaceId, 0L);
        } else {
            // 可以访问关联的空间和公共空间
            queryWrapper.and(wrapper -> wrapper
                    .eq(Picture::getSpaceId, 0L)
                    .or()
                    .in(Picture::getSpaceId, spaceIds));
        }

        // 模糊搜名称
        if (StrUtil.isNotBlank(request.getKeyword())) {
            queryWrapper.like(Picture::getName, request.getKeyword());
        }

        // 标签，JSON字符串数组["tag1", "tag2", ...]
        if (StrUtil.isNotBlank(request.getTag()) && JSONUtil.isTypeJSONArray(request.getTag())) {
            List<String> tags = JSONUtil.toList(request.getTag(), String.class);
            for (String tag : tags) {
                queryWrapper.like(Picture::getTags, "\"" + tag + "\"");
            }
        }

        // 分类，单分类
        if (StrUtil.isNotBlank(request.getCategory())) {
            queryWrapper.eq(Picture::getCategory, request.getCategory());
        }

        // 5. 处理图片尺寸条件
        if (request.getMinWidth() != null) {
            queryWrapper.ge(Picture::getPicWidth, request.getMinWidth());
        }
        if (request.getMaxWidth() != null) {
            queryWrapper.le(Picture::getPicWidth, request.getMaxWidth());
        }
        if (request.getMinHeight() != null) {
            queryWrapper.ge(Picture::getPicHeight, request.getMinHeight());
        }
        if (request.getMaxHeight() != null) {
            queryWrapper.le(Picture::getPicHeight, request.getMaxHeight());
        }

        // 获取符合条件的图片
        List<Picture> pictures = this.list(queryWrapper);

        // 如果结果超过限制，随机选择指定数量
        int count = Math.min(request.getCount(), ApiConstants.MAX_PICTURE_COUNT);
        if (pictures.size() > count) {
            Collections.shuffle(pictures);
            pictures = pictures.subList(0, count);
        }
        // 转VO并返回
        return pictures.stream()
                .map(PictureVo::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 动态添加颜色相似性条件
     *
     * @param wrapper     Lambda查询包装器
     * @param searchColor RGB颜色字符串（如"#FF0000"）
     * @param similarity  相似度阈值（0~1）
     */
    private void applyColorCondition(
            LambdaQueryWrapper<Picture> wrapper,
            String searchColor,
            float similarity
    ) {
        if (similarity <= 0 || similarity > 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "相似度需在(0,1]之间但实际是 " + similarity);
        }
        log.info("搜索颜色: {}", searchColor);
        log.info("相似度: {}", similarity);
        // Step 1: 转换RGB到HSV和分桶
        MrsColorHSV colorHSV = ColorUtils.toHSV(searchColor);
        float hue = colorHSV.getHue();
        float sat = colorHSV.getSaturation();
        float vak = colorHSV.getValue();
        // Step 2: 根据相似度计算允许偏差
        float hueRange = (1 - similarity) * 360;  // 最大允许色相差
        float satValRange = (1 - similarity) * 100; // 最大允许饱和度/明度差
        // Step 3: 确定需要查询的桶范围
        int hueTolerance = (int) Math.ceil(hueRange / 10); // 每个桶10°
        List<Integer> hueBuckets = calculateBuckets(hue, hueTolerance);
        int satTolerance = (int) Math.ceil(satValRange / 10); // 每个桶10%
        List<Integer> satBuckets = calculateLinearBuckets(colorHSV.getSaturationBucket(), satTolerance);
        int valTolerance = (int) Math.ceil(satValRange / 10);
        List<Integer> valBuckets = calculateLinearBuckets(colorHSV.getValueBucket(), valTolerance);
        log.info("色调桶范围: {}", hueBuckets.stream().sorted().collect(Collectors.toList()));
        log.info("饱和度范围: {}", satBuckets.stream().sorted().collect(Collectors.toList()));
        log.info("明度桶范围: {}", valBuckets.stream().sorted().collect(Collectors.toList()));
        // Step 4: 构建查询条件
        wrapper
                // 色调分桶范围过滤
                .in(Picture::getMHueBucket, hueBuckets)
                .in(Picture::getMSaturationBucket, satBuckets)
                .in(Picture::getMValueBucket, valBuckets)
                // 色调差异范围
                .apply("ABS(m_color_hue - {0}) <= {1}", hue, hueRange)
                // 饱和度差异
                .apply("ABS(m_color_saturation - {0}) <= {1}", sat, satValRange)
                // 明度差异
                .apply("ABS(m_color_value - {0}) <= {1}", vak, satValRange);
        log.info("SQL: {}", wrapper.getSqlSegment());

    }

    /**
     * 计算候选桶（处理环形色相）
     *
     * @param targetHue 目标色相
     * @param tolerance 色相容差
     * @return 桶号
     */
    private List<Integer> calculateBuckets(float targetHue, int tolerance) {
        List<Integer> buckets = new ArrayList<>();
        int baseBucket = (int) Math.floor(targetHue / 10);
        for (int i = -tolerance; i <= tolerance; i++) {
            int bucket = (baseBucket + i + 36) % 36; // 处理负值和溢出
            buckets.add(bucket);
        }
        return buckets;
    }

    /**
     * 计算线性分桶（0-9号桶）
     */
    private static List<Integer> calculateLinearBuckets(int base, int tolerance) {
        List<Integer> buckets = new ArrayList<>();
        for (int i = -tolerance; i <= tolerance; i++) {
            int bucket = base + i;
            if (bucket >= 0 && bucket <= 9) {
                buckets.add(bucket);
            }
        }
        return buckets;
    }
}