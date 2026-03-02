package rbac.report;

import rbac.manager.AssignmentManager;
import rbac.manager.RoleManager;
import rbac.manager.UserManager;
import rbac.model.Assignment;
import rbac.model.Role;
import rbac.model.User;

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
            List<Assignment> assignments = assignmentManager.getByUser(user.getUsername());
            if (assignments.isEmpty()) {
                sb.append("  No roles").append(System.lineSeparator());
            } else {
                for (Assignment a : assignments) {
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
            long userCount = assignmentManager.getByRole(role.getName()).stream()
                    .map(Assignment::getUsername)
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

        // Collect all resources
        Set<String> resources = new TreeSet<>();
        for (Assignment a : assignmentManager.getAll()) {
            resources.add(a.getRoleName());
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
            for (String role : resources) {
                boolean hasRole = assignmentManager.getByUser(username).stream()
                        .anyMatch(a -> a.getRoleName().equals(role));
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

