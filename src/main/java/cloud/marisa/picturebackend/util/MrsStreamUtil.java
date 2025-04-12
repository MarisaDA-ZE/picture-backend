package cloud.marisa.picturebackend.util;

/**
 * @author MarisaDAZE
 * @description 我的流工具类
 * @date 2025/4/11
 */
public class MrsStreamUtil {


    /**
     * 根据文件头字节判断常见图片类型
     */
    public static String determineFileType(byte[] header, int bytesRead) {
        if (bytesRead < 2) return "unknown";

        // 检查 BMP（"BM"）
        if (matchBytes(header, new byte[]{0x42, 0x4D})) { // "BM" 的十六进制
            return "bmp";
        }

        // 检查 JPEG（FF D8 FF）
        if (bytesRead >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF) {
            return "jpg";
        }

        // 检查 PNG（89 50 4E 47 0D 0A 1A 0A）
        if (bytesRead >= 8
                && header[0] == (byte) 0x89
                && header[1] == 0x50
                && header[2] == 0x4E
                && header[3] == 0x47
                && header[4] == 0x0D
                && header[5] == 0x0A
                && header[6] == 0x1A
                && header[7] == 0x0A) {
            return "png";
        }

        // 检查 GIF（"GIF87a" 或 "GIF89a"）
        if (bytesRead >= 6
                && header[0] == 'G'
                && header[1] == 'I'
                && header[2] == 'F'
                && header[3] == '8'
                && (header[4] == '7' || header[4] == '9')
                && header[5] == 'a') {
            return "gif";
        }

        return "unknown";
    }

    /**
     * 检查字节数组是否匹配指定模式
     */
    private static boolean matchBytes(byte[] actual, byte[] expected) {
        if (actual.length < expected.length) return false;
        for (int i = 0; i < expected.length; i++) {
            if (actual[i] != expected[i]) return false;
        }
        return true;
    }
}
