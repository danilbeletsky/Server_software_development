@FunctionalInterface
public interface AssignmentFilter {

    boolean test(RoleAssignment assignment);

    default AssignmentFilter and(AssignmentFilter other) {
        if (other == null) {
            throw new IllegalArgumentException("other filter must not be null");
        }
        return assignment -> this.test(assignment) && other.test(assignment);
    }

    default AssignmentFilter or(AssignmentFilter other) {
        if (other == null) {
            throw new IllegalArgumentException("other filter must not be null");
        }
        return assignment -> this.test(assignment) || other.test(assignment);
    }
}

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

final class AssignmentFilters {

    private AssignmentFilters() {
    }

    static AssignmentFilter byUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        return assignment -> assignment != null && user.equals(assignment.getUser());
    }

    static AssignmentFilter byUsername(String username) {
        String expected = nonNullTrim(username);
        return assignment -> assignment != null
                && assignment.getUser() != null
                && expected.equals(assignment.getUser().getUsername());
    }

    static AssignmentFilter byRole(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("role must not be null");
        }
        return assignment -> assignment != null && role.equals(assignment.getRole());
    }

    static AssignmentFilter byRoleName(String roleName) {
        String expected = nonNullTrim(roleName);
        return assignment -> assignment != null
                && assignment.getRole() != null
                && expected.equals(assignment.getRole().getName());
    }

    static AssignmentFilter activeOnly() {
        return assignment -> assignment != null && assignment.isActive();
    }

    static AssignmentFilter inactiveOnly() {
        return assignment -> assignment != null && !assignment.isActive();
    }

    static AssignmentFilter byType(String type) {
        String expected = nonNullTrim(type).toUpperCase();
        return assignment -> {
            if (assignment == null || assignment.getType() == null) {
                return false;
            }
            return assignment.getType().name().equals(expected);
        };
    }

    static AssignmentFilter assignedBy(String username) {
        String expected = nonNullTrim(username);
        return assignment -> assignment != null && expected.equals(assignment.getAssignedBy());
    }

    static AssignmentFilter assignedAfter(String date) {
        LocalDate threshold = parseDate(date);
        return assignment -> assignment != null
                && assignment.getAssignmentDate() != null
                && assignment.getAssignmentDate().isAfter(threshold);
    }

    static AssignmentFilter expiringBefore(String date) {
        LocalDate threshold = parseDate(date);
        return assignment -> {
            if (assignment == null || assignment.getExpirationDate() == null) {
                return false;
            }
            return assignment.getExpirationDate().isBefore(threshold);
        };
    }

    private static String nonNullTrim(String value) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        return value.trim();
    }

    private static LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(nonNullTrim(date));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Date must be in ISO-8601 format yyyy-MM-dd", e);
        }
    }
}

