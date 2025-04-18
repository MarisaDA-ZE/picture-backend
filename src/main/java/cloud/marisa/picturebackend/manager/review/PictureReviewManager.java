package cloud.marisa.picturebackend.manager.review;

import cloud.marisa.picturebackend.api.image.AliyunOssUtil;
import cloud.marisa.picturebackend.api.picgreen.ImageModerationApi;
import cloud.marisa.picturebackend.api.picgreen.MrsPictureIllegal;
import cloud.marisa.picturebackend.config.aliyun.green.MrsImageModeration;
import cloud.marisa.picturebackend.entity.dao.Picture;
import cloud.marisa.picturebackend.enums.ReviewStatus;
import cloud.marisa.picturebackend.queue.PersistentFallbackQueue;
import cloud.marisa.picturebackend.service.IPictureService;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
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
     * 阿里云对象存储OSS
     */
    @Resource
    private AliyunOssUtil ossUtil;


    /**
     * AI图片自动审核接口（基于阿里云）
     */
    @Resource
    private ImageModerationApi imageModerationApi;

    /**
     * 简易任务队列
     * <p>如果任务满了，会持久化到Redis</p>
     */
    @Resource
    private PersistentFallbackQueue reviewQueue;

    // 这里定时任务不应该这么设计
    // 或许就不应该用队列？
    @Scheduled(cron = "0/60 * * * * ?")
    public void startConsumer() throws Exception {
        Picture picture = reviewQueue.take();
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
        // 获取图片的URL，一小时过期
        // String savedPath = picture.getSavedPath();
        // String url = ossUtil.generatePresignedUrl(savedPath, 3600);
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
                // 粗审通过
                if (legal) {
                    updateWrapper.set(Picture::getReviewStatus, ReviewStatus.PASS.getValue());
                    updateWrapper.set(Picture::getReviewMessage, "AI自动审核通过, " + illegal.getReason());
                } else {
                    // 粗审不通过，看是不是属于违规图
                    boolean hasRisk = imageModerationApi.isAllowedRisk(illegal);
                    log.info("是不是真的违规了 {}", hasRisk);
                    // 真违规还是假违规
                    if (hasRisk) {
                        log.info("图片违规原因 {}", illegal.getReason());
                        log.info("图片违规原因列表 {}", illegal.getReasons());
                        log.info("图片违规详细原因 {}", JSONObject.toJSONString(illegal.getResult()));
                        updateWrapper.set(Picture::getReviewStatus, ReviewStatus.REJECT.getValue());
                        updateWrapper.set(Picture::getReviewMessage,  "AI自动审核不通过, " +illegal.getReason());
                    } else {
                        log.info("图片没有违规 {}", illegal.getReason());
                        updateWrapper.set(Picture::getReviewStatus, ReviewStatus.PASS.getValue());
                        updateWrapper.set(Picture::getReviewMessage, "AI自动审核通过, " + illegal.getReason());
                    }
                }
                // 立即删除缓存
                List<Long> ids = Collections.singletonList(picture.getId());
                // 更新数据库和缓存
                pictureService.removeCacheByKeys(ids);
                pictureService.update(updateWrapper);
                pictureService.delayRemoveCacheByKeys(ids);
                break;
            }
            log.error("获取审核结果超时 {}", result);
        } while (count < maxCount);
    }
}
