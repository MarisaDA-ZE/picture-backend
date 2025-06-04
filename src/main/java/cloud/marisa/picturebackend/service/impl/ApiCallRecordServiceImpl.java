package cloud.marisa.picturebackend.service.impl;

import cloud.marisa.picturebackend.entity.dao.ApiCallRecord;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cloud.marisa.picturebackend.service.IApiCallRecordService;
import cloud.marisa.picturebackend.mapper.ApiCallRecordMapper;
import org.springframework.stereotype.Service;

/**
* @author Marisa
* @description 针对表【api_call_record(API调用记录表)】的数据库操作Service实现
* @createDate 2025-06-05 01:33:26
*/
@Service
public class ApiCallRecordServiceImpl extends ServiceImpl<ApiCallRecordMapper, ApiCallRecord>
    implements IApiCallRecordService {

}




