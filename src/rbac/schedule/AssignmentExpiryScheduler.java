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

/**
 * Периодическая деактивация истёкших назначений (ветка feature/schedule-tasks).
 */
public final class AssignmentExpiryScheduler implements AutoCloseable {
    private final ScheduledExecutorService scheduler;
    private final AssignmentManager assignments;
    private final UserManager users;
    private final RoleManager roles;
    private final QueuedAuditLog audit;
    private ScheduledFuture<?> future;

    public AssignmentExpiryScheduler(
            AssignmentManager assignments,
            UserManager users,
            RoleManager roles,
            QueuedAuditLog audit
    ) {
        this.assignments = assignments;
        this.users = users;
        this.roles = roles;
        this.audit = audit;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "assignment-expiry");
            t.setDaemon(true);
            return t;
        });
    }

    public void startEverySeconds(long periodSeconds) {
        if (future != null) {
            future.cancel(false);
        }
        future = scheduler.scheduleAtFixedRate(this::tick, periodSeconds, periodSeconds, TimeUnit.SECONDS);
    }

    private void tick() {
        try {
            Instant now = Instant.now();
            int deactivated = assignments.deactivateExpired(now);
            int u = users.size();
            int r = roles.size();
            int a = assignments.size();
            audit.log("INFO", "Планировщик: деактивировано истёкших назначений: " + deactivated
                    + "; статистика: пользователей=" + u + ", ролей=" + r + ", назначений=" + a);
        } catch (RuntimeException e) {
            audit.log("ERROR", "Планировщик: " + e.getMessage());
        }
    }

    @Override
    public void close() throws InterruptedException {
        if (future != null) {
            future.cancel(false);
        }
        scheduler.shutdown();
        if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
            scheduler.shutdownNow();
        }
    }
}
