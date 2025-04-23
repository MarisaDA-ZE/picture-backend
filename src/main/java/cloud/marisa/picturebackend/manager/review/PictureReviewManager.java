package cloud.marisa.picturebackend.manager.review;

import cloud.marisa.picturebackend.api.picgreen.ImageModerationApi;
import cloud.marisa.picturebackend.api.picgreen.MrsPictureIllegal;
import cloud.marisa.picturebackend.config.aliyun.green.MrsImageModeration;
import cloud.marisa.picturebackend.entity.dao.Notice;
import cloud.marisa.picturebackend.entity.dao.Picture;
import cloud.marisa.picturebackend.enums.ReviewStatus;
import cloud.marisa.picturebackend.enums.notice.MrsNoticeRead;
import cloud.marisa.picturebackend.enums.notice.MrsNoticeType;
import cloud.marisa.picturebackend.queue.OverflowStorageDao;
import cloud.marisa.picturebackend.service.INoticeService;
import cloud.marisa.picturebackend.service.IPictureService;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author MarisaDAZE
 * @description 图片的AI审核管理器
 * @date 2025/4/18
 */
@Log4j2
@Component
public class PictureReviewManager {

    /**
     * 用于处理AI图片审核的线程池
     */
    @Resource
    private ThreadPoolExecutor asyncReviewThreadPool;

    /**
     * 图片服务
     */
    @Lazy
    @Resource
    private IPictureService pictureService;

    /**
     * 消息通知服务
     */
    @Resource
    private INoticeService noticeService;


    /**
     * AI图片自动审核接口（基于阿里云）
     */
    @Resource
    private ImageModerationApi imageModerationApi;

    @Resource
    private OverflowStorageDao<Picture> overflowStorage;

    // 每隔5秒尝试进行一次审核
    @Scheduled(cron = "0/5 * * * * ?")
    public void startConsumer() {
        Picture picture = overflowStorage.loadOne();
        if (picture == null) {
            return;
        }
        asyncReviewThreadPool.execute(() -> {
            Long pid = picture.getId();
            log.info("开始自动审核图片 {}", pid);
            try {
                aiAutoReview(picture);
            } catch (Exception ex) {
                log.error("AI图片审核失败", ex);
                log.error("图片审核失败，请管理员手动介入，图片ID {}", pid);
            }
        });
    }

    /**
     * AI自动审核图片内容
     *
     * @param picture 图片对象
     * @throws Exception 可能的异常
     */
    public void aiAutoReview(Picture picture) throws Exception {
        String url = picture.getUrl();
        log.info("imageURL {}", url);
        String taskId = imageModerationApi.resolveResponse(imageModerationApi.createTask(url));
        log.info("创建的任务ID {}", taskId);
        int count = 0;
        int maxCount = 15;
        do {
            Thread.sleep(5000);
            MrsImageModeration result = imageModerationApi.queryResultByTaskId(taskId);
            if (result.isProcessing()) {
                count++;
                log.info("等待结果中 {}/{}", count, maxCount);
                continue;
            }
            if (result.isSuccess()) {
                log.info("审核结果 {}", result);
                MrsPictureIllegal illegal = imageModerationApi.isIllegal(result);
                boolean legal = illegal.isLegal();
                log.info("图片是否合规 {}", legal);
                LambdaUpdateWrapper<Picture> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(Picture::getId, picture.getId());
                // 审核状态和消息
                String reviewMessage;
                ReviewStatus reviewStatus;
                // 粗审通过
                if (legal) {
                    reviewStatus = ReviewStatus.PASS;
                    reviewMessage = "AI自动审核通过, " + illegal.getReason();
                } else {
                    // 粗审不通过，看是不是属于违规图
                    boolean hasRisk = imageModerationApi.isAllowedRisk(illegal);
                    log.info("是不是真的违规了 {}", hasRisk);
                    // 真违规还是假违规
                    if (hasRisk) {
                        log.info("图片违规原因 {}", illegal.getReason());
                        log.info("图片违规原因列表 {}", illegal.getReasons());
                        log.info("图片违规详细原因 {}", JSONObject.toJSONString(illegal.getResult()));
                        reviewStatus = ReviewStatus.REJECT;
                        reviewMessage = "AI自动审核不通过, 因为存在：" + illegal.getReason();
                    } else {
                        log.info("图片没有违规 {}", illegal.getReason());
                        reviewStatus = ReviewStatus.PASS;
                        reviewMessage = "AI自动审核通过, 但存在" + illegal.getReason() + "的敏感元素";
                    }
                }
                updateWrapper.set(Picture::getReviewStatus, reviewStatus.getValue());
                updateWrapper.set(Picture::getReviewMessage, reviewMessage);
                overflowStorage.delete(Collections.singletonList(picture));
                // 立即删除缓存
                List<Long> ids = Collections.singletonList(picture.getId());
                // 更新数据库和缓存
                pictureService.removeCacheByKeys(ids);
                pictureService.update(updateWrapper);
                pictureService.delayRemoveCacheByKeys(ids);
                pushMessage(picture, reviewMessage);
                break;
            }
            log.error("获取审核结果超时 {}", result);
        } while (count < maxCount);
    }


    private void pushMessage(Picture picture, String reviewMessage) {
        // 消息对象
        Notice notice = new Notice();
        notice.setNoticeType(MrsNoticeType.SYSTEM.getValue());
        notice.setSenderId(0L); // 系统发送时候 senderId 为0
        notice.setUserId(picture.getUserId());
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
                reviewMessage
        );
        notice.setContent(msg);
        Map<String, Object> params = new HashMap<>();
        params.put("pictureId", String.valueOf(picture.getId()));
        notice.setAdditionalParams(JSONUtil.toJsonStr(params));
        notice.setCreateTime(new Date());
        noticeService.save(notice);
        noticeService.pushMessage(picture.getUserId(), notice);
    }
}
