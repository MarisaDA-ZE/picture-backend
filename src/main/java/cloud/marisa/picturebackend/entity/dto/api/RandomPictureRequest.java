package cloud.marisa.picturebackend.entity.dto.api;

import lombok.Data;
import lombok.ToString;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import java.io.Serializable;

@Data
@ToString
public class RandomPictureRequest implements Serializable{
    private static final long serialVersionUID = 1L;

    @NotNull(message = "accessKey不能为空")
    private String accessKey;

    @NotNull(message = "secretKey不能为空")
    private String secretKey;

    @Min(value = 1, message = "数量最小为1")
    @Max(value = 6, message = "数量最大为6")
    private Integer count = 1;

    private Integer minWidth;
    private Integer maxWidth;
    private Integer minHeight;
    private Integer maxHeight;
}
