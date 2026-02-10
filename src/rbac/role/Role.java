package rbac.role;

import java.util.HashSet;
import java.util.Set;

import rbac.permission.Permission;

import java.util.*;

public class Role {

    private static final Set<String> USED_NAMES = new HashSet<>();

    private final String id;
    private final String name;
    private final String description;
    private final Set<Permission> permissions = new HashSet<>();

    public Role(String name, String description) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Роль не может быть пустым");
        }
        synchronized (USED_NAMES) {
            if (USED_NAMES.contains(name)) {
                throw new IllegalArgumentException("Имя роли должно быть уникальным!");
            }
            USED_NAMES.add(name);
        }
        this.id = "role_" + UUID.randomUUID();
        this.name = name;
        this.description = description;
    }

    public void addPermission(Permission permission){
    permissions.add(permission);
    }

    public void removePermission(Permission permission){
        permissions.remove(permission);
    }

    public boolean hasPermission(Permission permission){
        return permissions.contains(permission);
    }

    public boolean hasPermission(String permissionName, String resouce){
        return permissions.stream().anyMatch(p -> p.name().equalsIgnoreCase(permissionName) && p.resource().equalsIgnoreCase(resouce));
    }

    public Set<Permission> getPermissions(){
        return Set.copyOf(permissions);
    }
    public String format() {
        StringBuilder sb = new StringBuilder();
        sb.append("Role: ").append(name).append(" [ID: ").append(id).append("]\n");
        sb.append("Description: ").append(description).append("\n");
        sb.append("Permissions (").append(permissions.size()).append("):\n");
        for (Permission p : permissions) {
            sb.append(" - ").append(p.format()).append("\n");
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role role)) return false;
        return id.equals(role.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Role{name='%s', id='%s'}".formatted(name, id);
    }

    public String getName() {
        return name;
    }
}
