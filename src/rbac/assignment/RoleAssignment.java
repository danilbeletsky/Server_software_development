package rbac.assignment;

import rbac.role.Role;
import rbac.user.User;

public interface RoleAssignment {

    String assignmentId();
    User user();
    Role role();
    AssignmentMetadata metadata();
    boolean isActive();
    String assignmentType();
}
