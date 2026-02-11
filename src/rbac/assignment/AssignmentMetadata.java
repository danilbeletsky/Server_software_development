package rbac.assignment;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record AssignmentMetadata(String assignedBy,
                                 String assignedAt,
                                 String reason) {

    public static AssignmentMetadata now(String assignedBy, String reason) {
        String time = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        return new AssignmentMetadata(assignedBy, time, reason);
    }

    public String format() {
        return "Assigned by %s at %s%s"
                .formatted(
                        assignedBy,
                        assignedAt,
                        reason != null ? "\nReason: " + reason : ""
                );
    }
}