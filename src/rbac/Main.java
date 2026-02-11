package rbac;

import rbac.assignment.*;
import rbac.permission.Permission;
import rbac.role.Role;
import rbac.user.User;

public class Main {

    public static void main(String[] args){
        User user = User.validate("admin_1", "System Admin", "admin@example.com");
        System.out.println(user.format());

        Permission read = new Permission("read", "users", "Can view users");
        Permission write = new Permission("WRITE", "users", "Can edit users");

        Role admin = new Role("Administrator", "Full system access");
        admin.addPermission(read);
        admin.addPermission(write);

        System.out.println(admin.format());

        AssignmentMetadata meta =
                AssignmentMetadata.now("system", "Initial setup");

        PermanentAssignment pa =
                new PermanentAssignment(user, admin, meta);

        System.out.println(pa.summary());
    }
}
