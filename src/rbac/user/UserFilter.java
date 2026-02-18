@FunctionalInterface
public interface UserFilter {

    boolean test(User user);

    default UserFilter and(UserFilter other) {
        if (other == null) {
            throw new IllegalArgumentException("other filter must not be null");
        }
        return user -> this.test(user) && other.test(user);
    }

    default UserFilter or(UserFilter other) {
        if (other == null) {
            throw new IllegalArgumentException("other filter must not be null");
        }
        return user -> this.test(user) || other.test(user);
    }
}

final class UserFilters {

    private UserFilters() {
    }

    static UserFilter byUsername(String username) {
        String expected = nonNullTrim(username);
        return user -> user != null && expected.equals(user.getUsername());
    }

    static UserFilter byUsernameContains(String substring) {
        String part = nonNullTrim(substring).toLowerCase();
        return user -> {
            if (user == null || user.getUsername() == null) {
                return false;
            }
            return user.getUsername().toLowerCase().contains(part);
        };
    }

    static UserFilter byEmail(String email) {
        String expected = nonNullTrim(email);
        return user -> user != null && expected.equals(user.getEmail());
    }

    static UserFilter byEmailDomain(String domain) {
        String expected = nonNullTrim(domain);
        return user -> {
            if (user == null || user.getEmail() == null) {
                return false;
            }
            return user.getEmail().endsWith(expected);
        };
    }

    static UserFilter byFullNameContains(String substring) {
        String part = nonNullTrim(substring).toLowerCase();
        return user -> {
            if (user == null || user.getFullName() == null) {
                return false;
            }
            return user.getFullName().toLowerCase().contains(part);
        };
    }

    private static String nonNullTrim(String value) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        return value.trim();
    }
}

