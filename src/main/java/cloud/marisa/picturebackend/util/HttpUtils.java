package cloud.marisa.picturebackend.util;

import okhttp3.*;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.TimeUnit;


/**
 * @Author: MarisaDAZE
 * @Description: Http工具类
 * @Date: 2024/1/21
 */
@Service
public class HttpUtils {
    private static final OkHttpClient client;

    // Post的最大响应体大小（512M）
    private static final Long MAX_RESPONSE_SIZE = 1024 * 1024 * 512L;

    static {
        client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)  // 连接超时时间
                .readTimeout(60, TimeUnit.SECONDS)     // 读取超时时间
                .writeTimeout(60, TimeUnit.SECONDS)    // 写入超时时间
                .build();
    }

    public static String get(String url) {
        byte[] bytes = _get(url);
        if (bytes == null) return null;
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static String post(String url, String content) {
        byte[] bytes = _post(url, content);
        if (bytes == null) return null;
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * 发送GET请求
     *
     * @param url 请求URL
     */
    public static InputStream getInputStream(String url) {
        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (body != null) {
                return body.byteStream();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 发送POST请求
     *
     * @param url     请求URL
     * @param content 请求参数（JSON风格的字符串）
     * @return 请求体数据的文件对象
     */
    public static File postFile(String url, String content) {

        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, content);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();
        try (Response response = client.newCall(request).execute();
             ResponseBody resBody = response.body()) {
            if (!response.isSuccessful()) {
                System.out.println("Unexpected code: " + response);
                return null;
            }
            if (!ObjectUtils.isEmpty(resBody)) {
                String contentType = response.header("Content-Type");
                String fileType = getFileType(contentType);
                Path tempFile = Files.createTempFile("temp", fileType);
                File file = tempFile.toFile();
                try (InputStream is = resBody.byteStream();
                     FileOutputStream fos = new FileOutputStream(file)) {
                    byte[] bytes = new byte[2048];
                    int read;
                    while ((read = is.read(bytes)) != -1) {
                        fos.write(bytes, 0, read);
                    }
                    fos.flush();
                } catch (Exception e) {
                    System.err.println("(HttpUtils.postFile)出错了." + e.getMessage());
                }
                return file;
            }
            return null;
        } catch (IOException e) {
            System.err.println("(HttpUtils._post)POST请求出错了." + e.getMessage());
        }
        return null;
    }

    /**
     * 发送POST请求
     *
     * @param url     请求URL
     * @param content 请求参数（JSON风格的字符串）
     * @return 请求体数据的Base64编码格式字符串
     */
    public static String postBase64(String url, String content) {
        return Base64.getEncoder().encodeToString(_post(url, content));
    }

    /**
     * 发送POST请求
     *
     * @param url     请求URL
     * @param content 请求参数（JSON风格的字符串）
     * @return 结果字节数组
     */
    public static byte[] postBytes(String url, String content) {
        return _post(url, content);
    }


    /**
     * 发送GET请求
     *
     * @param url URL地址
     * @return 结果字节数组
     */
    private static byte[] _get(String url) {
        Request request = new Request.Builder()
                .url(url)
                .build();
        try (Response response = client.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (!ObjectUtils.isEmpty(body)) {
                return body.bytes();
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 发送POST请求
     *
     * @param url     请求URL
     * @param content 请求参数（JSON风格的字符串）
     * @return 结果字节数组
     */
    private static byte[] _post(String url, String content) {
        try (InputStream is = postInputStream(url, content)) {
            if (is == null) {
                return null;
            }
            int available = is.available();
            if (available > MAX_RESPONSE_SIZE) {
                throw new RuntimeException("Response body is too large: " + available + " bytes");
            }
            return IOUtils.toByteArray(is);
        } catch (IOException e) {
            System.err.println("(HttpUtils._post)POST请求出错了." + e.getMessage());
        }
        return null;
    }

    /**
     * 发送POST请求
     *
     * @param url     请求URL
     * @param content 请求参数（JSON风格的字符串）
     * @return 结果输入流
     */
    public static InputStream postInputStream(String url, String content) {
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, content);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();
        try (Response response = client.newCall(request).execute();
             ResponseBody resBody = response.body()) {
            if (!response.isSuccessful()) {
                System.out.println("Unexpected code: " + response);
                return null;
            }
            if (!ObjectUtils.isEmpty(resBody)) {
                return resBody.byteStream();
            }
            return null;
        } catch (IOException e) {
            System.err.println("(HttpUtils._post)POST请求出错了." + e.getMessage());
        }
        return null;
    }


    /**
     * 替换URL的域名代理
     *
     * @param url   资源地址
     * @param proxy 域名代理（不要协议头、尾部的斜杠，类似于 "i.kmarisa.icu"）
     * @return 替换代理后的URL地址
     */
    public static String setProxy(String url, String proxy) {

        int schemeEnd = url.indexOf("://") + 3;
        int domainEnd = url.indexOf('/', schemeEnd);
        if (domainEnd == -1) {
            domainEnd = url.length();
        }
        String scheme = url.substring(0, schemeEnd);
        String pathAndQuery = url.substring(domainEnd);
        return scheme + proxy + pathAndQuery;
    }

    /**
     * 根据contentType获取文件类型
     *
     * @param contentType MIME类型
     * @return 文件类型
     */
    public static String getFileType(String contentType) {
        if (contentType != null) {
            String[] parts = contentType.split(";");
            String mimeType = parts[0].trim(); // 去掉可能的多余参数（如 charset）
            String[] typeParts = mimeType.split("/");
            if (typeParts.length == 2) {
                String mainType = typeParts[0]; // 主类型
                // String subType = typeParts[1];  // 子类型
                return "." + mainType;
            }
        }
        return null;
    }
}
