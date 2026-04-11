package rbac.schedule;

import rbac.managers.RbacManagers.AssignmentManager;
import rbac.managers.RbacManagers.RoleManager;
import rbac.managers.RbacManagers.UserManager;
import rbac.worker.RbacWorkers.QueuedAuditLog;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Периодически вызывает {@link AssignmentManager#deactivateExpired(Instant)} и пишет в аудит.
 */
public final class AssignmentExpiryScheduler implements AutoCloseable {

    private final AssignmentManager assignments;
    private final UserManager users;
    private final RoleManager roles;
    private final QueuedAuditLog audit;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "assignment-expiry");
        t.setDaemon(true);
        return t;
    });
    private final AtomicReference<ScheduledFuture<?>> future = new AtomicReference<>();

    public AssignmentExpiryScheduler(
            AssignmentManager assignments,
            UserManager users,
            RoleManager roles,
            QueuedAuditLog audit) {
        this.assignments = assignments;
        this.users = users;
        this.roles = roles;
        this.audit = audit;
    }

    public void start(long interval, TimeUnit unit) {
        future.updateAndGet(prev -> {
            if (prev != null) {
                prev.cancel(false);
            }
            return scheduler.scheduleAtFixedRate(this::runOnce, interval, interval, unit);
        });
    }

    private void runOnce() {
        try {
            int n = assignments.deactivateExpired(Instant.now());
            if (n > 0) {
                audit.log("INFO",
                        "Deactivated " + n + " expired assignment(s); users=" + users.size() + " roles=" + roles.size());
            }
        } catch (Exception e) {
            audit.log("ERROR", "Assignment expiry pass failed: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        ScheduledFuture<?> f = future.getAndSet(null);
        if (f != null) {
            f.cancel(false);
        }
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            scheduler.shutdownNow();
        }
    }
}
