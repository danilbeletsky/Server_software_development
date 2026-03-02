package rbac.system;

import rbac.manager.AssignmentManager;
import rbac.manager.RoleManager;
import rbac.manager.UserManager;
import rbac.model.Assignment;
import rbac.model.Permission;
import rbac.model.Role;
import rbac.model.User;

import java.util.UUID;

public class RBACSystem {
    private final UserManager userManager;
    private final RoleManager roleManager;
    private final AssignmentManager assignmentManager;
    private String currentUser;

    public RBACSystem() {
        this.userManager = new UserManager();
        this.roleManager = new RoleManager();
        this.assignmentManager = new AssignmentManager();
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public RoleManager getRoleManager() {
        return roleManager;
    }

    public AssignmentManager getAssignmentManager() {
        return assignmentManager;
    }

    public void setCurrentUser(String username) {
        this.currentUser = username;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public void initialize() {
        // permissions
        Permission readUsers = new Permission("READ", "USERS", "Read users");
        Permission writeUsers = new Permission("WRITE", "USERS", "Modify users");
        Permission deleteUsers = new Permission("DELETE", "USERS", "Delete users");

        Permission readRoles = new Permission("READ", "ROLES", "Read roles");
        Permission writeRoles = new Permission("WRITE", "ROLES", "Modify roles");
        Permission deleteRoles = new Permission("DELETE", "ROLES", "Delete roles");

        // roles
        Role admin = new Role(UUID.randomUUID().toString(), "Admin", "System administrator");
        admin.addPermission(readUsers);
        admin.addPermission(writeUsers);
        admin.addPermission(deleteUsers);
        admin.addPermission(readRoles);
        admin.addPermission(writeRoles);
        admin.addPermission(deleteRoles);

        Role manager = new Role(UUID.randomUUID().toString(), "Manager", "Manager role");
        manager.addPermission(readUsers);
        manager.addPermission(writeUsers);
        manager.addPermission(readRoles);

        Role viewer = new Role(UUID.randomUUID().toString(), "Viewer", "Read-only user");
        viewer.addPermission(readUsers);
        viewer.addPermission(readRoles);

        roleManager.add(admin);
        roleManager.add(manager);
        roleManager.add(viewer);

        // admin user
        User adminUser = new User("admin", "System Administrator", "admin@company.com");
        userManager.add(adminUser);
        setCurrentUser("admin");

        // assign Admin role to admin
        Assignment assignment = new Assignment(
                adminUser.getUsername(),
                admin.getName(),
                Assignment.Type.PERMANENT,
                "2026-01-01",
                null,
                "Initial admin"
        );
        assignmentManager.add(assignment);
    }

    public String generateStatistics() {
        int userCount = userManager.getAll().size();
        int roleCount = roleManager.getAll().size();
        int totalAssignments = assignmentManager.getAll().size();
        long activeAssignments = assignmentManager.getAll().stream()
                .filter(a -> a.getStatus() == Assignment.Status.ACTIVE)
                .count();
        long expiredAssignments = assignmentManager.getAll().stream()
                .filter(a -> a.getStatus() == Assignment.Status.EXPIRED)
                .count();

        double avgRolesPerUser = userCount == 0
                ? 0.0
                : (double) totalAssignments / userCount;

        StringBuilder sb = new StringBuilder();
        sb.append("System statistics\n");
        sb.append("-----------------\n");
        sb.append("Users: ").append(userCount).append("\n");
        sb.append("Roles: ").append(roleCount).append("\n");
        sb.append("Assignments total: ").append(totalAssignments)
                .append(" (active=").append(activeAssignments)
                .append(", expired=").append(expiredAssignments)
                .append(")\n");
        sb.append("Average roles per user: ")
                .append(String.format("%.2f", avgRolesPerUser))
                .append("\n");
        return sb.toString();
    }
}

