package rbac.report;

import rbac.assignment.AssignmentManager;
import rbac.assignment.RoleAssignment;
import rbac.role.Role;
import rbac.role.RoleManager;
import rbac.user.User;
import rbac.user.UserManager;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ReportGenerator {

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

        Set<String> resources = new TreeSet<>();
        for (RoleAssignment a : assignmentManager.findAll()) {
            if (a.getRoleName() != null) resources.add(a.getRoleName());
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

