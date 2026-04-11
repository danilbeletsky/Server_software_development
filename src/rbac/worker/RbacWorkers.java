package com.mileshko.rbac.worker;

import com.mileshko.rbac.model.AuditLogEntry;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Фоновый executor и аудит через очередь (ветка feature/workers).
 */
public final class RbacWorkers {

    private RbacWorkers() {
    }

    public static final class BackgroundExecutor implements AutoCloseable {
        private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "rbac-background");
            t.setDaemon(true);
            return t;
        });

        public void execute(Runnable command) {
            executor.execute(command);
        }

        public <T> CompletableFuture<T> submit(Callable<T> task) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return task.call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executor);
        }

        @Override
        public void close() throws InterruptedException {
            executor.shutdown();
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        }
    }

    public static final class QueuedAuditLog implements AutoCloseable {
        private final BlockingQueue<AuditLogEntry> queue = new LinkedBlockingQueue<>();
        private final CopyOnWriteArrayList<AuditLogEntry> entries = new CopyOnWriteArrayList<>();
        private final ExecutorService writer;
        private final AtomicBoolean running = new AtomicBoolean(true);

        public QueuedAuditLog() {
            this.writer = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "audit-log-writer");
                t.setDaemon(true);
                return t;
            });
            writer.submit(this::consumeLoop);
        }

        private void consumeLoop() {
            while (running.get() || !queue.isEmpty()) {
                try {
                    AuditLogEntry e = queue.poll(200, TimeUnit.MILLISECONDS);
                    if (e != null) {
                        entries.add(e);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        public void log(String level, String message) {
            if (!running.get()) {
                return;
            }
            queue.offer(new AuditLogEntry(Instant.now(), level, message));
        }

        public List<AuditLogEntry> snapshot() {
            return List.copyOf(entries);
        }

        public int size() {
            return entries.size();
        }

        @Override
        public void close() throws InterruptedException {
            running.set(false);
            writer.shutdown();
            if (!writer.awaitTermination(5, TimeUnit.SECONDS)) {
                writer.shutdownNow();
            }
        }
    }
}
