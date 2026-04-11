package com.mileshko.rbac.report;

import com.mileshko.rbac.managers.RbacManagers.AssignmentManager;
import com.mileshko.rbac.managers.RbacManagers.RoleManager;
import com.mileshko.rbac.managers.RbacManagers.UserManager;
import com.mileshko.rbac.model.Role;
import com.mileshko.rbac.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Параллельные отчёты (ветка feature/parallel-logs).
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

    public String buildUsersReportParallel() {
        List<User> list = new ArrayList<>(users.listAll());
        long active = list.parallelStream().filter(User::isActive).count();
        long inactive = list.size() - active;
        String header = "Всего пользователей: " + list.size()
                + ", активных: " + active
                + ", неактивных: " + inactive + "\n";
        String body = list.parallelStream()
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .map(u -> u.getId() + "\t" + u.getLogin() + "\t" + u.getDisplayName() + "\t" + u.isActive())
                .collect(Collectors.joining("\n"));
        return header + body;
    }

    public String buildPermissionMatrixParallel() {
        Map<String, Role> roleById = roles.snapshotMap();
        List<String> userIds = users.stream()
                .map(User::getId)
                .sorted()
                .collect(Collectors.toCollection(ArrayList::new));

        Map<String, Set<String>> matrix = userIds.parallelStream().collect(Collectors.toConcurrentMap(
                uid -> uid,
                uid -> collectPermissionIdsForUser(uid, roleById),
                (a, b) -> a
        ));

        TreeMap<String, Set<String>> sorted = new TreeMap<>(matrix);
        StringBuilder sb = new StringBuilder();
        sb.append("Матрица прав (userId → permissionIds):\n");
        for (Map.Entry<String, Set<String>> e : sorted.entrySet()) {
            List<String> perms = new ArrayList<>(e.getValue());
            Collections.sort(perms);
            sb.append(e.getKey()).append(" → ").append(String.join(", ", perms)).append("\n");
        }
        return sb.toString();
    }

    private Set<String> collectPermissionIdsForUser(String userId, Map<String, Role> roleById) {
        return assignments.stream()
                .filter(a -> a.getUserId().equals(userId) && a.isActive())
                .map(a -> roleById.get(a.getRoleId()))
                .filter(r -> r != null)
                .flatMap(r -> r.getPermissionIds().stream())
                .collect(Collectors.toSet());
    }

    public Map<String, Object> usersStatisticsParallel() {
        List<User> list = new ArrayList<>(users.listAll());
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", list.size());
        stats.put("active", list.parallelStream().filter(User::isActive).count());
        stats.put("inactive", list.parallelStream().filter(u -> !u.isActive()).count());
        return stats;
    }
}
