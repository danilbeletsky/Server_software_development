package rbac.audit;

import java.time.Instant;

/** Запись для асинхронного аудита (очередь в {@code rbac.worker.RbacWorkers}). */
public record AuditLogEntry(Instant timestamp, String level, String message) {
}
