import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RoleManager implements Repository<Role> {

    private final Map<String, Role> rolesById = new HashMap<>();
    private final Map<String, Role> rolesByName = new HashMap<>();

    @Override
    public void add(Role item) {
        if (item == null) {
            throw new IllegalArgumentException("role must not be null");
        }
        String id = requireNonBlank(item.getId(), "id");
        String name = requireNonBlank(item.getName(), "name");
        if (rolesById.containsKey(id)) {
            throw new IllegalArgumentException("Role with id '" + id + "' already exists");
        }
        if (rolesByName.containsKey(name)) {
            throw new IllegalArgumentException("Role with name '" + name + "' already exists");
        }
        rolesById.put(id, item);
        rolesByName.put(name, item);
    }

    @Override
    public boolean remove(Role item) {
        if (item == null) {
            return false;
        }
        Role existing = rolesById.get(item.getId());
        if (existing == null) {
            return false;
        }
        rolesById.remove(existing.getId());
        rolesByName.remove(existing.getName());
        return true;
    }

    @Override
    public Optional<Role> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(rolesById.get(id));
    }

    @Override
    public List<Role> findAll() {
        return new ArrayList<>(rolesById.values());
    }

    @Override
    public int count() {
        return rolesById.size();
    }

    @Override
    public void clear() {
        rolesById.clear();
        rolesByName.clear();
    }

    public Optional<Role> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(rolesByName.get(name));
    }

    public List<Role> findByFilter(RoleFilter filter) {
        if (filter == null) {
            return Collections.emptyList();
        }
        List<Role> result = new ArrayList<>();
        for (Role role : rolesById.values()) {
            if (filter.test(role)) {
                result.add(role);
            }
        }
        return result;
    }

    public List<Role> findAll(RoleFilter filter, Comparator<Role> sorter) {
        List<Role> filtered = findByFilter(filter);
        if (sorter != null) {
            filtered.sort(sorter);
        }
        return filtered;
    }

    public boolean exists(String name) {
        if (name == null) {
            return false;
        }
        return rolesByName.containsKey(name);
    }

    public void addPermissionToRole(String roleName, Permission permission) {
        if (permission == null) {
            throw new IllegalArgumentException("permission must not be null");
        }
        Role role = rolesByName.get(requireNonBlank(roleName, "roleName"));
        if (role == null) {
            throw new IllegalArgumentException("Role with name '" + roleName + "' not found");
        }
        role.addPermission(permission);
    }

    public void removePermissionFromRole(String roleName, Permission permission) {
        if (permission == null) {
            throw new IllegalArgumentException("permission must not be null");
        }
        Role role = rolesByName.get(requireNonBlank(roleName, "roleName"));
        if (role == null) {
            throw new IllegalArgumentException("Role with name '" + roleName + "' not found");
        }
        role.removePermission(permission);
    }

    public List<Role> findRolesWithPermission(String permissionName, String resource) {
        String name = requireNonBlank(permissionName, "permissionName");
        String res = requireNonBlank(resource, "resource");
        List<Role> result = new ArrayList<>();
        for (Role role : rolesById.values()) {
            for (Permission p : role.getPermissions()) {
                if (name.equals(p.getName()) && res.equals(p.getResource())) {
                    result.add(role);
                    break;
                }
            }
        }
        return result;
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must be non-blank");
        }
        return value.trim();
    }
}

