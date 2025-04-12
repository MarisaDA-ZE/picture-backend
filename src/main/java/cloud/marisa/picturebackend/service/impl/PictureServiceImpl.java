package cloud.marisa.picturebackend.service.impl;

import cloud.marisa.picturebackend.api.image.imageexpand.ImageOutPaintingApi;
import cloud.marisa.picturebackend.api.image.imageexpand.entity.request.ImagePaintingParameters;
import cloud.marisa.picturebackend.api.image.imageexpand.entity.request.ImageRequestParam;
import cloud.marisa.picturebackend.api.image.imageexpand.entity.response.create.CreateTaskResponse;
import cloud.marisa.picturebackend.entity.dao.Picture;
import cloud.marisa.picturebackend.entity.dao.Space;
import cloud.marisa.picturebackend.entity.dao.User;
import cloud.marisa.picturebackend.entity.dto.common.DeleteRequest;
import cloud.marisa.picturebackend.entity.dto.file.UploadPictureResult;
import cloud.marisa.picturebackend.entity.dto.picture.*;
import cloud.marisa.picturebackend.entity.vo.PictureVo;
import cloud.marisa.picturebackend.entity.vo.UserVo;
import cloud.marisa.picturebackend.enums.MathComparator;
import cloud.marisa.picturebackend.enums.ReviewStatus;
import cloud.marisa.picturebackend.enums.SortEnum;
import cloud.marisa.picturebackend.enums.UserRole;
import cloud.marisa.picturebackend.exception.BusinessException;
import cloud.marisa.picturebackend.exception.ErrorCode;
import cloud.marisa.picturebackend.exception.ThrowUtils;
import cloud.marisa.picturebackend.mapper.PictureMapper;
import cloud.marisa.picturebackend.service.IPictureService;
import cloud.marisa.picturebackend.service.ISpaceService;
import cloud.marisa.picturebackend.service.IUserService;
import cloud.marisa.picturebackend.upload.picture.PictureMultipartFileUpload;
import cloud.marisa.picturebackend.upload.picture.PictureUploadTemplate;
import cloud.marisa.picturebackend.upload.picture.PictureUrlUpload;
import cloud.marisa.picturebackend.util.*;
import cloud.marisa.picturebackend.util.colors.ColorUtils;
import cloud.marisa.picturebackend.util.colors.MrsColorHSV;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;


/**
 * @author Marisa
 * @description 针对表【picture(图片表)】的数据库操作Service实现
 * @createDate 2025-03-29 22:12:31
 */
