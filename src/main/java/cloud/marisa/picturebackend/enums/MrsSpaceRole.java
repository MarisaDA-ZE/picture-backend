package cloud.marisa.picturebackend.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author MarisaDAZE
 * @description 空间角色的枚举
 * @date 2025/4/13
 */
@Getter
public enum MrsSpaceRole implements MrsBaseEnum<String> {

    /**
     * 封禁用户
     * <p>什么都不可以</p>
     */
    BAN("ban", -1),

    /**
     * 访客
     * <p>可以：</p>
     * <p>①浏览、下载、分享空间图库的内容</p>
     */
    VIEWER("viewer", 1),

    /**
     * 编辑者
     * <p>可以：</p>
     * <p>①浏览、下载、分享空间图库的内容</p>
     * <p>②编辑空间图库的数据</p>
     */
    EDITOR("editor", 5),

    /**
     * 管理员
     * <p>可以：</p>
     * <p>①浏览、下载、分享空间图库的内容</p>
     * <p>②管理空间图库数据</p>
     * <p>③审核用户上传空间图库的图片</p>
     */
    ADMIN("admin", 10);

    MrsSpaceRole(String value, int level) {
        this.value = value;
        this.level = level;
    }

    /**
     * 角色名称
     */
    private final String value;

    /**
     * 角色等级
     */
    private final Integer level;


    public static List<String> getAllTexts() {
        return Arrays.stream(MrsSpaceRole.values())
                .map(MrsSpaceRole::getValue)
                .collect(Collectors.toList());
    }
}
