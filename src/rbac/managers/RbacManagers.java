package rbac.managers;

import rbac.assignment.AssignmentFilter;
import rbac.assignment.RoleAssignment;
import rbac.permission.Permission;
import rbac.role.Role;
import rbac.role.RoleFilter;
import rbac.user.User;
import rbac.user.UserFilter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Потокобезопасные менеджеры (ветка feature/threadsafe-managers).
 */
public final class RbacManagers {

    private RbacManagers() {
    }

    public static final class UserManager {
        private final Map<String, User> users = new ConcurrentHashMap<>();

        public User create(String username, String fullname, String email) {
            User u = User.validate(username, fullname, email);
            users.put(u.getUsername(), u);
            return u;
        }

        public Optional<User> get(String username) {
            return Optional.ofNullable(users.get(username));
        }

        public Collection<User> listAll() {
            return List.copyOf(users.values());
        }

        public List<User> findByFilter(UserFilter filter) {
            if (filter == null) {
                return new ArrayList<>(users.values());
            }
            return users.values().stream().filter(filter::test).toList();
        }

        public List<User> findByFilterParallel(UserFilter filter) {
            if (filter == null) {
                return users.values().parallelStream().toList();
            }
            return users.values().parallelStream().filter(filter::test).toList();
        }

        public Stream<User> stream() {
            return users.values().stream();
        }

        public boolean update(User updated) {
            if (updated == null || users.get(updated.getUsername()) == null) {
                return false;
            }
            users.put(updated.getUsername(), updated);
            return true;
        }

        public boolean delete(String username) {
            return users.remove(username) != null;
        }

        public int size() {
            return users.size();
        }

        public void clear() {
            users.clear();
        }

        public void putAll(Map<String, User> map) {
            users.clear();
            users.putAll(map);
        }

        public Map<String, User> snapshotMap() {
            return Map.copyOf(users);
        }
    }

    public static final class RoleManager {
        private final Map<String, Role> roles = new ConcurrentHashMap<>();

        public Role create(String name, String description) {
            Role r = new Role(name, description);
            roles.put(r.getId(), r);
            return r;
        }

        public Optional<Role> get(String id) {
            return Optional.ofNullable(roles.get(id));
        }

        public Collection<Role> listAll() {
            return List.copyOf(roles.values());
        }

        public List<Role> findByFilter(RoleFilter filter) {
            if (filter == null) {
                return new ArrayList<>(roles.values());
            }
            return roles.values().stream().filter(filter::test).toList();
        }

        public List<Role> findByFilterParallel(RoleFilter filter) {
            if (filter == null) {
                return roles.values().parallelStream().toList();
            }
            return roles.values().parallelStream().filter(filter::test).toList();
        }

        public Stream<Role> stream() {
            return roles.values().stream();
        }

        public boolean update(Role updated) {
            if (updated == null || !roles.containsKey(updated.getId())) {
                return false;
            }
            roles.put(updated.getId(), updated);
            return true;
        }

        public boolean delete(String id) {
            Role removed = roles.remove(id);
            if (removed != null) {
                removed.releaseName();
                return true;
            }
            return false;
        }

        public int size() {
            return roles.size();
        }

        public void clear() {
            for (Role r : roles.values()) {
                r.releaseName();
            }
            roles.clear();
        }

        public void putAll(Map<String, Role> map) {
            for (Role r : roles.values()) {
                r.releaseName();
            }
            roles.clear();
            roles.putAll(map);
        }

        public Map<String, Role> snapshotMap() {
            return Map.copyOf(roles);
        }
    }

    public static final class PermissionManager {
        private final Map<String, Permission> permissions = new ConcurrentHashMap<>();

        private static String key(Permission p) {
            return p.getName() + "@" + p.getResource();
        }

        public Permission create(String name, String resource, String description) {
            Permission p = new Permission(name, resource, description);
            permissions.put(key(p), p);
            return p;
        }

        public Optional<Permission> get(String name, String resource) {
            Permission probe = new Permission(name, resource, "x");
            return Optional.ofNullable(permissions.get(key(probe)));
        }

        public Optional<Permission> getByKey(String permissionKey) {
            return Optional.ofNullable(permissions.get(permissionKey));
        }

        public Collection<Permission> listAll() {
            return List.copyOf(permissions.values());
        }

        public Stream<Permission> stream() {
            return permissions.values().stream();
        }

        public boolean update(Permission updated) {
            if (updated == null) {
                return false;
            }
            String k = key(updated);
            if (!permissions.containsKey(k)) {
                return false;
            }
            permissions.put(k, updated);
            return true;
        }

        public boolean delete(String name, String resource) {
            Permission probe = new Permission(name, resource, "x");
            return permissions.remove(key(probe)) != null;
        }

        public int size() {
            return permissions.size();
        }

        public void clear() {
            permissions.clear();
        }

        public void putAll(Map<String, Permission> map) {
            permissions.clear();
            permissions.putAll(map);
        }

        public Map<String, Permission> snapshotMap() {
            return Map.copyOf(permissions);
        }
    }

    public static final class AssignmentManager {
        private final Map<String, RoleAssignment> assignments = new ConcurrentHashMap<>();

        public RoleAssignment add(RoleAssignment assignment) {
            assignments.put(assignment.getId(), assignment);
            return assignment;
        }

        public Optional<RoleAssignment> get(String id) {
            return Optional.ofNullable(assignments.get(id));
        }

        public Collection<RoleAssignment> listAll() {
            return List.copyOf(assignments.values());
        }

        public List<RoleAssignment> findByFilter(AssignmentFilter filter) {
            if (filter == null) {
                return new ArrayList<>(assignments.values());
            }
            return assignments.values().stream().filter(filter::test).toList();
        }

        public List<RoleAssignment> findByFilterParallel(AssignmentFilter filter) {
            if (filter == null) {
                return assignments.values().parallelStream().toList();
            }
            return assignments.values().parallelStream().filter(filter::test).toList();
        }

        public Stream<RoleAssignment> stream() {
            return assignments.values().stream();
        }

        public boolean update(RoleAssignment updated) {
            if (updated == null || !assignments.containsKey(updated.getId())) {
                return false;
            }
            assignments.put(updated.getId(), updated);
            return true;
        }

        public boolean delete(String id) {
            return assignments.remove(id) != null;
        }

        public int deactivateExpired(Instant now) {
            Instant ref = now != null ? now : Instant.now();
            int n = 0;
            List<String> ids;
            synchronized (this) {
                ids = new ArrayList<>(assignments.keySet());
            }
            for (String id : ids) {
                RoleAssignment a = assignments.get(id);
                if (a == null) {
                    continue;
                }
                if (a.isActive() && a.isExpired(ref)) {
                    synchronized (this) {
                        RoleAssignment cur = assignments.get(id);
                        if (cur != null && cur.isActive() && cur.isExpired(ref)) {
                            cur.setActive(false);
                            n++;
                        }
                    }
                }
            }
            return n;
        }

        public int size() {
            return assignments.size();
        }

        public void clear() {
            assignments.clear();
        }

        public void putAll(Map<String, RoleAssignment> map) {
            assignments.clear();
            assignments.putAll(map);
        }

        public Map<String, RoleAssignment> snapshotMap() {
            return Map.copyOf(assignments);
        }
    }
}
