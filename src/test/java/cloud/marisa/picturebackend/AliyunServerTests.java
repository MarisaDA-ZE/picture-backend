package cloud.marisa.picturebackend;

import cloud.marisa.picturebackend.api.picgreen.ImageModerationApi;
import cloud.marisa.picturebackend.api.picgreen.MrsPictureIllegal;
import cloud.marisa.picturebackend.config.aliyun.green.MrsImageModeration;
import cloud.marisa.picturebackend.api.image.AliyunOssUtil;
import cloud.marisa.picturebackend.entity.dto.file.UploadPictureResult;
import cloud.marisa.picturebackend.manager.upload.PictureUploadManager;
import com.alibaba.fastjson2.JSONObject;
import com.aliyun.oss.model.PutObjectResult;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.*;


/**
 * @author MarisaDAZE
 * @description 阿里云OSS对象存储测试
 * @date 2023/4/17
 */
@Log4j2
@SpringBootTest
public class AliyunServerTests {

    @Autowired
    private AliyunOssUtil ossUtil;

    @Autowired
    private ImageModerationApi imageModerationApi;

    @Autowired
    private PictureUploadManager pictureUploadManager;

    @Test
    public void ossTest() throws Exception {
        // 文件上传
        String path = "C:\\Users\\Marisa\\Desktop\\test-r18\\test_4.png";
        String fileName = "picture/common/sex_3.png";
        PutObjectResult uploadResult = ossUtil.uploadInputStream(fileName, new FileInputStream(path));
        log.info("上传对象信息 {}", uploadResult);

        // 生成以GET方法访问的预签名URL。本示例没有额外请求头，其他人可以直接通过浏览器访问相关内容。
        String url = ossUtil.generatePresignedUrl(fileName, 3600);
        log.info("文件访问地址 {}", url);

        // 文件下载
        String output = "C:\\Users\\Marisa\\Desktop\\test-r18\\output.png";
        downloadFile(fileName, output);
    }

    @Test
    public void ossPictureUploadTest() throws Exception {
        // 文件上传（同步模式）
        // ≈1MB 耗时 1491ms
        // ≈10MB 耗时 4885ms
        String path = "C:\\Users\\Marisa\\Desktop\\thems\\126638656_p0.png";
        String uploadPath = "picture/test/";
        String fileName = "marisa2.jpg";
        long start = System.currentTimeMillis();
        UploadPictureResult result = pictureUploadManager.uploadPictureInputStream(fileName, uploadPath, new FileInputStream(path));
        log.info("上传对象信息 {}", result);
        log.info("业务耗时 {}ms", (System.currentTimeMillis() - start));
    }

    @Test
    public void aiGreenTest() throws Exception {
        String fileName = "picture/common/sex_2.png";
        // String fileName = "picture/common/M200.png";
        String url = ossUtil.generatePresignedUrl(fileName, 3600);
        log.info("imageURL {}", url);
        String taskId = imageModerationApi.resolveResponse(imageModerationApi.createTask(url));
        // String taskId = "572C9551-5953-5EC1-8C21-1534D19945E2";
        log.info("创建的任务ID {}", taskId);
        int count = 0;
        int maxCount = 5;
        do {
            Thread.sleep(3000);
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
                if (!legal) {
                    boolean hasRisk = imageModerationApi.isAllowedRisk(illegal);
                    log.info("是不是真的违规了 {}", hasRisk);
                    // 真违规还是假违规
                    if (hasRisk) {
                        log.info("图片违规原因 {}", illegal.getReason());
                        log.info("图片违规原因列表 {}", illegal.getReasons());
                        log.info("图片违规详细原因 {}", JSONObject.toJSONString(illegal.getResult()));
                    } else {
                        log.info("图片没有违规 {}", illegal.getReason());
                    }
                }
                break;
            }
            log.error("获取审核结果超时 {}", result);
        } while (count < maxCount);
    }

    private void downloadFile(String filePath, String output) {
        File file = new File(output);
        try (InputStream is = ossUtil.downloadInputStream(filePath);
             FileOutputStream fos = new FileOutputStream(file)) {
            byte[] bytes = new byte[2048];
            int read;
            long size = 0;
            while ((read = is.read(bytes)) != -1) {
                size += read;
                fos.write(bytes, 0, read);
            }
            fos.flush();
            log.info("文件保存成功，文件大小 {}", size);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
