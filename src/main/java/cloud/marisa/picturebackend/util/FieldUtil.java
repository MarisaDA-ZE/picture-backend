package cloud.marisa.picturebackend.util;

import cloud.marisa.picturebackend.annotations.MrsFieldName;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

import java.io.Serializable;
import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

/**
 * @author MarisaDAZE
 * @description 数据库字段排序工具类
 * @date 2025/3/31
 */
public class FieldUtil {

    private FieldUtil() {
    }

    /**
     * 根据标记的排序名获取对应实体类字段的 SFunction
     *
     * @param fieldName 排序字段名
     * @param clazz     对应的实体类
     * @param <T>       类型T
     * @return 一个字段的SFunction
     * @deprecated 坏的，用不了，要报错（悲
     */
    public static <T, R> SFunction<T, R> getLambdaField(String fieldName, Class<T> clazz) {
        if (StrUtil.isBlank(fieldName) || clazz == null) {
            throw new IllegalArgumentException("fieldName和clazz不能为空");
        }
        // 获取所有字段（包含父类）
        List<Field> fields = getAllFields(clazz);
        for (Field field : fields) {
            if (field.isAnnotationPresent(MrsFieldName.class)) {
                MrsFieldName annotation = field.getAnnotation(MrsFieldName.class);
                if (fieldName.equals(annotation.value())) {
                    try {
                        Method getter = findGetterMethod(clazz, field);
                        return createSFunction(getter, clazz);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("无法为字段生成SFunction: " + field.getName(), e);
                    }

                }
            }
        }
        throw new IllegalArgumentException("未找到匹配的@MrsFieldSorted字段:" + fieldName);
    }


    /**
     * 获取类及其父类的所有字段
     *
     * @param clazz 对应类
     * @return 所有的字段
     */
    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> currentClass = clazz;
        while (currentClass != null) {
            fields.addAll(Arrays.asList(currentClass.getDeclaredFields()));
            currentClass = currentClass.getSuperclass();
        }
        return fields;
    }


    /**
     * 查找字段的getter方法
     *
     * @param clazz 实体类
     * @param field 字段
     * @param <T>   类型变量T
     * @return getter方法
     */
    private static <T> Method findGetterMethod(Class<T> clazz, Field field) {
        String fieldName = field.getName();
        String getterName = field.getType() == boolean.class ? "is" + capitalize(fieldName) : "get" + capitalize(fieldName);
        try {
            return clazz.getMethod(getterName);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("字段 " + fieldName + " 缺少getter方法: " + getterName, e);
        }
    }

    /**
     * 首字母大写
     *
     * @param str 字符串
     * @return 首字母大写后的字符串
     */
    private static String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * 动态创建SFunction
     *
     * @param method get方法
     * @param clazz  实体类
     * @param <T>    类型T
     * @param <R>    类型R
     * @return SFunction
     */
    private static <T, R> SFunction<T, R> createSFunction(Method method, Class<T> clazz) {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle handle = lookup.unreflect(method);
            MethodType funcType = MethodType.methodType(method.getReturnType(), clazz);
            // 增加Serializable接口支持
            MethodType factoryType = MethodType.methodType(SFunction.class); // 原接口
            MethodType interfaceType = MethodType.methodType(Object.class, Object.class); // SAM签名

            CallSite site = LambdaMetafactory.metafactory(
                    lookup,
                    "apply",
                    factoryType,
                    interfaceType.changeParameterType(0, clazz), // 修正参数类型
                    handle,
                    funcType
            );
            // 强制转换时添加Serializable接口
            return (SFunction<T, R> & Serializable) site.getTarget().invokeExact();
        } catch (Throwable e) {
            throw new RuntimeException("创建SFunction失败", e);
        }
    }
}
