package rbac.assignment;

import rbac.role.Role;
import rbac.user.User;

import java.time.LocalDate;

public interface RoleAssignment {

    String assignmentId();
    User user();
    Role role();
    AssignmentMetadata metadata();
    boolean isActive();
    String assignmentType();

    default User getUser() { return user(); }
    default Role getRole() { return role(); }
    default String getAssignedBy() { return metadata() != null ? metadata().assignedBy() : null; }
    default LocalDate getAssignmentDate() {
        if (metadata() == null || metadata().assignedAt() == null) return null;
        try {
            String s = metadata().assignedAt();
            return s.length() >= 10 ? LocalDate.parse(s.substring(0, 10)) : null;
        } catch (Exception e) { return null; }
    }
    default LocalDate getExpirationDate() { return null; }
    default AssignmentType getType() {
        return "TEMPORARY".equals(assignmentType()) ? AssignmentType.TEMPORARY : AssignmentType.PERMANENT;
    }
    default String getId() { return assignmentId(); }
    default String getAssignmentId() { return assignmentId(); }
    default String getUsername() { return getUser() != null ? getUser().getUsername() : null; }
    default String getRoleName() { return getRole() != null ? getRole().getName() : null; }
    default String getStatus() { return isActive() ? "ACTIVE" : "INACTIVE"; }
    default String getAssignedAt() { return metadata() != null ? metadata().assignedAt() : null; }
    default String getExpiresAt() { return null; }
    default void revoke() {}
    default void setExpirationDate(java.time.LocalDate date) {}
}
