package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 项目级长操作互斥锁服务。
 * <p>
 * 编译和导出都会备份、写回工作区文件并替换 SQLite 产物记录，同一项目并发执行会导致
 * 备份与恢复交错、产物清单错乱。该服务按项目 ID 提供非阻塞互斥：拿不到锁立即抛出
 * 业务异常，让前端提示"操作执行中"，而不是排队堆积线程。锁只在长操作入口使用，
 * 文件保存等快速操作不受影响。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class ProjectOperationLockService {

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    /**
     * 在项目互斥锁保护下执行长操作；同一项目已有操作未结束时直接失败。
     *
     * @param projectId 项目 ID
     * @param operation 待执行的长操作
     * @param <T>       操作返回类型
     * @return 操作执行结果
     * @throws IOException          操作内的文件读写失败时抛出
     * @throws InterruptedException 操作内的子进程等待被中断时抛出
     * @throws IllegalStateException 同一项目已有操作正在执行时抛出
     */
    public <T> T runExclusive(String projectId, ExclusiveProjectOperation<T> operation)
            throws IOException, InterruptedException {
        ReentrantLock lock = locks.computeIfAbsent(projectId, key -> new ReentrantLock());
        if (!lock.tryLock()) {
            throw new IllegalStateException(JarPatchConstants.MESSAGE_PROJECT_OPERATION_IN_PROGRESS);
        }
        try {
            return operation.execute();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 受项目互斥锁保护的长操作契约。
     *
     * @param <T> 操作返回类型
     */
    @FunctionalInterface
    public interface ExclusiveProjectOperation<T> {

        /**
         * 执行长操作。
         *
         * @return 操作结果
         * @throws IOException          文件读写失败时抛出
         * @throws InterruptedException 子进程等待被中断时抛出
         */
        T execute() throws IOException, InterruptedException;
    }
}
