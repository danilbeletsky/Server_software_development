import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class UserManager implements Repository<User> {

    private final Map<String, User> users = new HashMap<>();

    @Override
    public void add(User item) {
        if (item == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        String username = requireNonBlank(item.getUsername(), "username");
        if (users.containsKey(username)) {
            throw new IllegalArgumentException("User with username '" + username + "' already exists");
        }
        users.put(username, item);
    }

    @Override
    public boolean remove(User item) {
        if (item == null) {
            return false;
        }
        return users.remove(item.getUsername(), item);
    }

    @Override
    public Optional<User> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public int count() {
        return users.size();
    }

    @Override
    public void clear() {
        users.clear();
    }

    public Optional<User> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(users.get(username));
    }

    public Optional<User> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        for (User user : users.values()) {
            if (email.equals(user.getEmail())) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    public List<User> findByFilter(UserFilter filter) {
        if (filter == null) {
            return Collections.emptyList();
        }
        List<User> result = new ArrayList<>();
        for (User user : users.values()) {
            if (filter.test(user)) {
                result.add(user);
            }
        }
        return result;
    }

    public List<User> findAll(UserFilter filter, Comparator<User> sorter) {
        List<User> filtered = findByFilter(filter);
        if (sorter != null) {
            filtered.sort(sorter);
        }
        return filtered;
    }

    public boolean exists(String username) {
        if (username == null) {
            return false;
        }
        return users.containsKey(username);
    }

    public void update(String username, String newFullName, String newEmail) {
        String key = requireNonBlank(username, "username");
        User user = users.get(key);
        if (user == null) {
            throw new IllegalArgumentException("User with username '" + username + "' not found");
        }
        user.setFullName(requireNonBlank(newFullName, "newFullName"));
        user.setEmail(requireNonBlank(newEmail, "newEmail"));
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must be non-blank");
        }
        return value.trim();
    }
}

