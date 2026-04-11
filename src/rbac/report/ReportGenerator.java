package rbac.report;

import rbac.assignment.AssignmentManager;
import rbac.assignment.RoleAssignment;
import rbac.role.RoleManager;
import rbac.user.User;
import rbac.user.UserManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Параллельные отчёты по пользователям, назначениям и матрице прав.
 */
public final class ReportGenerator {
    private final UserManager users;
    private final RoleManager roles;
    private final AssignmentManager assignments;

    public ReportGenerator(UserManager users, RoleManager roles, AssignmentManager assignments) {
        this.users = users;
        this.roles = roles;
        this.assignments = assignments;
    }

    /** Пользователь считается «активным», если у него есть хотя бы одно активное назначение роли. */
    private boolean isUserActive(User user) {
        if (user == null) {
            return false;
        }
        return assignments.findAll().stream()
                .anyMatch(a -> a.isActive()
                        && a.getUser() != null
                        && a.getUser().getUsername().equals(user.getUsername()));
    }

    public String buildUsersReportParallel() {
        List<User> list = new ArrayList<>(users.findAll());
        long active = list.parallelStream().filter(this::isUserActive).count();
        long inactive = list.size() - active;
        String header = "Всего пользователей: " + list.size()
                + ", активных: " + active
                + ", неактивных: " + inactive + "\n";
        String body = list.parallelStream()
                .sorted(Comparator.comparing(User::getUsername))
                .map(u -> u.getUsername() + "\t" + u.getFullName() + "\t" + u.getEmail() + "\t" + isUserActive(u))
                .collect(Collectors.joining("\n"));
        return header + body;
    }

    private Set<String> collectPermissionKeysForUser(String username) {
        return assignments.findAll().stream()
                .filter(a -> a.isActive()
                        && a.getUser() != null
                        && username.equals(a.getUser().getUsername()))
                .map(RoleAssignment::getRole)
                .filter(Objects::nonNull)
                .flatMap(r -> r.getPermissions().stream())
                .map(p -> p.getName() + "@" + p.getResource())
                .collect(Collectors.toSet());
    }

    public String buildPermissionMatrixParallel() {
        List<String> usernames = users.findAll().stream()
                .map(User::getUsername)
                .sorted()
                .collect(Collectors.toCollection(ArrayList::new));

        Map<String, Set<String>> matrix = usernames.parallelStream().collect(Collectors.toConcurrentMap(
                u -> u,
                this::collectPermissionKeysForUser,
                (a, b) -> a
        ));

        TreeMap<String, Set<String>> sorted = new TreeMap<>(matrix);
        StringBuilder sb = new StringBuilder();
        sb.append("Матрица прав (username → permission@resource):\n");
        for (Map.Entry<String, Set<String>> e : sorted.entrySet()) {
            List<String> perms = new ArrayList<>(e.getValue());
            Collections.sort(perms);
            sb.append(e.getKey()).append(" → ").append(String.join(", ", perms)).append("\n");
        }
        return sb.toString();
    }

    public Map<String, Object> usersStatisticsParallel() {
        List<User> list = new ArrayList<>(users.findAll());
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", list.size());
        stats.put("active", list.parallelStream().filter(this::isUserActive).count());
        stats.put("inactive", list.parallelStream().filter(u -> !isUserActive(u)).count());
        return stats;
    }
}
