package cloud.marisa.picturebackend.entity.dto.api;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

@Data
@ToString
public class ApiKeyListResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<ApiKeyResponse> apiKeys;
    private Long total;
}
