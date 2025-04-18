package cloud.marisa.picturebackend.queue;

import cloud.marisa.picturebackend.entity.dao.Picture;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 图片溢出时如何保存
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class DatabaseOverflowStorage implements OverflowStorageDao<Picture> {

    /**
     * RedisTemplate
     * <p>将数据存储到Redis中</p>
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 缓存图片对象的键前缀
     */
    private static final String REVIEW_PREFIX = "review-queue:picture";

    /**
     * 缓存图片ID列表的键前缀
     */
    private static final String ID_LIST_KEY = REVIEW_PREFIX + ":id-list";
    private static final String PROCESSING = "-processing";

    @Override
    public void save(Picture picture) {
        Long pid = picture.getId();
        String key = REVIEW_PREFIX + ":" + pid;
        redisTemplate.opsForValue().set(key, picture);
        log.info("任务保存操作，保存了: {}", key);
        // 两个redisTemplate是不一样的
        // 一个存储的是对象，另一个存储的是id列表
        Object o = redisTemplate.opsForValue().get(ID_LIST_KEY);

        List<String> ids = new ArrayList<>();
        if (ObjUtil.isNotNull(o) && JSONUtil.isTypeJSONArray((String) o)) {
            ids = JSONUtil.toList((String) o, String.class);
        }
        ids.add(String.valueOf(pid));
        String str = JSONUtil.toJsonStr(ids);
        redisTemplate.opsForValue().set(ID_LIST_KEY, str);
        log.info("保存后的ID列表: {}", str);
    }

    @Override
    public List<Picture> load() {
        // 实际应根据状态查询未处理任务
        Object json = redisTemplate.opsForValue().get(ID_LIST_KEY);
        // id列表的json为空，说明没有任务处理
        if (ObjUtil.isNull(json) || !JSONUtil.isTypeJSONArray((String) json)) {
            return new ArrayList<>();
        }
        // id列表为空，说明没有任务处理
        List<Long> collect = JSONUtil.toList((String) json, Long.class);
        if (collect == null || collect.isEmpty()) {
            return new ArrayList<>();
        }
        // 将ID和之前存储的时候的前缀拼接成完整的Key
        Set<String> keys = collect.stream()
                .map(id -> {
                    if (id != null) {
                        return REVIEW_PREFIX + ":" + id;
                    }
                    return null;
                })
                .filter(ObjUtil::isNotNull)
                .collect(Collectors.toSet());
        // 根据键列表获取对应的值
        log.info("keys: {}", keys);
        List<Object> objects = redisTemplate.opsForValue().multiGet(keys);
        if (objects == null || objects.isEmpty()) {
            return new ArrayList<>();
        }
        return objects.stream()
                .map(o -> {
                    String str = JSONUtil.toJsonStr(o);
                    return JSONUtil.toBean(str, Picture.class);
                })
                .collect(Collectors.toList());
    }

    @Override
    public Picture loadOne() {
        // 实际应根据状态查询未处理任务
        Object json = redisTemplate.opsForValue().get(ID_LIST_KEY);
        // id列表的json为空，说明没有任务处理
        log.info("ID列表的JSON {}", json);
        if (ObjUtil.isNull(json) || !JSONUtil.isTypeJSONArray((String) json)) {
            return null;
        }
        // id列表为空，说明没有任务处理
        List<String> collect = JSONUtil.toList((String) json, String.class);
        if (collect == null || collect.isEmpty()) {
            return null;
        }
        String pid = null;
        for (String id : collect) {
            // 取第一个不是空且不是正在处理的图片ID
            if (id != null && !id.endsWith(PROCESSING)) {
                pid = id;
                break;
            }
        }
        if (pid == null) {
            log.info("任务队列中的所有图片均在处理中...");
            return null;
        }
        String key = REVIEW_PREFIX + ":" + pid;
        log.info("将要处理: {}", pid);
        Object object = redisTemplate.opsForValue().get(key);
        collect.remove(pid);
        if (object == null) {
            String jsonStr = JSONUtil.toJsonStr(collect);
            redisTemplate.opsForValue().set(ID_LIST_KEY, jsonStr);
            return null;
        }
        collect.add(pid + PROCESSING);
        String jsonStr = JSONUtil.toJsonStr(collect);
        log.info("更新ID列表: {}", jsonStr);
        redisTemplate.opsForValue().set(ID_LIST_KEY, jsonStr);
        // 返回图片对象
        String str = JSONUtil.toJsonStr(object);
        return JSONUtil.toBean(str, Picture.class);
    }

    @Override
    public void delete(List<Picture> pictures) {
        Set<String> keys = pictures.stream()
                .map(p -> REVIEW_PREFIX + ":" + p.getId())
                .collect(Collectors.toSet());
        log.info("需要删除: {}", keys);
        redisTemplate.delete(keys);
        // 删除key
        Set<String> idSet = pictures.stream()
                .map(Picture::getId)
                .map(String::valueOf)
                .collect(Collectors.toSet());
        // 取出ID列表
        Object json = redisTemplate.opsForValue().get(ID_LIST_KEY);
        List<String> ids = new ArrayList<>();
        if (ObjUtil.isNotNull(json) && JSONUtil.isTypeJSONArray((String) json)) {
            ids = JSONUtil.toList((String) json, String.class);
        }
        if (!ids.isEmpty()) {
            Set<String> processSet = idSet.stream()
                    .map(id -> id + PROCESSING)
                    .collect(Collectors.toSet());
            // 直接删除键
            ids.removeAll(idSet);
            // 它们也可能是正在处理的图片
            ids.removeAll(processSet);
        }
        // 删除后列表为空了，那就直接删除整个对象
        if (ids.isEmpty()) {
            log.info("键列表整体删除了: {}", ID_LIST_KEY);
            redisTemplate.delete(ID_LIST_KEY);
            return;
        }
        log.info("删除前的ID列表: {}", json);
        // 不然就转成JSON覆盖保存
        String jsonStr = JSONUtil.toJsonStr(ids);
        log.info("删除后的ID列表: {}", jsonStr);
        redisTemplate.opsForValue().set(ID_LIST_KEY, jsonStr);
    }
}