@Log4j2
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements IPictureService {

    @Value("${minio.folders.pictures}")
    private String PICTURE_PATH;

    @Autowired
    private MinioUtil minioUtil;

    @Autowired
    private IUserService userService;

    @Autowired
    private ISpaceService spaceService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private PictureUrlUpload pictureUrlUpload;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private PictureMultipartFileUpload pictureMultipartFileUpload;

    @Autowired
    private ImageOutPaintingApi imageOutPaintingApi;

    @Value("${mrs.picture-bath-url}")
    private String pictureBathUrl;

    @Override
    public String upload(MultipartFile multipartFile) {
        String fileName = multipartFile.getOriginalFilename();
        String filePath = MrsPathUtil.repairPath(PICTURE_PATH) + fileName;
        minioUtil.upload(multipartFile, filePath);
        return minioUtil.getFileUrl(filePath);
    }

    @Override
    public PictureVo uploadPicture(Object inputSource, PictureUploadRequest uploadRequest, User loginUser) {
        if (ObjectUtils.isEmpty(loginUser)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        Long pictureId = null;
        if (uploadRequest.getId() != null) {
            pictureId = uploadRequest.getId();
        }
        boolean isAdmin = userService.hasPermission(loginUser, UserRole.ADMIN);
        Long spaceId = uploadRequest.getSpaceId();
        // ID小于等于0，是非法空间ID
        if (spaceId != null && spaceId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "非法的空间ID");
        }
        Space space;
        // 上传到指定空间，spaceId为空表示公共空间
        if (spaceId != null) {
            space = spaceService.getById(spaceId);
            if (space == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "空间不存在");
            }
            // 用户只能上传到自己的空间
            Long userId = loginUser.getId();
            if (!Objects.equals(space.getUserId(), userId)) {
                throw new BusinessException(ErrorCode.AUTHORIZATION_ERROR, "无空间访问权限");
            }
        } else {
            space = null;
        }

        if (pictureId != null) {
            /* 之前的校验方式，判断对象是否存在
               boolean exists = this.lambdaQuery()
                       .eq(Picture::getId, pictureId)
                       .exists();
               if (!exists) {
                   throw new BusinessException(ErrorCode.NOT_FOUND, "图片不存在");
              }*/
            Picture picture = this.getById(pictureId);
            if (picture == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "图片不存在");
            }
            if (!picture.getUserId().equals(loginUser.getId()) && !isAdmin) {
                throw new BusinessException(ErrorCode.FORBIDDEN_ERROR);
            }
        }
        UserRole currentRole = EnumUtil.fromValue(loginUser.getUserRole(), UserRole.class);
        // 封禁账号、游客等不能上传图片
        if (currentRole == null || currentRole.notThanRole(UserRole.USER)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR);
        }
        // 上传图片（模板方法模式）
        PictureUploadTemplate uploadTemplate = pictureMultipartFileUpload;
        if (inputSource instanceof String) {
            uploadTemplate = pictureUrlUpload;
        }
        long current = System.currentTimeMillis();
        String hex = MD5.create().digestHex(uploadTemplate.getPictureStream(inputSource));
        // 只找一个就够了，这样会快一点
        Page<Picture> dbPicturePage = this.lambdaQuery()
                .eq(Picture::getMd5, hex)
                .page(new Page<>(1, 1));
        List<Picture> records = dbPicturePage.getRecords();
        Picture picture;
        // 库中不存在该文件
        if (records == null || records.isEmpty()) {
            String uploadPath = MrsPathUtil.repairPath(PICTURE_PATH);
            // 有空间ID，上传到对应的空间
            if (spaceId != null) {
                uploadPath = uploadPath + spaceId + "/";
            } else {
                // 根据用户ID划分
                uploadPath = uploadPath + loginUser.getId() + "/";
            }
            // 保存图片到文件服务器
            UploadPictureResult uploadPictureResult = uploadTemplate.uploadPictureObject(inputSource, uploadPath);
            System.out.println("上传耗时: " + (System.currentTimeMillis() - current));
            log.info("原始上传数据 {}", uploadPictureResult);
            picture = getPicture(loginUser, uploadPictureResult);
        } else {
            // TODO: 图片秒传功能有点BUG
            // 划分空间后，秒传时还需要将图片拷贝到自己的空间下
            // 这存在开销，但秒传的意义是为了让用户上传大图片时0等待
            // 那拷贝操作或许应异步进行，先给用户一个可访问的图片链接
            // 但用户要编辑该怎么办？
            // Woc，我不知道，鱼哥没讲QwQ
            picture = getPicture(loginUser, records.get(0));
            System.out.println("秒传啦");
            System.out.println("上传耗时: " + (System.currentTimeMillis() - current));
        }
        // 上传到私有空间，需要限制大小和数量
        if (spaceId != null) {
            picture.setSpaceId(spaceId);
            // 这里只是校验，更新空间大小在下面
            Long totalSize = space.getTotalSize();
            Integer totalCount = space.getTotalCount();
            Long fileSize = picture.getPicSize();
            if ((totalSize + fileSize) > space.getMaxSize()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间容量不足");
            }
            if ((totalCount + 1) > space.getMaxCount()) {
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
        log.info("图片信息 {}", picture);
        // 开启事务
        transactionTemplate.execute(status -> {
            boolean pictureSaved = this.saveOrUpdate(picture);
            if (!pictureSaved) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存图片失败");
            }
            // 上传的私有空间，更新空间信息
            if (space != null) {
                Long totalSize = space.getTotalSize() + picture.getPicSize();
                // 更新空间数据
                LambdaUpdateWrapper<Space> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(Space::getId, space.getId());
                updateWrapper.set(Space::getTotalSize, totalSize);
                updateWrapper.set(Space::getTotalCount, space.getTotalCount() + 1);
                updateWrapper.set(Space::getEditTime, new Date());
                boolean spaceUpdated = spaceService.update(updateWrapper);
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
        List<String> picturesURL = WebDocumentUtil.getPictureUrlInBing(pictureURL);
        PictureUploadRequest uploadRequest = new PictureUploadRequest();
        // 批量导入的名称前缀
        String namePrefix = uploadBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(uploadBatchRequest.getNamePrefix())) {
            namePrefix = searchText;
        }
        int uploadCount = 0;
        for (String purl : picturesURL) {
            PictureVo vo = new PictureVo();
            try {
                uploadRequest.setPicName(namePrefix + "_" + (uploadCount + 1));
                vo = this.uploadPicture(purl, uploadRequest, loggedUser);
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
        Picture dbPicture = this.lambdaQuery().eq(Picture::getId, pictureId).one();
        // 判断图片是否存在
        if (dbPicture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "图片不存在");
        }
        // 判断是否有编辑权限
        boolean isMaster = userService.hasPermission(loggedUser, UserRole.MASTER);
        if (!dbPicture.getUserId().equals(loggedUser.getId()) && !isMaster) {
            throw new BusinessException(ErrorCode.AUTHORIZATION_ERROR, "无操作权限");
        }
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
        boolean isAdmin = userService.hasPermission(loggedUser, UserRole.ADMIN);
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
        // 图片所属ID为空，说明是新增
        if (picture.getUserId() == null) {
            picture.setUserId(loggedUser.getId());
        }
        // 校验图片参数
        this.validPicture(picture);
        // 校验用户是否具有操作权限
        this.checkPictureAuth(picture, loggedUser);
        // 填充审核信息
        this.fillReviewParams(picture, loggedUser);
        // 检查图片是否存在
        Picture dbPicture = this.getById(editRequest.getId());
        if (dbPicture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "图片不存在");
        }
        // 没有空间ID，则自动使用之前的
        picture.setSpaceId(dbPicture.getSpaceId());
        // 图片空间信息
        Long spaceId = editRequest.getSpaceId();
        if (spaceId != null) {
            // 有空间ID，检查空间是否存在
            boolean exists = spaceService.lambdaQuery()
                    .eq(Space::getId, spaceId).exists();
            ThrowUtils.throwIf(!exists, ErrorCode.NOT_FOUND, "空间不存在");
        }
        boolean updated = this.updateById(picture);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
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
        boolean isAdmin = userService.hasPermission(loggedUser, UserRole.ADMIN);
        // 同一个空间不会有别人上传图片，所以空间ID一致时，用户ID也一定一致
        Picture pic = dbPictures.get(0);
        if (!pic.getUserId().equals(loggedUser.getId()) && !isAdmin) {
            throw new BusinessException(ErrorCode.AUTHORIZATION_ERROR, "没有权限修改图片");
        }
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
                boolean updated = this.updateBatchById(pictures);
                if (!updated) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR);
                }
            }, threadPoolExecutor);
            futures.add(future);
        }
        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return true;
    }

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
        // 图片空间ID
        Long newSpaceId = updateRequest.getSpaceId();
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
        // 没传空间ID就复用原图的空间ID
        picture.setSpaceId(dbPicture.getSpaceId());
        if (newSpaceId != null) {
            boolean exists = spaceService.lambdaQuery()
                    .eq(Space::getId, newSpaceId).exists();
            ThrowUtils.throwIf(!exists, ErrorCode.NOT_FOUND, "空间不存在");
            picture.setSpaceId(newSpaceId);
        }
        // 更新图片信息
        boolean updated = this.updateById(picture);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return picture.getId();
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
        picture.setUserId(loginUser.getId());
        picture.setName(uploadResult.getPicName());
        // 默认图
        picture.setUrl(uploadResult.getUrl());
        picture.setSavedPath(uploadResult.getSavedPath());
        // 拇指图
        picture.setThumbPath(uploadResult.getThumbPath());
        picture.setThumbnailUrl(uploadResult.getUrlThumb());
        // 原图
        picture.setOriginalPath(uploadResult.getOriginalPath());
        picture.setOriginalUrl(uploadResult.getUrlOriginal());
        // 颜色信息
        picture.setPicColor(uploadResult.getPicColor());
        // 主要颜色(hsv)
        picture.setMColorHue(uploadResult.getMColorHue());
        picture.setMColorSaturation(uploadResult.getMColorSaturation());
        picture.setMColorValue(uploadResult.getMColorValue());
        // 颜色桶(hsv)
        picture.setMHueBucket(uploadResult.getMHueBucket());
        picture.setMSaturationBucket(uploadResult.getMSaturationBucket());
        picture.setMValueBucket(uploadResult.getMValueBucket());
        // ...
        picture.setMd5(uploadResult.getMd5());
        picture.setPicSize(uploadResult.getPicSize());
        picture.setPicWidth(uploadResult.getPicWidth());
        picture.setPicHeight(uploadResult.getPicHeight());
        picture.setPicScale(uploadResult.getPicScale());
        picture.setPicFormat(uploadResult.getPicFormat());
        return picture;
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
        picture.setMHueBucket(dbPicture.getMHueBucket());
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
        User loggedUser = userService.getLoginUser(httpServletRequest);
        Picture picture = this.getById(picId);
        Long spaceId = picture.getSpaceId();
        Space space;
        if (spaceId != null) {
            space = spaceService.getById(spaceId);
        } else {
            space = null;
        }
        // 校验操作权限
        checkPictureAuth(picture, loggedUser);

        transactionTemplate.execute(status -> {
            // 删除图片
            boolean picRemoved = this.removeById(picId);
            if (!picRemoved) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR);
            }
            if (space != null) {
                LambdaUpdateWrapper<Space> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(Space::getId, space.getId());

                long size = space.getTotalSize() - picture.getPicSize();
                size = (size <= 0) ? 0 : size;
                updateWrapper.set(Space::getTotalSize, size);
                updateWrapper.set(Space::getTotalCount, Math.max((space.getTotalCount() - 1), 0));
                boolean spaceUpdated = spaceService.update(updateWrapper);
                if (!spaceUpdated) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间回收失败");
                }
            }
            return null;
        });

        // 不要删图的文件，交给以后的定时任务或者别的东西
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
        queryWrapper.isNull(queryRequest.isNullSpaceId(), Picture::getSpaceId);

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
            applyColorCondition(queryWrapper, picColor, queryRequest.getPicSimilarity());
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
        Picture picture = this.getById(pid);
        if (picture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        User loggedUser = userService.getLoginUser(servletRequest);
        // 私人空间，校验是否有操作权限
        if (picture.getSpaceId() != null) {
            this.checkPictureAuth(picture, loggedUser);
        }
        // 拷贝图片数据并转换为VO
        PictureVo vo = PictureVo.toVO(picture);
        Long userId = picture.getUserId();
        if (vo != null && userId != null && userId > 0) {
            User user = userService.getById(userId);
            List<String> permissions = MrsAuthUtil.getPermissions(userId, loggedUser);
            vo.setPermissionList(permissions);
            vo.setUserVo(User.toVO(user));
        }
        return vo;
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
        User loginUser = userService.getLoginUser(servletRequest);
        UserRole currentRole = EnumUtil.fromValue(loginUser.getUserRole(), UserRole.class);
        Page<PictureVo> result = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        List<Picture> records = picturePage.getRecords();
        Set<Long> ids = records.stream().map(Picture::getUserId).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            result.setRecords(new ArrayList<>());
            return result;
        }
        Map<Long, List<UserVo>> userVos = userService.listByIds(ids).stream()
                .map(User::toVO)
                .collect(Collectors.groupingBy(userVo -> (userVo != null) ? userVo.getId() : -1));

        List<PictureVo> pictureVos = records.stream().map(picture -> {
            PictureVo vo = PictureVo.toVO(picture);
            if (vo != null) {
                // 不能在查列表的时候就拿到所有格式的图片信息
                // 不然容易被爬
                vo.setUrl(null);
                vo.setOriginalUrl(null);
                if (userVos.containsKey(picture.getUserId())) {
                    vo.setUserVo(userVos.get(picture.getUserId()).get(0));
                } else {
                    vo.setUserVo(null);
                }
                List<String> permissions = MrsAuthUtil.getPermissions(picture.getUserId(), loginUser.getId(), currentRole);
                vo.setPermissionList(permissions);
            }
            return vo;
        }).collect(Collectors.toList());
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
        Picture picture = this.getById(reviewRequest.getId());
        if (picture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        ReviewStatus currentStatus = EnumUtil.fromValue(picture.getReviewStatus(), ReviewStatus.class);
        if (currentStatus == reviewStatus) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复发起审核");
        }
        User loginUser = SessionUtil.getLoginUser(servletRequest);
        picture.setReviewerId(loginUser.getId());
        picture.setReviewStatus(reviewStatus.getValue());
        picture.setReviewMessage(reviewRequest.getReviewMessage());
        picture.setReviewTime(new Date());
        boolean updated = this.updateById(picture);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
    }

    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        UserRole currentRole = EnumUtil.fromValue(loginUser.getUserRole(), UserRole.class);
        // 未知角色、封禁中、游客等不能上传图片
        if (currentRole == null || currentRole.notThanRole(UserRole.USER)) {
            throw new BusinessException(ErrorCode.AUTHORIZATION_ERROR, "权限不足");
        }
        // 权限比管理员低
        if (currentRole.notThanRole(UserRole.ADMIN)) {
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
     */
    @Override
    public void checkPictureAuth(Picture picture, User loginUser) {
        Long spaceId = picture.getSpaceId();
        Long puid = picture.getUserId();
        Long uuid = loginUser.getId();
        // TODO: 这里设计的不对，要根据权限系统重构，但现在可以满足基础功能
        if (spaceId == null) {
            // 公共空间，仅本人和系统管理员可以操作
            boolean isAdmin = userService.hasPermission(loginUser, UserRole.ADMIN);
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
        System.out.println("搜索颜色: " + searchColor);
        System.out.println("相似度: " + similarity);
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
        System.out.println("色调桶范围: " + hueBuckets.stream().sorted().collect(Collectors.toList()));
        System.out.println("饱和度范围: " + satBuckets.stream().sorted().collect(Collectors.toList()));
        System.out.println("明度桶范围: " + valBuckets.stream().sorted().collect(Collectors.toList()));
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
        System.out.println("SQL: " + wrapper.getSqlSegment());

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