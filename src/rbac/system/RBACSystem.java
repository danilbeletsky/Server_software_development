package rbac.system;

import rbac.assignment.AssignmentManager;
import rbac.assignment.AssignmentMetadata;
import rbac.assignment.PermanentAssignment;
import rbac.permission.Permission;
import rbac.role.Role;
import rbac.role.RoleManager;
import rbac.user.User;
import rbac.user.UserManager;

import java.util.UUID;

public class RBACSystem {
    private final UserManager userManager;
    private final RoleManager roleManager;
    private final AssignmentManager assignmentManager;
    private String currentUser;

    public RBACSystem() {
        this.userManager = new UserManager();
        this.roleManager = new RoleManager();
        this.assignmentManager = new AssignmentManager(userManager, roleManager);
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
        Role admin = new Role("Admin", "System administrator");
        admin.addPermission(readUsers);
        admin.addPermission(writeUsers);
        admin.addPermission(deleteUsers);
        admin.addPermission(readRoles);
        admin.addPermission(writeRoles);
        admin.addPermission(deleteRoles);

        Role manager = new Role("Manager", "Manager role");
        manager.addPermission(readUsers);
        manager.addPermission(writeUsers);
        manager.addPermission(readRoles);

        Role viewer = new Role("Viewer", "Read-only user");
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
        PermanentAssignment assignment = new PermanentAssignment(
                adminUser,
                admin,
                AssignmentMetadata.now("system", "Initial admin")
        );
        assignmentManager.add(assignment);
    }

    public String generateStatistics() {
        int userCount = userManager.getAll().size();
        int roleCount = roleManager.getAll().size();
        int totalAssignments = assignmentManager.findAll().size();
        long activeAssignments = assignmentManager.findAll().stream()
                .filter(a -> a.isActive())
                .count();
        long expiredAssignments = assignmentManager.findAll().stream()
                .filter(a -> !a.isActive())
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

