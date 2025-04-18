package cloud.marisa.picturebackend.queue;

import cloud.marisa.picturebackend.entity.dao.Picture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
@RequiredArgsConstructor
public class PersistentFallbackQueue {

    // 主队列（内存）
    private final BlockingQueue<Picture> memoryQueue = new LinkedBlockingQueue<>(100);

    // 溢出存储（此处以数据库为例，可替换为文件存储）
    private final OverflowStorageDao<Picture> overflowStorage;


    /**
     * 添加任务：优先入内存队列，满时转存溢出存储
     */
    public boolean add(Picture task) {
        if (memoryQueue.offer(task)) {
            return true;
        } else {
            // 溢出时持久化
            overflowStorage.save(task);
            return false;
        }
    }

    /**
     * 消费任务：优先从内存队列获取，空时加载溢出存储
     */
    public Picture take() throws InterruptedException {
        Picture task = memoryQueue.poll();
        if (task == null) {
            // 从溢出存储加载一批任务到内存队列
            List<Picture> tasks = overflowStorage.load();
            if (!tasks.isEmpty()) {
                memoryQueue.addAll(tasks);
                overflowStorage.delete(tasks); // 加载后删除已恢复的任务
                task = memoryQueue.take();
            }
        }
        return task;
    }
}