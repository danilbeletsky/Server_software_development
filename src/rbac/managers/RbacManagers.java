package com.mileshko.rbac.managers;

import com.mileshko.rbac.filter.AssignmentFilter;
import com.mileshko.rbac.filter.RoleFilter;
import com.mileshko.rbac.filter.UserFilter;
import com.mileshko.rbac.model.Permission;
import com.mileshko.rbac.model.Role;
import com.mileshko.rbac.model.RoleAssignment;
import com.mileshko.rbac.model.User;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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

        public User create(String login, String displayName) {
            String id = UUID.randomUUID().toString();
            User u = new User(id, login, displayName, true);
            users.put(id, u);
            return u;
        }

        public Optional<User> get(String id) {
            return Optional.ofNullable(users.get(id));
        }

        public Collection<User> listAll() {
            return List.copyOf(users.values());
        }

        public List<User> findByFilter(UserFilter filter) {
            if (filter == null || filter.isEmpty()) {
                return new ArrayList<>(users.values());
            }
            return users.values().stream().filter(filter.toPredicate()).toList();
        }

        public List<User> findByFilterParallel(UserFilter filter) {
            if (filter == null || filter.isEmpty()) {
                return users.values().parallelStream().toList();
            }
            return users.values().parallelStream().filter(filter.toPredicate()).toList();
        }

        public Stream<User> stream() {
            return users.values().stream();
        }

        public boolean update(User updated) {
            if (users.get(updated.getId()) == null) {
                return false;
            }
            users.put(updated.getId(), updated);
            return true;
        }

        public boolean delete(String id) {
            return users.remove(id) != null;
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
            String id = UUID.randomUUID().toString();
            Role r = new Role(id, name, description, null);
            roles.put(id, r);
            return r;
        }

        public Optional<Role> get(String id) {
            return Optional.ofNullable(roles.get(id));
        }

        public Collection<Role> listAll() {
            return List.copyOf(roles.values());
        }

        public List<Role> findByFilter(RoleFilter filter) {
            if (filter == null || filter.isEmpty()) {
                return new ArrayList<>(roles.values());
            }
            return roles.values().stream().filter(filter.toPredicate()).toList();
        }

        public List<Role> findByFilterParallel(RoleFilter filter) {
            if (filter == null || filter.isEmpty()) {
                return roles.values().parallelStream().toList();
            }
            return roles.values().parallelStream().filter(filter.toPredicate()).toList();
        }

        public Stream<Role> stream() {
            return roles.values().stream();
        }

        public boolean update(Role updated) {
            if (!roles.containsKey(updated.getId())) {
                return false;
            }
            roles.put(updated.getId(), updated);
            return true;
        }

        public boolean delete(String id) {
            return roles.remove(id) != null;
        }

        public int size() {
            return roles.size();
        }

        public void clear() {
            roles.clear();
        }

        public void putAll(Map<String, Role> map) {
            roles.clear();
            roles.putAll(map);
        }

        public Map<String, Role> snapshotMap() {
            return Map.copyOf(roles);
        }
    }

    public static final class PermissionManager {
        private final Map<String, Permission> permissions = new ConcurrentHashMap<>();

        public Permission create(String name, String description) {
            String id = UUID.randomUUID().toString();
            Permission p = new Permission(id, name, description);
            permissions.put(id, p);
            return p;
        }

        public Optional<Permission> get(String id) {
            return Optional.ofNullable(permissions.get(id));
        }

        public Collection<Permission> listAll() {
            return List.copyOf(permissions.values());
        }

        public Stream<Permission> stream() {
            return permissions.values().stream();
        }

        public boolean update(Permission updated) {
            if (!permissions.containsKey(updated.getId())) {
                return false;
            }
            permissions.put(updated.getId(), updated);
            return true;
        }

        public boolean delete(String id) {
            return permissions.remove(id) != null;
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
            if (filter == null || filter.isEmpty()) {
                return new ArrayList<>(assignments.values());
            }
            return assignments.values().stream().filter(filter.toPredicate()).toList();
        }

        public List<RoleAssignment> findByFilterParallel(AssignmentFilter filter) {
            if (filter == null || filter.isEmpty()) {
                return assignments.values().parallelStream().toList();
            }
            return assignments.values().parallelStream().filter(filter.toPredicate()).toList();
        }

        public Stream<RoleAssignment> stream() {
            return assignments.values().stream();
        }

        public boolean update(RoleAssignment updated) {
            if (!assignments.containsKey(updated.getId())) {
                return false;
            }
            assignments.put(updated.getId(), updated);
            return true;
        }

        public boolean delete(String id) {
            return assignments.remove(id) != null;
        }

        public int deactivateExpired(Instant now) {
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
                if (a.isActive() && a.isExpired(now)) {
                    synchronized (this) {
                        RoleAssignment cur = assignments.get(id);
                        if (cur != null && cur.isActive() && cur.isExpired(now)) {
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
