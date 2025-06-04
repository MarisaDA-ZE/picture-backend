package cloud.marisa.picturebackend.entity.dto.api;

import lombok.Data;
import lombok.ToString;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Data
@ToString
public class ApiKeyCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "密钥名称不能为空")
    private String name;

    private String description;

    @NotNull(message = "空间ID列表不能为空")
    private List<Long> spaceIds;

    @NotNull(message = "每日调用限制不能为空")
    private Integer dailyLimit;
}