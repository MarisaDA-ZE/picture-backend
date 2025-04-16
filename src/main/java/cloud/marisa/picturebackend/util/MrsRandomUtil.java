package cloud.marisa.picturebackend.util;

/**
 * @author MarisaDAZE
 * @description MrsRandomUtil.类
 * @date 2025/3/28
 */
public class MrsRandomUtil {

    /**
     * 获取一个[x,y]区间的随机数
     *
     * @param min 最小值
     * @param max 最大值
     * @return 随机值
     */
    public static int getRandomLong(int min, int max) {
        return (int) (Math.random() * (max - min + 1) + min);
    }

    public static String getRandomString(int length) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * str.length());
            char c = str.charAt(index);
            sb.append(c);
        }
        return sb.toString();
    }
}
