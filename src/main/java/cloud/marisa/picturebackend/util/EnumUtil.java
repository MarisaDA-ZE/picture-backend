package cloud.marisa.picturebackend.util;

import cloud.marisa.picturebackend.enums.MrsBaseEnum;
import org.springframework.util.ObjectUtils;

/**
 * @author MarisaDAZE
 * @description 枚举工具类
 * @date 2025/3/28
 */
public class EnumUtil {

    private EnumUtil() {
    }

    /**
     * 根据枚举值尝试获取枚举对象
     * <p> 值 -> 枚举 </p>
     *
     * @param value 枚举值
     * @param clazz 期待的枚举类
     * @param <E>   E
     * @return 具体的枚举对象
     */
    public static <T, E extends MrsBaseEnum<T>> E fromValue(T value, Class<E> clazz) {
        if (ObjectUtils.isEmpty(value) || ObjectUtils.isEmpty(clazz)) {
            return null;
        }
        if (!clazz.isEnum()) {
            throw new IllegalArgumentException("clazz must be an enum type.");
        }
        // 这里遍历的是一个枚举类中的所有枚举对象
        // 一般来说一个枚举类中不会有太多枚举对象，因此效率上应该没问题
        for (E item : clazz.getEnumConstants()) {
            T val = item.getValue();
            if (val.equals(value)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 根据枚举值尝试获取枚举对象
     * <p>非空方法</p>
     * <p> 值 -> 枚举 </p>
     *
     * @param value        枚举值的字符串形式
     * @param defaultValue 如果不存在时的默认值
     * @param <E>          E
     * @return 具体的枚举对象
     */
    public static <T, E extends MrsBaseEnum<T>> E fromValueNotNull(T value, E defaultValue) {
        Class<?> aClass = defaultValue.getClass();
        Class<E> clazz = (Class<E>) defaultValue.getClass();
        E res = fromValue(value, clazz);
        if (res == null) {
            return defaultValue;
        }
        return res;
    }

    /**
     * 值是否在某个枚举类中
     *
     * @param value 要判断的值
     * @param clazz 待查找的枚举类
     * @param <T>   类型T
     * @return true:是该枚举的属性值，false:不是该枚举的属性值
     */
    public static <T> boolean hasEnumValue(T value, Class<? extends MrsBaseEnum<T>> clazz) {
        if (ObjectUtils.isEmpty(value) || ObjectUtils.isEmpty(clazz)) return false;
        if (!clazz.isEnum()) {
            throw new IllegalArgumentException("clazz must be an enum type.");
        }
        for (MrsBaseEnum<T> item : clazz.getEnumConstants()) {
            if (item.getValue() == value) {
                return true;
            }
        }
        return false;
    }
}
