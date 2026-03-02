package rbac.core;

import rbac.manager.AssignmentManager;
import rbac.manager.RoleManager;
import rbac.manager.UserManager;
import rbac.model.Assignment;
import rbac.model.Permission;
import rbac.model.Role;
import rbac.model.User;
import rbac.system.RBACSystem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;

public class CommandRegistry {

    public static void registerAll(CommandParser parser) {
        registerUserCommands(parser);
        registerRoleCommands(parser);
        registerAssignmentCommands(parser);
        registerPermissionCommands(parser);
        registerServiceCommands(parser);
    }

    private static void registerUserCommands(CommandParser parser) {
        parser.registerCommand("user-list", "List users", (scanner, system) -> {
            UserManager um = system.getUserManager();
            System.out.println("Users:");
            um.getAll().stream()
                    .sorted(Comparator.comparing(User::getUsername))
                    .forEach(u -> System.out.printf("- %s (%s, %s)%n",
                            u.getUsername(), u.getFullName(), u.getEmail()));
        });

        parser.registerCommand("user-create", "Create user", (scanner, system) -> {
            UserManager um = system.getUserManager();
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            System.out.print("Full name: ");
            String fullName = scanner.nextLine().trim();
            System.out.print("Email: ");
            String email = scanner.nextLine().trim();
            if (username.isEmpty() || fullName.isEmpty() || email.isEmpty()) {
                System.out.println("All fields are required.");
                return;
            }
            try {
                um.add(new User(username, fullName, email));
                System.out.println("User created.");
            } catch (IllegalArgumentException ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        });

        parser.registerCommand("user-view", "View user", (scanner, system) -> {
            UserManager um = system.getUserManager();
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            Optional<User> userOpt = um.findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("User not found.");
                return;
            }
            User user = userOpt.get();
            System.out.printf("Username: %s%nFull name: %s%nEmail: %s%n",
                    user.getUsername(), user.getFullName(), user.getEmail());

            AssignmentManager am = system.getAssignmentManager();
            RoleManager rm = system.getRoleManager();
            List<Assignment> assignments = am.getByUser(username);
            System.out.println("Roles:");
            assignments.forEach(a -> System.out.printf("- %s (%s)%n", a.getRoleName(), a.getStatus()));
            System.out.println("Permissions:");
            am.getUserPermissions(username, rm.getAll()).forEach((resource, perms) -> {
                System.out.println("  " + resource + ":");
                perms.forEach(p -> System.out.println("    - " + p.getName()));
            });
        });

        parser.registerCommand("user-update", "Update user", (scanner, system) -> {
            UserManager um = system.getUserManager();
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            System.out.print("New full name: ");
            String fullName = scanner.nextLine().trim();
            System.out.print("New email: ");
            String email = scanner.nextLine().trim();
            try {
                um.update(username, fullName, email);
                System.out.println("User updated.");
            } catch (Exception ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        });

        parser.registerCommand("user-delete", "Delete user", (scanner, system) -> {
            UserManager um = system.getUserManager();
            AssignmentManager am = system.getAssignmentManager();
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            System.out.print("Type 'да' to confirm: ");
            String confirm = scanner.nextLine().trim();
            if (!"да".equalsIgnoreCase(confirm)) {
                System.out.println("Cancelled.");
                return;
            }
            am.deleteAssignmentsForUser(username);
            um.delete(username);
            System.out.println("User and assignments deleted.");
        });

        parser.registerCommand("user-search", "Search users", (scanner, system) -> {
            UserManager um = system.getUserManager();
            System.out.println("Choose filter:");
            System.out.println("1 - username contains");
            System.out.println("2 - email contains");
            System.out.println("3 - email domain");
            System.out.println("4 - full name contains");
            String choice = scanner.nextLine().trim();
            List<User> result = new ArrayList<>();
            switch (choice) {
                case "1" -> {
                    System.out.print("Part of username: ");
                    String part = scanner.nextLine().trim();
                    result = um.searchByUsernameContains(part);
                }
                case "2" -> {
                    System.out.print("Part of email: ");
                    String part = scanner.nextLine().trim();
                    result = um.searchByEmailContains(part);
                }
                case "3" -> {
                    System.out.print("Email domain (example.com): ");
                    String domain = scanner.nextLine().trim();
                    result = um.searchByEmailDomain(domain);
                }
                case "4" -> {
                    System.out.print("Part of full name: ");
                    String part = scanner.nextLine().trim();
                    result = um.searchByFullNameContains(part);
                }
                default -> System.out.println("Unknown option");
            }
            if (!result.isEmpty()) {
                result.forEach(u -> System.out.printf("- %s (%s, %s)%n",
                        u.getUsername(), u.getFullName(), u.getEmail()));
            }
        });
    }

    private static void registerRoleCommands(CommandParser parser) {
        parser.registerCommand("role-list", "List roles", (scanner, system) -> {
            RoleManager rm = system.getRoleManager();
            System.out.println("Roles:");
            rm.getAll().forEach(r -> System.out.printf("- %s (id=%s, permissions=%d)%n",
                    r.getName(), r.getId(), r.getPermissions().size()));
        });

        parser.registerCommand("role-create", "Create role", (scanner, system) -> {
            RoleManager rm = system.getRoleManager();
            System.out.print("Role name: ");
            String name = scanner.nextLine().trim();
            System.out.print("Description: ");
            String description = scanner.nextLine().trim();
            Role role = new Role(UUID.randomUUID().toString(), name, description);
            rm.add(role);
            System.out.println("Role created. Add permissions (empty name to stop).");
            while (true) {
                System.out.print("Permission name (empty to finish): ");
                String permName = scanner.nextLine().trim();
                if (permName.isEmpty()) {
                    break;
                }
                System.out.print("Resource: ");
                String resource = scanner.nextLine().trim();
                System.out.print("Description: ");
                String permDesc = scanner.nextLine().trim();
                rm.addPermissionToRole(role, new Permission(permName, resource, permDesc));
            }
        });

        parser.registerCommand("role-view", "View role", (scanner, system) -> {
            RoleManager rm = system.getRoleManager();
            System.out.print("Role name: ");
            String name = scanner.nextLine().trim();
            Optional<Role> role = rm.findByName(name);
            if (role.isEmpty()) {
                System.out.println("Role not found.");
                return;
            }
            System.out.println(role.get().format());
        });

        parser.registerCommand("role-update", "Update role", (scanner, system) -> {
            RoleManager rm = system.getRoleManager();
            System.out.print("Role name: ");
            String name = scanner.nextLine().trim();
            Optional<Role> roleOpt = rm.findByName(name);
            if (roleOpt.isEmpty()) {
                System.out.println("Role not found.");
                return;
            }
            Role role = roleOpt.get();
            System.out.print("New name (blank to keep): ");
            String newName = scanner.nextLine().trim();
            if (!newName.isEmpty()) {
                role.setName(newName);
            }
            System.out.print("New description (blank to keep): ");
            String newDesc = scanner.nextLine().trim();
            if (!newDesc.isEmpty()) {
                role.setDescription(newDesc);
            }
            rm.update(role);
            System.out.println("Role updated.");
        });

        parser.registerCommand("role-delete", "Delete role", (scanner, system) -> {
            RoleManager rm = system.getRoleManager();
            AssignmentManager am = system.getAssignmentManager();
            System.out.print("Role name: ");
            String name = scanner.nextLine().trim();
            Optional<Role> roleOpt = rm.findByName(name);
            if (roleOpt.isEmpty()) {
                System.out.println("Role not found.");
                return;
            }
            Role role = roleOpt.get();
            List<Assignment> assignments = am.getByRole(role.getName());
            if (!assignments.isEmpty()) {
                System.out.println("Role is assigned to users:");
                assignments.forEach(a -> System.out.println(" - " + a.getUsername()));
                System.out.print("Delete anyway? (yes/no): ");
                String ans = scanner.nextLine().trim();
                if (!"yes".equalsIgnoreCase(ans)) {
                    System.out.println("Cancelled.");
                    return;
                }
            }
            rm.delete(role);
            System.out.println("Role deleted.");
        });

        parser.registerCommand("role-add-permission", "Add permission to role", (scanner, system) -> {
            RoleManager rm = system.getRoleManager();
            System.out.print("Role name: ");
            String name = scanner.nextLine().trim();
            Optional<Role> roleOpt = rm.findByName(name);
            if (roleOpt.isEmpty()) {
                System.out.println("Role not found.");
                return;
            }
            Role role = roleOpt.get();
            System.out.print("Permission name: ");
            String permName = scanner.nextLine().trim();
            System.out.print("Resource: ");
            String resource = scanner.nextLine().trim();
            System.out.print("Description: ");
            String permDesc = scanner.nextLine().trim();
            rm.addPermissionToRole(role, new Permission(permName, resource, permDesc));
            System.out.println("Permission added.");
        });

        parser.registerCommand("role-remove-permission", "Remove permission from role", (scanner, system) -> {
            RoleManager rm = system.getRoleManager();
            System.out.print("Role name: ");
            String name = scanner.nextLine().trim();
            Optional<Role> roleOpt = rm.findByName(name);
            if (roleOpt.isEmpty()) {
                System.out.println("Role not found.");
                return;
            }
            Role role = roleOpt.get();
            List<Permission> perms = new ArrayList<>(role.getPermissions());
            for (int i = 0; i < perms.size(); i++) {
                System.out.printf("%d) %s on %s%n", i + 1, perms.get(i).getName(), perms.get(i).getResource());
            }
            System.out.print("Number to remove: ");
            String idxStr = scanner.nextLine().trim();
            int idx = Integer.parseInt(idxStr) - 1;
            if (idx < 0 || idx >= perms.size()) {
                System.out.println("Invalid index.");
                return;
            }
            role.removePermission(idx);
            System.out.println("Permission removed.");
        });

        parser.registerCommand("role-search", "Search roles", (scanner, system) -> {
            RoleManager rm = system.getRoleManager();
            System.out.println("Choose filter:");
            System.out.println("1 - name contains");
            System.out.println("2 - has permission");
            System.out.println("3 - min permissions count");
            String choice = scanner.nextLine().trim();
            List<Role> result = new ArrayList<>();
            switch (choice) {
                case "1" -> {
                    System.out.print("Part of name: ");
                    String part = scanner.nextLine().trim();
                    result = rm.searchByNameContains(part);
                }
                case "2" -> {
                    System.out.print("Permission name: ");
                    String permName = scanner.nextLine().trim();
                    result = rm.searchByPermission(permName);
                }
                case "3" -> {
                    System.out.print("Min permissions: ");
                    int min = Integer.parseInt(scanner.nextLine().trim());
                    result = rm.searchByMinPermissions(min);
                }
                default -> System.out.println("Unknown option");
            }
            result.forEach(r -> System.out.printf("- %s (perms=%d)%n",
                    r.getName(), r.getPermissions().size()));
        });
    }

    private static void registerAssignmentCommands(CommandParser parser) {
        parser.registerCommand("assign-role", "Assign role to user", (scanner, system) -> {
            UserManager um = system.getUserManager();
            RoleManager rm = system.getRoleManager();
            AssignmentManager am = system.getAssignmentManager();

            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            if (um.findByUsername(username).isEmpty()) {
                System.out.println("User not found.");
                return;
            }

            List<Role> roles = new ArrayList<>(rm.getAll());
            for (int i = 0; i < roles.size(); i++) {
                System.out.printf("%d) %s%n", i + 1, roles.get(i).getName());
            }
            System.out.print("Choose role number: ");
            int idx = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (idx < 0 || idx >= roles.size()) {
                System.out.println("Invalid index.");
                return;
            }
            Role role = roles.get(idx);

            System.out.print("Type (1-permanent, 2-temporary): ");
            String typeStr = scanner.nextLine().trim();
            Assignment.Type type = "2".equals(typeStr)
                    ? Assignment.Type.TEMPORARY
                    : Assignment.Type.PERMANENT;
            String expiresAt = null;
            if (type == Assignment.Type.TEMPORARY) {
                System.out.print("Expires at (YYYY-MM-DD): ");
                expiresAt = scanner.nextLine().trim();
            }
            System.out.print("Reason: ");
            String reason = scanner.nextLine().trim();

            Assignment a = new Assignment(username, role.getName(), type,
                    "2026-01-01", expiresAt, reason);
            am.add(a);
            System.out.println("Role assigned.");
        });

        parser.registerCommand("revoke-role", "Revoke role", (scanner, system) -> {
            AssignmentManager am = system.getAssignmentManager();
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            List<Assignment> list = am.getByUser(username);
            for (int i = 0; i < list.size(); i++) {
                Assignment a = list.get(i);
                System.out.printf("%d) %s (%s)%n", i + 1, a.getRoleName(), a.getStatus());
            }
            System.out.print("Choose assignment number: ");
            int idx = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (idx < 0 || idx >= list.size()) {
                System.out.println("Invalid index.");
                return;
            }
            Assignment a = list.get(idx);
            am.revoke(a);
            System.out.println("Assignment revoked.");
        });

        parser.registerCommand("assignment-list", "List assignments", (scanner, system) -> {
            AssignmentManager am = system.getAssignmentManager();
            System.out.println("Assignments:");
            am.getAll().forEach(a -> System.out.printf(
                    "%s: user=%s, role=%s, type=%s, status=%s, at=%s, expires=%s%n",
                    a.getId(), a.getUsername(), a.getRoleName(),
                    a.getType(), a.getStatus(), a.getAssignedAt(), a.getExpiresAt()));
        });

        parser.registerCommand("assignment-list-user", "Assignments of user", (scanner, system) -> {
            AssignmentManager am = system.getAssignmentManager();
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            am.getByUser(username).forEach(a -> System.out.printf(
                    "%s: role=%s, type=%s, status=%s, at=%s, expires=%s%n",
                    a.getId(), a.getRoleName(), a.getType(),
                    a.getStatus(), a.getAssignedAt(), a.getExpiresAt()));
        });

        parser.registerCommand("assignment-list-role", "Assignments for role", (scanner, system) -> {
            AssignmentManager am = system.getAssignmentManager();
            System.out.print("Role name: ");
            String role = scanner.nextLine().trim();
            am.getByRole(role).forEach(a -> System.out.printf(
                    "%s: user=%s, type=%s, status=%s, at=%s, expires=%s%n",
                    a.getId(), a.getUsername(), a.getType(),
                    a.getStatus(), a.getAssignedAt(), a.getExpiresAt()));
        });

        parser.registerCommand("assignment-active", "Active assignments", (scanner, system) -> {
            AssignmentManager am = system.getAssignmentManager();
            am.getActive().forEach(a -> System.out.printf(
                    "%s: user=%s, role=%s, type=%s, at=%s, expires=%s%n",
                    a.getId(), a.getUsername(), a.getRoleName(),
                    a.getType(), a.getAssignedAt(), a.getExpiresAt()));
        });

        parser.registerCommand("assignment-expired", "Expired assignments", (scanner, system) -> {
            AssignmentManager am = system.getAssignmentManager();
            am.getExpired().forEach(a -> System.out.printf(
                    "%s: user=%s, role=%s, at=%s, expired=%s%n",
                    a.getId(), a.getUsername(), a.getRoleName(),
                    a.getAssignedAt(), a.getExpiresAt()));
        });

        parser.registerCommand("assignment-extend", "Extend temporary assignment", (scanner, system) -> {
            AssignmentManager am = system.getAssignmentManager();
            System.out.print("Assignment ID: ");
            String id = scanner.nextLine().trim();
            Optional<Assignment> opt = am.findById(id);
            if (opt.isEmpty()) {
                System.out.println("Assignment not found.");
                return;
            }
            Assignment a = opt.get();
            System.out.print("New expires at (YYYY-MM-DD): ");
            String date = scanner.nextLine().trim();
            a.setExpiresAt(date);
            System.out.println("Extended.");
        });

        parser.registerCommand("assignment-search", "Search assignments", (scanner, system) -> {
            AssignmentManager am = system.getAssignmentManager();
            System.out.println("Choose filter:");
            System.out.println("1 - by user");
            System.out.println("2 - by role");
            System.out.println("3 - by type");
            System.out.println("4 - by status");
            String choice = scanner.nextLine().trim();
            List<Assignment> result = new ArrayList<>();
            switch (choice) {
                case "1" -> {
                    System.out.print("Username: ");
                    String username = scanner.nextLine().trim();
                    result = am.getByUser(username);
                }
                case "2" -> {
                    System.out.print("Role name: ");
                    String role = scanner.nextLine().trim();
                    result = am.getByRole(role);
                }
                case "3" -> {
                    System.out.print("Type (PERMANENT/TEMPORARY): ");
                    String t = scanner.nextLine().trim().toUpperCase();
                    result = am.getAll().stream()
                            .filter(a -> a.getType().name().equals(t))
                            .toList();
                }
                case "4" -> {
                    System.out.print("Status (ACTIVE/EXPIRED/REVOKED): ");
                    String s = scanner.nextLine().trim().toUpperCase();
                    result = am.getAll().stream()
                            .filter(a -> a.getStatus().name().equals(s))
                            .toList();
                }
                default -> System.out.println("Unknown option");
            }
            result.forEach(a -> System.out.printf(
                    "%s: user=%s, role=%s, type=%s, status=%s, at=%s, expires=%s%n",
                    a.getId(), a.getUsername(), a.getRoleName(), a.getType(),
                    a.getStatus(), a.getAssignedAt(), a.getExpiresAt()));
        });
    }

    private static void registerPermissionCommands(CommandParser parser) {
        parser.registerCommand("permissions-user", "List user's permissions", (scanner, system) -> {
            AssignmentManager am = system.getAssignmentManager();
            RoleManager rm = system.getRoleManager();
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            var map = am.getUserPermissions(username, rm.getAll());
            if (map.isEmpty()) {
                System.out.println("No permissions.");
                return;
            }
            map.forEach((resource, perms) -> {
                System.out.println(resource + ":");
                perms.forEach(p -> System.out.println("  - " + p.getName()));
            });
        });

        parser.registerCommand("permissions-check", "Check user's permission", (scanner, system) -> {
            AssignmentManager am = system.getAssignmentManager();
            RoleManager rm = system.getRoleManager();
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            System.out.print("Permission name: ");
            String permName = scanner.nextLine().trim();
            System.out.print("Resource: ");
            String resource = scanner.nextLine().trim();
            boolean has = am.userHasPermission(username, permName, resource, rm.getAll());
            System.out.println(has ? "Permission granted." : "Permission denied.");
        });
    }

    private static void registerServiceCommands(CommandParser parser) {
        parser.registerCommand("help", "Show help", (scanner, system) -> parser.printHelp());

        parser.registerCommand("stats", "Show statistics", (scanner, system) ->
                System.out.println(system.generateStatistics()));

        parser.registerCommand("clear", "Clear screen", (scanner, system) -> {
            for (int i = 0; i < 30; i++) {
                System.out.println();
            }
        });

        parser.registerCommand("exit", "Exit program", (scanner, system) -> {
            System.out.print("Exit? (yes/no): ");
            String ans = scanner.nextLine().trim();
            if ("yes".equalsIgnoreCase(ans)) {
                System.out.println("Bye.");
                System.exit(0);
            }
        });
    }
}

