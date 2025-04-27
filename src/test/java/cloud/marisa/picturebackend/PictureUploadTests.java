package cloud.marisa.picturebackend;

import cloud.marisa.picturebackend.entity.dao.Picture;
import cloud.marisa.picturebackend.service.IPictureService;
import cloud.marisa.picturebackend.util.colors.ColorUtils;
import cloud.marisa.picturebackend.util.colors.MrsColorHSV;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * @author MarisaDAZE
 * @description SpringRedis集成测试
 * @date 2025/4/2
 */
@SpringBootTest
public class PictureUploadTests {

    @Autowired
    private IPictureService pictureService;

//    @Test
    public void test2() {

    }

//    @Test
    public void addMockPicture() {
        // 插入总条数
        long mockCount = 20_0000L;
        int fragment = 1000;

        long count = 0;
        int maxCount = (int) (mockCount / fragment);
        for (int i = 0; i < (mockCount / fragment); i++) {
            List<Picture> pictures = new ArrayList<>();
            for (int k = 0; k < fragment; k++) {
                Color color = ColorUtils.getRandomColor();
                MrsColorHSV hsv = ColorUtils.toHSV(color);
                Picture picture = getPicture(count, hsv);
                pictures.add(picture);
                count++;
            }
            boolean saved = pictureService.saveBatch(pictures);
            try {
                if (!saved) {
                    System.err.printf("第%d轮保存失败\n", i);
                }
                System.out.printf("第%d/%d轮循环，已添加(%d/%d条数据)\n",
                        i + 1, maxCount, ((i + 1) * fragment), mockCount);
                Thread.sleep(500);
            } catch (InterruptedException e) {
                System.err.println("报错啦: " + e.getMessage());
                System.out.printf("第%d次循环中断，共添加(%d/%d条数据)\n",
                        count, (i * fragment), mockCount);
                return;
            }

        }
    }

    @NotNull
    private static Picture getPicture(long count, MrsColorHSV colorHSV) {
        Color color = colorHSV.getColor();
        String rgbStr = color.getRed() + "," + color.getGreen() + "," + color.getBlue();
        Picture picture = new Picture();
        picture.setName("Mock图片_" + count);
        picture.setPicColor(rgbStr);
        picture.setMColorHue(colorHSV.getHue());
        picture.setMColorSaturation(colorHSV.getSaturation());
        picture.setMColorValue(colorHSV.getValue());
        picture.setMHueBucket(colorHSV.getHueBucket());
        picture.setMSaturationBucket(colorHSV.getSaturationBucket());
        picture.setMValueBucket(colorHSV.getValueBucket());
        picture.setUserId(1905886838348619777L);
        picture.setSpaceId(1905886838348619777L);
        picture.setUrl("http://localhost:9000/mock/picture_" + count);
        picture.setSavedPath("/mock/mock.jpg");
        picture.setReviewStatus(1);
        picture.setReviewMessage("mock数据");
        picture.setReviewTime(new Date());
        return picture;
    }
}
