package cloud.marisa.picturebackend.entity.dto.api;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

@Data
@ToString
public class ApiKeyResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String accessKey;
    private String secretKey;
    private String name;
    private String description;
    private Integer status;
    private Integer dailyLimit;
    private List<Long> spaceIds;
}