package cloud.marisa.picturebackend.api.picgreen;

import cloud.marisa.picturebackend.enums.base.MrsBaseEnum;
import lombok.Getter;

import java.util.List;

/**
 * @author MarisaDAZE
 * @description 阿里云AI图片审核相关标签的枚举
 * @date 2025/4/17
 */
@Getter
public enum MrsAliyunGreenLabels implements MrsBaseEnum<String> {
    PORNOGRAPHIC_ADULT_CONTENT("pornographic_adultContent", "疑似含有成人色情内容", 70F),
    PORNOGRAPHIC_ADULT_CONTENT_TII("pornographic_adultContent_tii", "图中文字疑似含有色情内容", 70F),
    SEXUAL_SUGGESTIVE_CONTENT("sexual_suggestiveContent", "疑似含有疑似低俗或性暗示内容", 98F),
    SEXUAL_PARTIAL_NUDITY("sexual_partialNudity", "疑似含有包含肢体裸露或性感内容", 98F),
    // 铁拳，惹不起
    POLITICAL_POLITICAL_FIGURE("political_politicalFigure", "疑似含有政治人物的内容", 60F),
    POLITICAL_POLITICAL_FIGURE_NAME_TII("political_politicalFigure_name_tii", "图中文字疑似含有领导人姓名", 60F),
    POLITICAL_POLITICAL_FIGURE_METAPHOR_TII("political_politicalFigure_metaphor_tii", "图中文字疑似含有对主要领导人的代称、暗喻", 60F),
    POLITICAL_TV_LOGO("political_TVLogo", "疑似含有特定电视台台标", 100F),
    POLITICAL_MAP("political_map", "疑似含有中国地图", 100F),
    POLITICAL_OUTFIT("political_outfit", "疑似含有公务服装", 100F),
    POLITICAL_PROHIBITED_PERSON("political_prohibitedPerson", "疑似含有不宜宣传的人物的内容", 100F),
    POLITICAL_PROHIBITED_PERSON_TII("political_prohibitedPerson_tii", "图中文字疑似含有落马官员的姓名", 100F),
    POLITICAL_TAINTED_CELEBRITY("political_taintedCelebrity", "疑似含有重大负面的公众人物的内容", 100F),
    POLITICAL_TAINTED_CELEBRITY_TII("political_taintedCelebrity_tii", "图中文字疑似含有劣迹艺人的姓名", 100F),
    POLITICAL_FLAG("political_flag", "疑似含有国家或地区旗帜", 100F),
    POLITICAL_HISTORICAL_NIHILITY("political_historicalNihility", "疑似含有历史虚无内容", 100F),
    POLITICAL_HISTORICAL_NIHILITY_TII("political_historicalNihility_tii", "图中文字疑似含有历史虚无信息", 100F),
    POLITICAL_RELIGION_TII("political_religion_tii", "图中文字疑似含有宗教元素或信息", 100F),
    POLITICAL_RACISM_TII("political_racism_tii", "图中文字疑似含有歧视的表达内容", 100F),
    POLITICAL_BADGE("political_badge", "疑似含有国徽，党徽相关内容", 100F),
    VIOLENT_EXPLOSION("violent_explosion", "疑似含有烟火类内容元素", 100F),
    VIOLENT_GUN_KNIVES("violent_gunKnives", "疑似含有刀具、枪支等内容", 100F),
    VIOLENT_GUN_KNIVES_TII("violent_gunKnives_tii", "图中文字疑似含枪支刀具的描述", 100F),
    VIOLENT_ARMED_FORCES("violent_armedForces", "疑似含有武装元素", 100F),
    VIOLENT_CROWDING("violent_crowding", "疑似含有人群聚集元素", 100F),
    VIOLENT_HORRIFIC_CONTENT("violent_horrificContent", "疑似含有惊悚、血腥等内容", 100F),
    VIOLENT_HORRIFIC_TII("violent_horrific_tii", "图中文字疑似描述暴力、恐怖的内容", 100F),
    CONTRABAND_DRUG("contraband_drug", "含有疑似毒品等内容", 70F),
    CONTRABAND_DRUG_TII("contraband_drug_tii", "图中文字疑似描述违禁毒品", 70F),
    CONTRABAND_GAMBLE("contraband_gamble", "含有疑似赌博等内容", 100F),
    CONTRABAND_GAMBLE_TII("contraband_gamble_tii", "图中文字疑似描述赌博行为", 100F),
    FRAUD_VIDEO_ABUSE("fraud_videoAbuse", "图片疑似有隐藏视频风险", 100F),
    FRAUD_PLAYER_ABUSE("fraud_playerAbuse", "图片疑似有隐藏播放器风险", 100F);

    /**
     * 默认构造函数
     *
     * @param value       枚举值
     * @param description 标签描述
     * @param confidence  标签置信度
     */
    MrsAliyunGreenLabels(String value, String description, float confidence) {
        this.value = value;
        this.description = description;
        this.confidence = confidence;
    }

    /**
     * 标签名称
     */
    private final String value;

    /**
     * 标签的中文含义
     */
    private final String description;

    /**
     * 标签的置信度
     * <p>0~100分，分数越高置信度越高</p>
     * <p>你认为某个违规项多少分才是真违规 就写多少</p>
     * <p>比如你觉得”疑似含有刀具、枪支等内容“置信度达到95才算违规，那你就写95</p>
     */
    private final float confidence;

    /**
     * 返回一个默认的黑名单列表
     * <p>黑名单中的条目，只要命中一条就算图片违规</p>
     *
     * @return 黑名单列表
     */
    public static List<MrsAliyunGreenLabels> getDefaultBlackList() {
        return List.of(
                // ↓ 疑似含有成人色情内容
                PORNOGRAPHIC_ADULT_CONTENT,
                // ↓ 图中文字疑似含有色情内容
                PORNOGRAPHIC_ADULT_CONTENT_TII,
                // ↓ 疑似含有政治人物的内容
                POLITICAL_POLITICAL_FIGURE,
                // ↓ 图中文字疑似含有领导人姓名
                POLITICAL_POLITICAL_FIGURE_NAME_TII,
                // ↓ 图中文字疑似含有对主要领导人的代称、暗喻
                POLITICAL_POLITICAL_FIGURE_METAPHOR_TII,
                // ↓ 含有疑似毒品等内容
                CONTRABAND_DRUG,
                // ↓ 图中文字疑似描述违禁毒品
                CONTRABAND_DRUG_TII
        );
    }

}
