package cloud.marisa.picturebackend.api.image;

import cloud.marisa.picturebackend.config.aliyun.oss.AliyunOssConfigProperties;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.common.auth.*;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;

/**
 * @author MarisaDAZE
 * @description OSS对象存储 工具类
 * @date 2025/4/17
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class AliyunOssUtil {

    /**
     * 对象存储客户端
     */
    private final OSS ossClient;

    /**
     * 对象存储配置信息
     */
    private final AliyunOssConfigProperties properties;


    /**
     * 根据名称创建一个存储桶
     *
     * @param bucketName 存储桶名（mrs-example-bucket）
     */
    public void createBucket(String bucketName) {
        ossClient.createBucket(bucketName);
    }


    /**
     * 上传一个文件到存储桶
     * <p>默认存储桶为配置文件中指定的</p>
     *
     * @param fileName 保存的文件全路径名（path/to/example.txt）
     * @param is       文件输入流
     * @return 文件上传信息
     */
    public PutObjectResult uploadInputStream(String fileName, InputStream is) {
        return uploadInputStream(properties.getBucketName(), fileName, is);
    }

    /**
     * 上传一个文件到存储桶
     *
     * @param bucketName 存储桶名(mrs-example-bucket)
     * @param fileName   保存的文件全路径名（path/to/example.txt）
     * @param is         文件输入流
     * @return 文件上传信息
     */
    public PutObjectResult uploadInputStream(String bucketName, String fileName, InputStream is) {
        return ossClient.putObject(bucketName, fileName, is);
    }

    /**
     * 从OSS文件服务器上下载内容
     * <p>默认存储桶为配置文件中指定的</p>
     *
     * @param fileName 文件保存全路径（path/to/example.txt）
     * @return 文件数据的输入流
     */
    public InputStream downloadInputStream(String fileName) {
        return downloadInputStream(properties.getBucketName(), fileName);
    }

    /**
     * 从OSS文件服务器上下载内容
     *
     * @param bucketName 存储桶名（mrs-example-bucket）
     * @param fileName   文件保存全路径（path/to/example.txt）
     * @return 文件数据的输入流
     */
    public InputStream downloadInputStream(String bucketName, String fileName) {
        OSSObject ossObject = ossClient.getObject(bucketName, fileName);
        return ossObject.getObjectContent();
    }

    /**
     * 生成文件的访问链接
     * <p>默认过期时间单位 秒</p>
     *
     * @param fileName 文件的全路径（/path/to/example.txt）
     * @param expired  过期时间（秒）
     * @return 文件的URL地址
     */
    public String generatePresignedUrl(String fileName, long expired) {
        return generatePresignedUrl(properties.getBucketName(), fileName, expired, TimeUnit.SECONDS);
    }

    /**
     * 生成文件下载链接
     *
     * @param fileName 文件的全路径（/path/to/example.txt）
     * @param expired  过期时间
     * @param unit     时间单位
     * @return 文件的URL地址
     */
    public String generatePresignedUrl(String fileName, long expired, TimeUnit unit) {
        return generatePresignedUrl(properties.getBucketName(), fileName, expired, unit);
    }

    /**
     * 获取文件的信息
     *
     * @param request 要查询的参数
     * @return 查到的结果
     */
    public OSSObject getObject(GetObjectRequest request) {
        return ossClient.getObject(request);
    }

    /**
     * 生成文件下载链接
     *
     * @param bucketName 存储桶名（mrs-example）
     * @param fileName   文件的全路径（/path/to/example.txt）
     * @param expired    过期时间
     * @param unit       时间单位
     * @return 文件的URL地址
     */
    public String generatePresignedUrl(String bucketName, String fileName, long expired, TimeUnit unit) {
        long millis = unit.toMillis(expired);
        log.info("原始过期时间 {}, 时间单位 {}, 最终过期毫秒值 {}", expired, unit, millis);
        Date expiration = new Date(new Date().getTime() + millis);
        URL url = ossClient.generatePresignedUrl(bucketName, fileName, expiration);
        return url.toString();
    }

    /**
     * 列出一个存储桶中的所有文件
     * <p>默认存储桶为配置文件中指定的</p>
     *
     * @return 文件列表对象
     */
    public ObjectListing listBucketFiles() {
        return listBucketFiles(properties.getBucketName());
    }

    /**
     * 列出一个存储桶中的所有文件
     *
     * @param bucketName 存储桶名（mrs-example-bucket）
     * @return 文件列表对象
     */
    public ObjectListing listBucketFiles(String bucketName) {
        return ossClient.listObjects(bucketName);
    }

    /**
     * 根据文件路径，删除存储桶中的一个文件
     * <p>默认存储桶为配置文件中指定的</p>
     *
     * @param fileName 要删除的文件的全路径名（path/to/example.txt）
     */
    public void removeFile(String fileName) {
        removeFile(properties.getBucketName(), fileName);
    }

    /**
     * 根据文件路径，删除存储桶中的一个文件
     *
     * @param bucketName 存储桶名（mrs-example-bucket）
     * @param fileName   要删除的文件的全路径名（path/to/example.txt）
     */
    public void removeFile(String bucketName, String fileName) {
        ossClient.deleteObject(bucketName, fileName);
    }

    /**
     * 删除一个存储桶（包括里面的所有文件）
     * <p>默认存储桶为配置文件中指定的</p>
     */
    public void removeBucket() {
        removeBucket(properties.getBucketName());
    }

    /**
     * 删除一个存储桶（包括里面的所有文件）
     *
     * @param bucketName 存储桶名（mrs-example-bucket）
     */
    public void removeBucket(String bucketName) {
        ossClient.deleteBucket(bucketName);
    }


    public void getPermanentAddress(String bucketName, String fileName) {
        try {
            // 以下示例用于资源拥有者（即UID为174649585760xxxx的Bucket Owner）通过Bucket Policy授权指定用户（UID为20214760404935xxxx的RAM用户）拥有列举examplebucket下所有文件的权限。
            String policyText = "{\"Statement\": [{\"Effect\": \"Allow\", \"Action\": [\"oss:GetObject\", \"oss:ListObjects\"], \"Principal\": [\"20214760404935xxxx\"], \"Resource\": [\"acs:oss:*:174649585760xxxx:examplebucket/*\"]}], \"Version\": \"1\"}";

            // 设置Bucket Policy。
            ossClient.setBucketPolicy(bucketName, policyText);
        } catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
        } catch (ClientException ce) {
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message:" + ce.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

}
