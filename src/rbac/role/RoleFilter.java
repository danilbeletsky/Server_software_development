@FunctionalInterface
public interface RoleFilter {

    boolean test(Role role);

    default RoleFilter and(RoleFilter other) {
        if (other == null) {
            throw new IllegalArgumentException("other filter must not be null");
        }
        return role -> this.test(role) && other.test(role);
    }

    default RoleFilter or(RoleFilter other) {
        if (other == null) {
            throw new IllegalArgumentException("other filter must not be null");
        }
        return role -> this.test(role) || other.test(role);
    }
}

import java.util.Objects;

final class RoleFilters {

    private RoleFilters() {
    }

    static RoleFilter byName(String name) {
        String expected = nonNullTrim(name);
        return role -> role != null && expected.equals(role.getName());
    }

    static RoleFilter byNameContains(String substring) {
        String part = nonNullTrim(substring).toLowerCase();
        return role -> {
            if (role == null || role.getName() == null) {
                return false;
            }
            return role.getName().toLowerCase().contains(part);
        };
    }

    static RoleFilter hasPermission(Permission permission) {
        Objects.requireNonNull(permission, "permission");
        return role -> role != null && role.getPermissions().contains(permission);
    }

    static RoleFilter hasPermission(String permissionName, String resource) {
        String name = nonNullTrim(permissionName);
        String res = nonNullTrim(resource);
        return role -> {
            if (role == null) {
                return false;
            }
            for (Permission p : role.getPermissions()) {
                if (name.equals(p.getName()) && res.equals(p.getResource())) {
                    return true;
                }
            }
            return false;
        };
    }

    static RoleFilter hasAtLeastNPermissions(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("n must be >= 0");
        }
        return role -> role != null && role.getPermissions().size() >= n;
    }

    private static String nonNullTrim(String value) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        return value.trim();
    }
}

