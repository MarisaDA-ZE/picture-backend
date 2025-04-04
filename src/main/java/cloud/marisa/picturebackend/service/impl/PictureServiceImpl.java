package cloud.marisa.picturebackend.service.impl;

import cloud.marisa.picturebackend.entity.dao.Picture;
import cloud.marisa.picturebackend.entity.dao.Space;
import cloud.marisa.picturebackend.entity.dao.User;
import cloud.marisa.picturebackend.entity.dto.common.DeleteRequest;
import cloud.marisa.picturebackend.entity.dto.file.UploadPictureResult;
import cloud.marisa.picturebackend.entity.dto.picture.PictureQueryRequest;
import cloud.marisa.picturebackend.entity.dto.picture.PictureReviewRequest;
import cloud.marisa.picturebackend.entity.dto.picture.PictureUploadBatchRequest;
import cloud.marisa.picturebackend.entity.dto.picture.PictureUploadRequest;
import cloud.marisa.picturebackend.entity.vo.PictureVo;
import cloud.marisa.picturebackend.entity.vo.UserVo;
import cloud.marisa.picturebackend.enums.ReviewStatus;
import cloud.marisa.picturebackend.enums.SortEnum;
import cloud.marisa.picturebackend.enums.UserRole;
import cloud.marisa.picturebackend.exception.BusinessException;
import cloud.marisa.picturebackend.exception.ErrorCode;
import cloud.marisa.picturebackend.mapper.PictureMapper;
import cloud.marisa.picturebackend.service.IPictureService;
import cloud.marisa.picturebackend.service.ISpaceService;
import cloud.marisa.picturebackend.service.IUserService;
import cloud.marisa.picturebackend.upload.picture.PictureMultipartFileUpload;
import cloud.marisa.picturebackend.upload.picture.PictureUploadTemplate;
import cloud.marisa.picturebackend.upload.picture.PictureUrlUpload;
import cloud.marisa.picturebackend.util.*;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static cloud.marisa.picturebackend.common.Constants.USER_LOGIN;

/**
 * @author Marisa
 * @description 针对表【picture(图片表)】的数据库操作Service实现
 * @createDate 2025-03-29 22:12:31
 */
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
    private PictureMultipartFileUpload pictureMultipartFileUpload;

    @Autowired
    private PictureUrlUpload pictureUrlUpload;

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
        Long spaceId = uploadRequest.getSpaceId();
        // 上传到指定空间，spaceId为空表示公共空间
        if (spaceId != null) {
            boolean isAdmin = userService.hasPermission(loginUser, UserRole.ADMIN);
            // 用户只能上传到自己的空间
            boolean exists = spaceService.lambdaQuery()
                    .eq(Space::getId, spaceId)
                    // 不是管理员，只能传到自己的空间
                    .eq(!isAdmin, Space::getUserId, loginUser.getId())
                    .exists();
            if (!exists) {
                throw new BusinessException(ErrorCode.AUTHORIZATION_ERROR, "空间不存在或没有权限");
            }
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
            if (!picture.getUserId().equals(loginUser.getId()) && !userService.hasPermission(loginUser, UserRole.ADMIN)) {
                throw new BusinessException(ErrorCode.FORBIDDEN_ERROR);
            }
        }
        UserRole currentRole = EnumUtil.fromValue(loginUser.getUserRole(), UserRole.class);
        // 封禁账号、游客等不能上传图片
        if (currentRole == null || currentRole.getLevel() < UserRole.USER.getLevel()) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR);
        }


        // 上传图片（模板方法模式）
        PictureUploadTemplate uploadTemplate = pictureMultipartFileUpload;
        System.out.println("inputSource: " + inputSource);
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
            // 保存图片到文件服务器
            UploadPictureResult uploadPictureResult = uploadTemplate.uploadPictureObject(inputSource, MrsPathUtil.repairPath(PICTURE_PATH));
            System.out.println("上传耗时: " + (System.currentTimeMillis() - current));
            picture = getPicture(loginUser, uploadPictureResult);
        } else {
            picture = getPicture(loginUser, records.get(0));
            System.out.println("秒传啦");
            System.out.println("上传耗时: " + (System.currentTimeMillis() - current));
        }

        // 自定义文件名（入库名不影响存储名）
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
        boolean saved = this.saveOrUpdate(picture);
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存图片失败");
        }
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

    /**
     * 封装图片信息
     *
     * @param loginUser           登录用户
     * @param uploadPictureResult 上传图片信息
     * @return 图片的DAO对象
     */
    private static Picture getPicture(User loginUser, UploadPictureResult uploadPictureResult) {
        Picture picture = new Picture();
        picture.setUserId(loginUser.getId());
        picture.setName(uploadPictureResult.getPicName());
        // 默认图
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setSavedPath(uploadPictureResult.getSavedPath());
        // 拇指图
        picture.setThumbPath(uploadPictureResult.getThumbPath());
        picture.setThumbnailUrl(uploadPictureResult.getUrlThumb());
        // 原图
        picture.setOriginalPath(uploadPictureResult.getOriginalPath());
        picture.setOriginalUrl(uploadPictureResult.getUrlOriginal());
        // ...
        picture.setMd5(uploadPictureResult.getMd5());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
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
        Object attribute = httpServletRequest.getSession().getAttribute(USER_LOGIN);
        if (attribute == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        User loginedUser = (User) attribute;
        Picture picture = this.getById(picId);
        // 既不是删自己的，又不是管理员
        if (!picture.getUserId().equals(loginedUser.getId()) &&
                !userService.hasPermission(loginedUser, UserRole.ADMIN)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR);
        }
        this.removeById(picId);
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
        // 图片宽度，等值匹配
        if (queryRequest.getPicWidth() != null) {
            queryWrapper.eq(Picture::getPicWidth, queryRequest.getPicWidth());
        }
        // 图片高度，等值匹配
        if (queryRequest.getPicHeight() != null) {
            queryWrapper.eq(Picture::getPicHeight, queryRequest.getPicHeight());
        }
        // 图片长宽比，等值匹配
        if (queryRequest.getPicScale() != null) {
            queryWrapper.eq(Picture::getPicScale, queryRequest.getPicScale());
        }
        // 图片类型，等值匹配
        if (StrUtil.isNotBlank(queryRequest.getPicFormat())) {
            queryWrapper.eq(Picture::getPicFormat, queryRequest.getPicFormat());
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
    public PictureVo getPictureVo(Picture picture) {
        PictureVo vo = PictureVo.toVO(picture);
        Long userId = picture.getUserId();
        if (vo != null && userId != null && userId > 0) {
            User user = userService.getById(userId);
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
        Page<PictureVo> result = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        List<Picture> records = picturePage.getRecords();
        System.out.println(records);
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
            }
            return vo;
        }).collect(Collectors.toList());
        result.setRecords(pictureVos);
        return result;
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
        // 封禁账号、游客等不能上传图片
        if (currentRole == null || currentRole.getLevel() < UserRole.USER.getLevel()) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR);
        }
        // 权限比管理员低
        if (currentRole.getLevel() < UserRole.ADMIN.getLevel()) {
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
}