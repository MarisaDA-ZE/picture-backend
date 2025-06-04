package cloud.marisa.picturebackend.entity.dto.api;

import lombok.Data;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@ToString
public class ApiKeyUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "id不能为空")
    private Long id;
    @NotNull(message = "状态不能为空")
    private Integer status;

}