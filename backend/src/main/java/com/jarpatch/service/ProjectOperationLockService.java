package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 项目级操作互斥锁服务。
 * <p>
 * 文件保存、结构分析、编译、导出和工作区清理都依赖同一份项目工作区；这些操作并发执行会导致
 * 分析快照不一致、备份与恢复交错，甚至在文件写入期间删除工作区。该服务按项目 ID 提供非阻塞互斥：
 * 拿不到锁立即抛出业务异常，让前端提示"操作执行中"，而不是排队堆积线程。
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
        ReentrantLock lock = acquire(projectId);
        try {
            return operation.execute();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 在项目互斥锁保护下执行只会抛出 I/O 异常的操作。
     * <p>
     * 保存、分析和清理不启动需要等待的子进程，使用该入口可以保留它们原有的异常契约，
     * 同时与编译、导出共享完全相同的项目锁。
     * </p>
     *
     * @param projectId 项目 ID
     * @param operation 待执行的 I/O 操作
     * @param <T>       操作返回类型
     * @return 操作执行结果
     * @throws IOException          操作内的文件读写失败时抛出
     * @throws IllegalStateException 同一项目已有操作正在执行时抛出
     */
    public <T> T runExclusiveIo(String projectId, ExclusiveIoProjectOperation<T> operation) throws IOException {
        ReentrantLock lock = acquire(projectId);
        try {
            return operation.execute();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 非阻塞获取指定项目的互斥锁。
     *
     * @param projectId 项目 ID
     * @return 已成功持有的项目锁
     * @throws IllegalStateException 同一项目已有操作正在执行时抛出
     */
    private ReentrantLock acquire(String projectId) {
        ReentrantLock lock = locks.computeIfAbsent(projectId, key -> new ReentrantLock());
        if (!lock.tryLock()) {
            throw new IllegalStateException(JarPatchConstants.MESSAGE_PROJECT_OPERATION_IN_PROGRESS);
        }
        return lock;
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

    /**
     * 受项目互斥锁保护、只会抛出 I/O 异常的操作契约。
     *
     * @param <T> 操作返回类型
     */
    @FunctionalInterface
    public interface ExclusiveIoProjectOperation<T> {

        /**
         * 执行保存、分析或清理等 I/O 操作。
         *
         * @return 操作结果
         * @throws IOException 文件读写失败时抛出
         */
        T execute() throws IOException;
    }
}
