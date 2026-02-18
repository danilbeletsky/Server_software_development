import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class AssignmentManager implements Repository<RoleAssignment> {

    private final Map<String, RoleAssignment> assignments = new HashMap<>();
    private final UserManager userManager;
    private final RoleManager roleManager;

    public AssignmentManager(UserManager userManager, RoleManager roleManager) {
        if (userManager == null || roleManager == null) {
            throw new IllegalArgumentException("userManager and roleManager must not be null");
        }
        this.userManager = userManager;
        this.roleManager = roleManager;
    }

    @Override
    public void add(RoleAssignment item) {
        if (item == null) {
            throw new IllegalArgumentException("assignment must not be null");
        }
        String id = requireNonBlank(item.getAssignmentId(), "assignmentId");
        if (assignments.containsKey(id)) {
            throw new IllegalArgumentException("Assignment with id '" + id + "' already exists");
        }

        User user = item.getUser();
        Role role = item.getRole();

        if (user == null || !userManager.exists(user.getUsername())) {
            throw new IllegalArgumentException("User must exist before creating assignment");
        }
        if (role == null || !roleManager.exists(role.getName())) {
            throw new IllegalArgumentException("Role must exist before creating assignment");
        }

        for (RoleAssignment existing : assignments.values()) {
            if (existing.getUser().equals(user)
                    && existing.getRole().equals(role)
                    && existing.isActive()) {
                throw new IllegalStateException("User already has active assignment for this role");
            }
        }

        assignments.put(id, item);
    }

    @Override
    public boolean remove(RoleAssignment item) {
        if (item == null) {
            return false;
        }
        return assignments.remove(item.getAssignmentId(), item);
    }

    @Override
    public Optional<RoleAssignment> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(assignments.get(id));
    }

    @Override
    public List<RoleAssignment> findAll() {
        return new ArrayList<>(assignments.values());
    }

    @Override
    public int count() {
        return assignments.size();
    }

    @Override
    public void clear() {
        assignments.clear();
    }

    public List<RoleAssignment> findByUser(User user) {
        if (user == null) {
            return Collections.emptyList();
        }
        List<RoleAssignment> result = new ArrayList<>();
        for (RoleAssignment assignment : assignments.values()) {
            if (user.equals(assignment.getUser())) {
                result.add(assignment);
            }
        }
        return result;
    }

    public List<RoleAssignment> findByRole(Role role) {
        if (role == null) {
            return Collections.emptyList();
        }
        List<RoleAssignment> result = new ArrayList<>();
        for (RoleAssignment assignment : assignments.values()) {
            if (role.equals(assignment.getRole())) {
                result.add(assignment);
            }
        }
        return result;
    }

    public List<RoleAssignment> findByFilter(AssignmentFilter filter) {
        if (filter == null) {
            return Collections.emptyList();
        }
        List<RoleAssignment> result = new ArrayList<>();
        for (RoleAssignment assignment : assignments.values()) {
            if (filter.test(assignment)) {
                result.add(assignment);
            }
        }
        return result;
    }

    public List<RoleAssignment> findAll(AssignmentFilter filter, Comparator<RoleAssignment> sorter) {
        List<RoleAssignment> filtered = findByFilter(filter);
        if (sorter != null) {
            filtered.sort(sorter);
        }
        return filtered;
    }

    public List<RoleAssignment> getActiveAssignments() {
        List<RoleAssignment> result = new ArrayList<>();
        for (RoleAssignment assignment : assignments.values()) {
            if (assignment.isActive()) {
                result.add(assignment);
            }
        }
        return result;
    }

    public List<RoleAssignment> getExpiredAssignments() {
        List<RoleAssignment> result = new ArrayList<>();
        for (RoleAssignment assignment : assignments.values()) {
            if (!assignment.isActive()) {
                result.add(assignment);
            }
        }
        return result;
    }

    public boolean userHasRole(User user, Role role) {
        if (user == null || role == null) {
            return false;
        }
        for (RoleAssignment assignment : assignments.values()) {
            if (user.equals(assignment.getUser())
                    && role.equals(assignment.getRole())
                    && assignment.isActive()) {
                return true;
            }
        }
        return false;
    }

    public boolean userHasPermission(User user, String permissionName, String resource) {
        if (user == null) {
            return false;
        }
        String name = requireNonBlank(permissionName, "permissionName");
        String res = requireNonBlank(resource, "resource");
        for (RoleAssignment assignment : assignments.values()) {
            if (!user.equals(assignment.getUser()) || !assignment.isActive()) {
                continue;
            }
            for (Permission p : assignment.getRole().getPermissions()) {
                if (name.equals(p.getName()) && res.equals(p.getResource())) {
                    return true;
                }
            }
        }
        return false;
    }

    public Set<Permission> getUserPermissions(User user) {
        if (user == null) {
            return Collections.emptySet();
        }
        Set<Permission> result = new HashSet<>();
        for (RoleAssignment assignment : assignments.values()) {
            if (user.equals(assignment.getUser()) && assignment.isActive()) {
                result.addAll(assignment.getRole().getPermissions());
            }
        }
        return result;
    }

    public void revokeAssignment(String assignmentId) {
        RoleAssignment assignment = assignments.get(requireNonBlank(assignmentId, "assignmentId"));
        if (assignment == null) {
            throw new IllegalArgumentException("Assignment with id '" + assignmentId + "' not found");
        }
        assignment.revoke();
    }

    public void extendTemporaryAssignment(String assignmentId, String newExpirationDate) {
        RoleAssignment assignment = assignments.get(requireNonBlank(assignmentId, "assignmentId"));
        if (assignment == null) {
            throw new IllegalArgumentException("Assignment with id '" + assignmentId + "' not found");
        }
        if (assignment.getType() != AssignmentType.TEMPORARY) {
            throw new IllegalStateException("Only temporary assignments can be extended");
        }
        assignment.setExpirationDate(parseDate(newExpirationDate));
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must be non-blank");
        }
        return value.trim();
    }

    private static LocalDate parseDate(String date) {
        if (date == null) {
            throw new IllegalArgumentException("date must not be null");
        }
        try {
            return LocalDate.parse(date.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Date must be in ISO-8601 format yyyy-MM-dd", e);
        }
    }
}

