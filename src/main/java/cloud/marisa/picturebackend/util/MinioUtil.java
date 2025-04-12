package cloud.marisa.picturebackend.util;

import io.minio.*;
import io.minio.http.Method;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;
import cloud.marisa.picturebackend.config.MinioConfig;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;

import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

@Component
public class MinioUtil {

    @Autowired
    private MinioConfig configuration;

    @Autowired
    private MinioClient minioClient;

    /**
     * 判断bucket是否存在，不存在则创建
     */
    public boolean existBucket(String bucketName) {
        boolean exists;
        try {
            exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                exists = true;
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            exists = false;
        }
        return exists;
    }

    /**
     * 删除bucket
     */
    public Boolean removeBucket(String bucketName) {
        try {
            minioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * 上传文件
     * <p>可以在文件名一栏指定上传到的子路径,如/temp/example.txt</p>
     *
     * @param file     MIME文件
     * @param fileName 文件名称（example.txt）
     */
    public void upload(MultipartFile file, String fileName) {
        try {
            upload(file.getInputStream(), fileName);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * 上传文件
     *
     * @param file 文件对象
     */
    public void upload(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            upload(fis, file.getName());
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * 上传文件
     * <p>可以在文件名一栏指定上传到的子路径,如/temp/example.txt</p>
     *
     * @param file     文件对象
     * @param fileName 文件名（example.txt）
     */
    public void upload(File file, String fileName) {
        try (FileInputStream fis = new FileInputStream(file)) {
            upload(fis, fileName);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * 上传文件
     * <p>可以在文件名一栏指定上传到的子路径,如/temp/example.txt</p>
     * <p>方法调用后自动关闭流</p>
     *
     * @param is       文件流
     * @param fileName 文件名称（example.txt）
     */
    public void upload(InputStream is, String fileName) {
        if (is == null) {
            System.err.println("文件流是空的.");
            return;
        }
        // 使用putObject上传一个文件到存储桶中。
        try (is) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(configuration.getBucketName())
                    .object(fileName)
                    .stream(is, is.available(), -1)
                    .contentType(getContentType(fileName))
                    .build());
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("文件上传出错==>" + e.getMessage());
        }
    }

    /**
     * 获取文件的类型
     *
     * @param fileName 文件名
     * @return 文件类型
     */
    private String getContentType(String fileName) {
        return MediaTypeFactory.getMediaType(fileName)
                .map(MimeType::toString)
                .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);
    }

    /**
     * 获取文件访问地址（有过期时间）
     * <p>时间单位默认为秒</p>
     *
     * @param fileName 文件名称
     * @param time     时间
     */
    public String getExpireFileUrl(String fileName, int time) {
        return getExpireFileUrl(fileName, time, TimeUnit.SECONDS);
    }

    /**
     * 获取文件访问地址（有过期时间）
     *
     * @param fileName 文件名称
     * @param time     时间
     * @param timeUnit 时间单位
     */
    public String getExpireFileUrl(String fileName, int time, TimeUnit timeUnit) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(configuration.getBucketName())
                    .object(fileName)
                    .expiry(time, timeUnit).build());
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return null;
    }

    /**
     * 获取文件访问地址
     *
     * @param fileName 文件名称
     */
    public String getFileUrl(String fileName) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(configuration.getBucketName())
                    .object(fileName)
                    .build()
            );
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return null;
    }

    /**
     * 下载文件为文件对象
     * <p>用完之后记得把文件删除，不然会一直占用磁盘空间</p>
     *
     * @param minioPath 文件在minIO上的存储路径（/path/to/example.txt）
     * @param fileName  保存为文件的名称（example.txt）
     * @return 输入流
     */
    public File downloadFile(String minioPath, String fileName) {
        File res = new File(fileName);
        try (InputStream is = downloadInputStream(minioPath);
             FileOutputStream fos = new FileOutputStream(res);
        ) {
            byte[] bytes = new byte[2048];
            int read;

            while ((read = is.read(bytes)) != -1) {
                fos.write(bytes, 0, read);
            }
            fos.flush();
            return res;
        } catch (Exception e) {
            System.err.println("报错了==>" + e.getMessage());
        }
        return null;
    }

    /**
     * 下载文件为字节数组
     * <p>对大文件不要用这个，小心内存泄露</p>
     *
     * @param filePath 文件在minIO上的存储路径（/path/to/example.txt）
     * @return 输入流
     */
    public byte[] downloadBytes(String filePath) {
        try (InputStream is = downloadInputStream(filePath)) {
            int available = is.available();
            byte[] bytes = new byte[available];
            int read = is.read(bytes);
            return bytes;
        } catch (Exception e) {
            System.err.println("报错了==>" + e.getMessage());
        }
        return null;
    }

    /**
     * 下载文件为字节流
     *
     * @param minioPath 文件在minIO上的存储路径（/path/to/example.txt）
     * @return 输入流
     */
    public InputStream downloadInputStream(String minioPath) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(configuration.getBucketName())
                            .object(minioPath)
                            .build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 删除文件
     *
     * @param fileName 文件名称
     */
    public void delete(String fileName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs
                            .builder()
                            .bucket(configuration.getBucketName())
                            .object(fileName)
                            .build()
            );
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private static MinioUtil localCall() {
//        String host = "https://192.168.10.254:9000";
        String host = "https://oss.marisa.cloud:9000";
        String accessKey = "marisa";
        String secretKey = "$pwdByMIO6860.";
        String bucketName = "picture-backend";
        MinioUtil minioUtil = new MinioUtil();
        MinioConfig config = new MinioConfig();
        config.setUrl(host);
        config.setAccessKey(accessKey);
        config.setSecretKey(secretKey);
        config.setBucketName(bucketName);
        minioUtil.configuration = config;

        // 使用自定义 HTTP 客户端初始化 MinioClient
        minioUtil.minioClient = MinioClient.builder()
                .endpoint(config.getUrl())
                .credentials(config.getAccessKey(), config.getSecretKey())
                .build();
        return minioUtil;

    }

    public static void main(String[] args) {
        MinioUtil instance = localCall();
        String fileUrl = instance.getFileUrl("/picture/124891671_p0.jpg");
        System.out.println(fileUrl);
//        String path = "C:\\Users\\Marisa\\Desktop\\thems\\124891671_p0.jpg";
//        File file = new File(path);
//        instance.upload(file, "test.jpg");
//        System.out.println("上传完成");
//        System.exit(0);
    }
}