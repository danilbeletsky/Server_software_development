import java.util.Comparator;

final class UserSorters {

    private UserSorters() {
    }

    static Comparator<User> byUsername() {
        return Comparator.comparing(User::getUsername, Comparator.nullsFirst(String::compareTo));
    }

    static Comparator<User> byFullName() {
        return Comparator.comparing(User::getFullName, Comparator.nullsFirst(String::compareTo));
    }

    static Comparator<User> byEmail() {
        return Comparator.comparing(User::getEmail, Comparator.nullsFirst(String::compareTo));
    }
}

final class RoleSorters {

    private RoleSorters() {
    }

    static Comparator<Role> byName() {
        return Comparator.comparing(Role::getName, Comparator.nullsFirst(String::compareTo));
    }

    static Comparator<Role> byPermissionCount() {
        return Comparator.comparingInt(role -> role.getPermissions().size());
    }
}

final class AssignmentSorters {

    private AssignmentSorters() {
    }

    static Comparator<RoleAssignment> byUsername() {
        return Comparator.comparing(
                assignment -> assignment.getUser() != null ? assignment.getUser().getUsername() : null,
                Comparator.nullsFirst(String::compareTo)
        );
    }

    static Comparator<RoleAssignment> byRoleName() {
        return Comparator.comparing(
                assignment -> assignment.getRole() != null ? assignment.getRole().getName() : null,
                Comparator.nullsFirst(String::compareTo)
        );
    }

    static Comparator<RoleAssignment> byAssignmentDate() {
        return Comparator.comparing(RoleAssignment::getAssignmentDate, Comparator.nullsFirst((d1, d2) -> {
            if (d1 == null && d2 == null) {
                return 0;
            }
            if (d1 == null) {
                return -1;
            }
            if (d2 == null) {
                return 1;
            }
            return d1.compareTo(d2);
        }));
    }
}

