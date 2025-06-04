package cloud.marisa.picturebackend.service.impl;

import cloud.marisa.picturebackend.entity.dao.ApiKeySpace;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cloud.marisa.picturebackend.service.IApiKeySpaceService;
import cloud.marisa.picturebackend.mapper.ApiKeySpaceMapper;
import org.springframework.stereotype.Service;

/**
* @author Marisa
* @description 针对表【api_key_space(API密钥与空间关联表)】的数据库操作Service实现
* @createDate 2025-06-05 01:33:26
*/
@Service
public class ApiKeySpaceServiceImpl extends ServiceImpl<ApiKeySpaceMapper, ApiKeySpace>
    implements IApiKeySpaceService {

}




