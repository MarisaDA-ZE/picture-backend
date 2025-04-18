package cloud.marisa.picturebackend.queue;

import java.util.List;

// 数据库存储接口
public interface OverflowStorageDao<T> {
    void save(T task);

    List<T> load();

    T loadOne();

    void delete(List<T> tasks);
}

