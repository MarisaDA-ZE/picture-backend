package cloud.marisa.picturebackend.service.impl;

import cloud.marisa.picturebackend.entity.dto.file.UploadPictureResult;
import cloud.marisa.picturebackend.service.IFileService;
import cloud.marisa.picturebackend.upload.picture.PictureMultipartFileUpload;
import cloud.marisa.picturebackend.upload.picture.PictureUrlUpload;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


/**
 * @author MarisaDAZE
 * @description 文件服务实现类
 * @date 2025/3/30
 */
@Service
public class FileServiceImpl implements IFileService {

    @Autowired
    private PictureMultipartFileUpload pictureMultipartFileUpload;

    @Autowired
    private PictureUrlUpload pictureUrlUpload;

    @Override
    public UploadPictureResult uploadPictureByFile(MultipartFile multipartFile, String pathPrefix) {
        return pictureMultipartFileUpload.uploadPictureObject(multipartFile, pathPrefix);
    }

    @Override
    public UploadPictureResult uploadPictureByURL(String url, String pathPrefix) {
        return pictureUrlUpload.uploadPictureObject(url, pathPrefix);
    }
}
