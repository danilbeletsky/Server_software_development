package rbac.report;

import rbac.assignment.AssignmentManager;
import rbac.assignment.RoleAssignment;
import rbac.managers.RbacManagers;
import rbac.permission.Permission;
import rbac.role.Role;
import rbac.role.RoleManager;
import rbac.user.User;
import rbac.user.UserManager;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Отчёты: потокобезопасные менеджеры ({@link RbacManagers}) и классические {@link UserManager}/{@link AssignmentManager}.
 */
public class ReportGenerator {

    private final RbacManagers.UserManager threadSafeUsers;
    private final RbacManagers.RoleManager threadSafeRoles;
    private final RbacManagers.AssignmentManager threadSafeAssignments;

    public ReportGenerator(RbacManagers.UserManager users,
                           RbacManagers.RoleManager roles,
                           RbacManagers.AssignmentManager assignments) {
        this.threadSafeUsers = Objects.requireNonNull(users);
        this.threadSafeRoles = Objects.requireNonNull(roles);
        this.threadSafeAssignments = Objects.requireNonNull(assignments);
    }

    /** Сводка по пользователям: всего / с активным назначением / без. */
    public String buildUserSummaryText() {
        List<User> list = new ArrayList<>(threadSafeUsers.listAll());
        long active = list.parallelStream().filter(this::userHasActiveAssignment).count();
        long inactive = list.size() - active;

        String lines = list.stream()
                .map(u -> String.format("- %s (%s, %s)", u.getId(), u.getUsername(), u.getFullName()))
                .collect(Collectors.joining("\n"));

        return String.format(
                "Users: total=%d, with active assignment=%d, without=%d%n%n%s%n",
                list.size(), active, inactive, lines);
    }

    /** Статистика для дашборда. */
    public Map<String, Long> buildUserActivityStats() {
        List<User> list = new ArrayList<>(threadSafeUsers.listAll());
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("total", (long) list.size());
        stats.put("active", list.parallelStream().filter(this::userHasActiveAssignment).count());
        stats.put("inactive", list.parallelStream().filter(u -> !userHasActiveAssignment(u)).count());
        return stats;
    }

    /** Покрытие прав по пользователям (ключи прав: name@resource). */
    public String buildPermissionCoverageText() {
        Map<String, Role> roleById = threadSafeRoles.snapshotMap();
        String rows = threadSafeUsers.listAll().stream()
                .map(User::getId)
                .map(userId -> {
                    Set<String> permKeys = collectPermissionIdsForUser(userId, roleById);
                    return userId + "\t" + permKeys.size() + "\t" + String.join(",", permKeys);
                })
                .collect(Collectors.joining("\n"));
        return "userId\tpermissionCount\tpermissions\n" + rows;
    }

    private boolean userHasActiveAssignment(User u) {
        String key = u.getUsername();
        for (RoleAssignment a : threadSafeAssignments.listAll()) {
            if (a.isActive() && a.getUser() != null && key.equals(a.getUser().getUsername())) {
                return true;
            }
        }
        return false;
    }

    private Set<String> collectPermissionIdsForUser(String userId, Map<String, Role> roleById) {
        Set<String> out = new LinkedHashSet<>();
        for (RoleAssignment a : threadSafeAssignments.listAll()) {
            if (!a.isActive() || a.getUser() == null || !userId.equals(a.getUser().getUsername())) {
                continue;
            }
            Role r = a.getRole();
            if (r == null) {
                continue;
            }
            Role resolved = roleById.getOrDefault(r.getId(), r);
            for (Permission p : resolved.getPermissions()) {
                out.add(p.getName() + "@" + p.getResource());
            }
        }
        return out;
    }

    public String generateUserReport(UserManager userManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();
        sb.append("User report").append(System.lineSeparator());
        sb.append("================").append(System.lineSeparator());
        for (User user : userManager.getAll()) {
            sb.append(String.format("User: %s (%s, %s)%n",
                    user.getUsername(), user.getFullName(), user.getEmail()));
            List<RoleAssignment> assignments = assignmentManager.findByUser(user);
            if (assignments.isEmpty()) {
                sb.append("  No roles").append(System.lineSeparator());
            } else {
                for (RoleAssignment a : assignments) {
                    sb.append(String.format("  - %s (%s, %s)%n",
                            a.getRoleName(), a.getType(), a.getStatus()));
                }
            }
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }

    public String generateRoleReport(RoleManager roleManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();
        sb.append("Role report").append(System.lineSeparator());
        sb.append("============").append(System.lineSeparator());
        for (Role role : roleManager.getAll()) {
            long userCount = assignmentManager.findByRole(role).stream()
                    .map(RoleAssignment::getUsername)
                    .distinct()
                    .count();
            sb.append(String.format("Role: %s (users=%d, permissions=%d)%n",
                    role.getName(), userCount, role.getPermissions().size()));
        }
        return sb.toString();
    }

    public String generatePermissionMatrix(UserManager userManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();
        sb.append("Permission matrix").append(System.lineSeparator());
        sb.append("==================").append(System.lineSeparator());

        Set<String> resources = new java.util.TreeSet<>();
        for (RoleAssignment a : assignmentManager.findAll()) {
            if (a.getRoleName() != null) {
                resources.add(a.getRoleName());
            }
        }
        List<String> usernames = userManager.getAll().stream()
                .map(User::getUsername)
                .sorted()
                .collect(Collectors.toList());

        sb.append("Users × Roles").append(System.lineSeparator());
        sb.append(String.format("%-20s", "User"));
        for (String role : resources) {
            sb.append(String.format("%-15s", role));
        }
        sb.append(System.lineSeparator());

        for (String username : usernames) {
            sb.append(String.format("%-20s", username));
            User u = userManager.findByUsername(username).orElse(null);
            List<RoleAssignment> userAssignments = assignmentManager.findByUser(u);
            for (String role : resources) {
                boolean hasRole = userAssignments.stream()
                        .anyMatch(a -> role.equals(a.getRoleName()));
                sb.append(String.format("%-15s", hasRole ? "X" : ""));
            }
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }

    public void exportToFile(String report, String filename) {
        try (FileWriter fw = new FileWriter(filename)) {
            fw.write(report);
        } catch (IOException e) {
            System.out.println("Failed to save report: " + e.getMessage());
        }
    }
}
