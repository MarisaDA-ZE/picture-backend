package cloud.marisa.picturebackend.service;

import cloud.marisa.picturebackend.entity.dto.file.UploadPictureResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author MarisaDAZE
 * @description 文件服务接口
 * @date 2025/3/30
 * @deprecated 已废弃
 */
public interface IFileService {

    /**
     * 上传图片
     * <p>两个参数结合在一起才是文件在服务器上的保存地址</p>
     * <p>uploadPathPrefix + example.txt</p>
     *
     * @param multipartFile 图片文件对象
     * @param pathPrefix    上传图片的前缀(path/to/)
     * @return 封装好的图片信息
     */
    UploadPictureResult uploadPictureByFile(MultipartFile multipartFile, String pathPrefix);

    /**
     * 根据URL上传图片
     * <p>两个参数结合在一起才是文件在服务器上的保存地址</p>
     * <p>uploadPathPrefix + example.txt</p>
     *
     * @param url        图片URL路径
     * @param pathPrefix 上传图片的前缀(path/to/)
     * @return 封装好的图片信息
     */
    UploadPictureResult uploadPictureByURL(String url, String pathPrefix);
}
